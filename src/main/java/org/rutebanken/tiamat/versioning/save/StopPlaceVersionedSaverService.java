/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.versioning.save;

import org.rutebanken.tiamat.auth.StopPlaceAuthorizationService;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.changelog.EntityChangedListener;
import org.rutebanken.tiamat.diff.TiamatObjectDiffer;
import org.rutebanken.tiamat.geo.ZoneDistanceChecker;
import org.rutebanken.tiamat.importer.finder.NearbyStopPlaceFinder;
import org.rutebanken.tiamat.importer.finder.StopPlaceByQuayOriginalIdFinder;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.repository.TariffZoneRepository;
import org.rutebanken.tiamat.service.TariffZonesLookupService;
import org.rutebanken.tiamat.service.TopographicPlaceLookupService;
import org.rutebanken.tiamat.service.metrics.MetricsService;
import org.rutebanken.tiamat.versioning.ValidityUpdater;
import org.rutebanken.tiamat.versioning.VersionIncrementor;
import org.rutebanken.tiamat.versioning.util.AccessibilityAssessmentOptimizer;
import org.rutebanken.tiamat.versioning.validate.SubmodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.rutebanken.tiamat.versioning.save.DefaultVersionedSaverService.MILLIS_BETWEEN_VERSIONS;


@Transactional
@Service
public class StopPlaceVersionedSaverService {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceVersionedSaverService.class);

    public static final int ADJACENT_STOP_PLACE_MAX_DISTANCE_IN_METERS = 30;

    public static final InterchangeWeightingEnumeration DEFAULT_WEIGHTING = InterchangeWeightingEnumeration.INTERCHANGE_ALLOWED;

    @Autowired
    private ZoneDistanceChecker zoneDistanceChecker;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private TariffZoneRepository tariffZoneRepository;

    @Autowired
    private AccessibilityAssessmentOptimizer accessibilityAssessmentOptimizer;

    @Autowired
    private TopographicPlaceLookupService countyAndMunicipalityLookupService;

    @Autowired
    private TariffZonesLookupService tariffZonesLookupService;

    @Autowired
    private StopPlaceByQuayOriginalIdFinder stopPlaceByQuayOriginalIdFinder;

    @Autowired
    private NearbyStopPlaceFinder nearbyStopPlaceFinder;

    @Autowired
    private EntityChangedListener entityChangedListener;

    @Autowired
    private SubmodeValidator submodeValidator;

    @Autowired
    private StopPlaceAuthorizationService stopPlaceAuthorizationService;

    @Autowired
    private ValidityUpdater validityUpdater;

    @Autowired
    private VersionIncrementor versionIncrementor;

    @Autowired
    private UsernameFetcher usernameFetcher;

    @Autowired
    private TiamatObjectDiffer tiamatObjectDiffer;

    @Autowired
    private MetricsService metricsService;

    public StopPlace saveNewVersion(StopPlace existingVersion, StopPlace newVersion, Instant defaultValidFrom) {
        return saveNewVersion(existingVersion, newVersion, defaultValidFrom, new HashSet<>());
    }

    public StopPlace saveNewVersion(StopPlace existingVersion, StopPlace newVersion, Set<String> childStopsUpdated) {
        return saveNewVersion(existingVersion, newVersion, Instant.now(), childStopsUpdated);
    }

    public StopPlace saveNewVersion(StopPlace existingStopPlace, StopPlace newVersion) {
        return saveNewVersion(existingStopPlace, newVersion, Instant.now());
    }

    public StopPlace saveNewVersion(StopPlace newVersion) {
        return saveNewVersion(null, newVersion);
    }

    public StopPlace saveNewVersion(StopPlace existingVersion, StopPlace newVersion, Instant defaultValidFrom, Set<String> childStopsUpdated) {

        if (existingVersion == null) {
            existingVersion = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(newVersion.getNetexId());
        }

        if (existingVersion != null && existingVersion.getNetexId() != null && newVersion.getNetexId() != null && !newVersion.getNetexId().equals(existingVersion.getNetexId())) {
            throw new IllegalArgumentException("Saving new version of different object is not allowed");
        }

        if (newVersion.getParentSiteRef() != null && !newVersion.isParentStopPlace()) {
            throw new IllegalArgumentException("StopPlace " +
                                                       newVersion.getNetexId() +
                                                       " seems to be a child stop. Save the parent stop place instead: "
                                                       + newVersion.getParentSiteRef());
        }

        if (newVersion.getTariffZones() != null) {
            newVersion.setTariffZones(newVersion.getTariffZones().stream()
                    .map(tariffZoneRef -> {
                        TariffZone tariffZone = tariffZoneRepository.findFirstByNetexIdOrderByVersionDesc(tariffZoneRef.getRef());
                        if(tariffZone == null){
                            tariffZone = resolve(tariffZoneRef);
                        }
                        if (tariffZone == null) {
                            throw new IllegalArgumentException("StopPlace refers to non-existing tariff zone: " + tariffZoneRef);
                        }
                        return new TariffZoneRef(tariffZone.getNetexId());
                    })
                    .collect(Collectors.toSet()));
        }

        validateAdjacentSites(newVersion);

        submodeValidator.validate(newVersion);

        Instant changed = Instant.now();

        logger.debug("Rearrange accessibility assessments for: {}", newVersion);
        accessibilityAssessmentOptimizer.optimizeAccessibilityAssessmentsStopPlace(newVersion);

        Instant newVersionValidFrom = validityUpdater.updateValidBetween(existingVersion, newVersion, defaultValidFrom);
        updateValidBetweenInChildren(newVersion, newVersion.getValidBetween());

        if (existingVersion == null) {
            logger.debug("Existing version is not present, which means new entity. {}", newVersion);
            newVersion.setCreated(changed);
            stopPlaceAuthorizationService.assertAuthorizedToEdit(null, newVersion, childStopsUpdated);
        } else {
            newVersion.setChanged(changed);
            Instant oldversionTerminationTime = newVersionValidFrom.minusMillis(MILLIS_BETWEEN_VERSIONS);
            logger.debug("About to terminate previous version for {},{}", existingVersion.getNetexId(), existingVersion.getVersion());
            logger.debug("Found previous version {},{}. Terminating it.", existingVersion.getNetexId(), existingVersion.getVersion());
            validityUpdater.terminateVersion(existingVersion, oldversionTerminationTime);
            terminateChild(existingVersion, oldversionTerminationTime);
            stopPlaceAuthorizationService.assertAuthorizedToEdit(existingVersion, newVersion, childStopsUpdated);
            stopPlaceRepository.delete(existingVersion);
        }

        newVersion = versionIncrementor.initiateOrIncrementVersionsStopPlace(newVersion);

        newVersion.setChangedBy(usernameFetcher.getUserNameForAuthenticatedUser());
        logger.info("StopPlace [{}], version {} changed by user [{}]. {}", newVersion.getNetexId(), newVersion.getVersion(), newVersion.getChangedBy(), newVersion.getValidBetween());

        if (newVersion.getWeighting() == null) {
            logger.info("Weighting is null for stop {} {}. Setting default value {}.", newVersion.getName(), newVersion.getNetexId(), DEFAULT_WEIGHTING);
            newVersion.setWeighting(DEFAULT_WEIGHTING);
        }

        countyAndMunicipalityLookupService.populateTopographicPlaceRelation(newVersion);
        tariffZonesLookupService.populateTariffZone(newVersion);


        if (newVersion.getChildren() != null && newVersion.getChildren().size() > 0) {
            newVersion.getChildren().forEach(child -> {
                child.setChanged(changed);
                tariffZonesLookupService.populateTariffZone(child);
            });

            stopPlaceRepository.saveAll(newVersion.getChildren());
            if (logger.isDebugEnabled()) {
                logger.debug("Saved children: {}", newVersion.getChildren().stream()
                                                           .map(sp -> "{id:" + sp.getId() + " netexId:" + sp.getNetexId() + " version:" + sp.getVersion() + "}")
                                                           .collect(Collectors.toList()));
            }
        }
        newVersion = stopPlaceRepository.save(newVersion);
        logger.debug("Saved stop place with id: {} and childs {}", newVersion.getId(), newVersion.getChildren().stream().map(ch -> ch.getId()).collect(toList()));

        updateParentSiteRefsForChildren(newVersion);

        if (existingVersion != null) {
            tiamatObjectDiffer.logDifference(existingVersion, newVersion);
        }
        metricsService.registerEntitySaved(newVersion.getClass());

        updateQuaysCache(newVersion);

        nearbyStopPlaceFinder.update(newVersion);
        newVersion.getChildren().forEach(nearbyStopPlaceFinder::update);
        entityChangedListener.onChange(newVersion);

        return newVersion;
    }

    private TariffZone resolve(TariffZoneRef tariffZoneRef) {
        String netexId = tariffZoneRepository.findFirstByKeyValue(NetexIdMapper.FARE_ZONE, tariffZoneRef.getRef());
        return tariffZoneRef.getRef() != null ? tariffZoneRepository.findFirstByNetexIdOrderByVersionDesc(netexId) : null;
    }


    private void updateValidBetweenInChildren(StopPlace stopPlace, ValidBetween validBetween){
        if (stopPlace.getChildren() == null){
            return;
        }

        for (StopPlace child : stopPlace.getChildren()) {
            child.setValidBetween(validBetween);
        }
    }

    private void terminateChild(StopPlace stopPlaceToTerminate, Instant terminationInstant ){
        if (stopPlaceToTerminate.getChildren() != null){

            for (StopPlace child : stopPlaceToTerminate.getChildren()) {
                ValidBetween validBetween;
                if (child.getValidBetween() != null){
                    validBetween = child.getValidBetween();
                }else{
                    validBetween = new ValidBetween();
                    validBetween.setFromDate(terminationInstant.minusMillis(MILLIS_BETWEEN_VERSIONS));
                    child.setValidBetween(validBetween);
                }
                validBetween.setToDate(terminationInstant);
            }
        }
    }

    private void validateAdjacentSites(StopPlace newVersion) {
        if (newVersion.getAdjacentSites() != null) {
            logger.info("Validating adjacent sites for {} {}", newVersion.getNetexId(), newVersion.getName());
            for (SiteRefStructure siteRefStructure : newVersion.getAdjacentSites()) {

                if (newVersion.getNetexId() != null && (newVersion.getNetexId().equals(siteRefStructure.getRef()))) {
                    throw new IllegalArgumentException("Cannot set own ID as adjacent site ref: " + siteRefStructure.getRef());
                }

                StopPlace adjacentStop = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(siteRefStructure.getRef());
                if (adjacentStop == null) {
                    throw new IllegalArgumentException("StopPlace " + newVersion.getId() + ", " + newVersion.getName() + " cannot have " + siteRefStructure.getRef() + " as adjacent stop as it does not exist");
                }

                if (zoneDistanceChecker.exceedsLimit(newVersion, adjacentStop, ADJACENT_STOP_PLACE_MAX_DISTANCE_IN_METERS)) {
                    throw new IllegalArgumentException(
                            "StopPlace " + newVersion.getId() + ", " + newVersion.getName() +
                                    " cannot be located more than " + ADJACENT_STOP_PLACE_MAX_DISTANCE_IN_METERS +
                                    " meters from the adjacent stop: " + siteRefStructure.getRef());
                }
            }
        }
    }

    private void updateQuaysCache(StopPlace stopPlace) {
        if (stopPlace.getQuays() != null) {
            stopPlaceByQuayOriginalIdFinder.updateCache(stopPlace.getNetexId(),
                    stopPlace.getQuays()
                            .stream()
                            .flatMap(q -> q.getOriginalIds().stream())
                            .collect(toList()));
        }
        if (stopPlace.isParentStopPlace()) {
            if (stopPlace.getChildren() != null) {
                stopPlace.getChildren().forEach(this::updateQuaysCache);
            }
        }
    }


    /**
     * Needs to be done after parent stop place has been assigned an ID
     *
     * @param parentStopPlace saved parent stop place
     */
    private void updateParentSiteRefsForChildren(StopPlace parentStopPlace) {
        long count = 0;
        if (parentStopPlace.getChildren() != null) {
            parentStopPlace.getChildren().stream()
                    .forEach(child -> child.setParentSiteRef(new SiteRefStructure(parentStopPlace.getNetexId(), String.valueOf(parentStopPlace.getVersion()))));
            count = parentStopPlace.getChildren().size();
        }
        logger.info("Updated {} childs with parent site refs", count);
    }

}
