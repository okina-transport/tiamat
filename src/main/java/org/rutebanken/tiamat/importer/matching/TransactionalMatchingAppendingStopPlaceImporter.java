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

package org.rutebanken.tiamat.importer.matching;

import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.tiamat.geo.StopPlaceCentroidComputer;
import org.rutebanken.tiamat.geo.ZoneDistanceChecker;
import org.rutebanken.tiamat.importer.AlternativeStopTypes;
import org.rutebanken.tiamat.importer.KeyValueListAppender;
import org.rutebanken.tiamat.importer.finder.NearbyStopPlaceFinder;
import org.rutebanken.tiamat.importer.finder.SimpleNearbyStopPlaceFinder;
import org.rutebanken.tiamat.importer.finder.StopPlaceByIdFinder;
import org.rutebanken.tiamat.importer.merging.MergingStopPlaceImporter;
import org.rutebanken.tiamat.importer.merging.QuayMerger;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.stopplace.StopPlaceDeleter;
import org.rutebanken.tiamat.service.stopplace.StopPlaceQuayMover;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class TransactionalMatchingAppendingStopPlaceImporter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalMatchingAppendingStopPlaceImporter.class);

    private static final boolean CREATE_NEW_QUAYS = true;

    private static final boolean ALLOW_OTHER_TYPE_AS_ANY_MATCH = true;

    @Autowired
    private KeyValueListAppender keyValueListAppender;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private QuayMerger quayMerger;

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    private NearbyStopPlaceFinder nearbyStopPlaceFinder;

    @Autowired
    private StopPlaceByIdFinder stopPlaceByIdFinder;

    @Autowired
    private SimpleNearbyStopPlaceFinder simpleNearbyStopPlaceFinder;

    @Autowired
    private ZoneDistanceChecker zoneDistanceChecker;

    @Autowired
    private AlternativeStopTypes alternativeStopTypes;

    @Autowired
    private MergingStopPlaceImporter mergingStopPlaceImporter;

    @Value("${onMoveOnly.modeMatch.noMergeSeveralProducers:false}")
    boolean noMergeOnMoveOnly;

    @Autowired
    protected VersionCreator versionCreator;

    @Autowired
    private StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    @Autowired
    private StopPlaceCentroidComputer stopPlaceCentroidComputer;

    @Autowired
    private QuayMover quayMover;

    public void findAppendAndAdd(final org.rutebanken.tiamat.model.StopPlace incomingStopPlace,
                                 List<StopPlace> matchedStopPlaces,
                                 AtomicInteger stopPlacesCreatedOrUpdated, boolean onMoveOnlyImport) {


        List<org.rutebanken.tiamat.model.StopPlace> foundStopPlaces = stopPlaceByIdFinder.findStopPlace(incomingStopPlace);
        final int foundStopPlacesCount = foundStopPlaces.size();

        if (!foundStopPlaces.isEmpty()) {

            List<org.rutebanken.tiamat.model.StopPlace> filteredStopPlaces = foundStopPlaces
                    .stream()
                    .filter(foundStopPlace -> {
                        if (zoneDistanceChecker.exceedsLimit(incomingStopPlace, foundStopPlace)) {
                            logger.warn("Found stop place, but the distance between incoming and found stop place is too far in meters: {}. Incoming: {}. Found: {}",
                                    ZoneDistanceChecker.DEFAULT_MAX_DISTANCE,
                                    incomingStopPlace,
                                    foundStopPlace);
                            return false;
                        }
                        return true;
                    })
                    .filter(foundStopPlace -> {

                        if (foundStopPlacesCount == 1) {
                            logger.info("There are only one found stop places. Filtering in stop place regardless of type {}", foundStopPlace);
                            return true;
                        }

                        if (incomingStopPlace.getStopPlaceType() == null) {
                            logger.info("Incoming stop place type is null. Filter in. {}", incomingStopPlace);
                            return true;
                        }

                        if (incomingStopPlace.getStopPlaceType().equals(StopTypeEnumeration.OTHER)) {
                            logger.info("Incoming stop place type is OTHER. Filter in. {}", incomingStopPlace);
                            return true;
                        }

                        if (foundStopPlace.getStopPlaceType().equals(incomingStopPlace.getStopPlaceType())
                                || alternativeStopTypes.matchesAlternativeType(foundStopPlace.getStopPlaceType(), incomingStopPlace.getStopPlaceType())) {
                            return true;
                        }

                        logger.warn("Found match for incoming stop place {}, but the type does not match: {} != {}. Filter out. Incoming stop: {}", foundStopPlace.getNetexId(), incomingStopPlace.getStopPlaceType(), foundStopPlace.getStopPlaceType(), incomingStopPlace);

                        return false;
                    })
                    // Collect distinct on ID
                    .collect(toList());

            foundStopPlaces = filteredStopPlaces;
        }

        if (foundStopPlaces.isEmpty()){
            //No stop place were found using ids. Looking for stop place by location
            simpleNearbyStopPlaceFinder.find(incomingStopPlace)
                                       .ifPresent(foundStopPlaces::add);

            logger.warn("Neaby Finder : foundStopPlaces:"+foundStopPlaces.size());
        }




        if (foundStopPlaces.isEmpty()) {
            logger.warn("Cannot find stop place from IDs or location: {}. StopPlace toString: {}. Will add it as a new one",
                    incomingStopPlace.importedIdAndNameToString(),
                    incomingStopPlace);

            StopPlace newStopPlace = null;
            try {
                newStopPlace = mergingStopPlaceImporter.importStopPlace(incomingStopPlace);
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Problem while adding new stop place", e);
            }
            matchedStopPlaces.add(newStopPlace);
            stopPlacesCreatedOrUpdated.incrementAndGet();

        } else {

            if (foundStopPlaces.size() > 1) {
                logger.warn("Found {} matches for incoming stop place {}. Matches: {}", foundStopPlaces.size(), incomingStopPlace, foundStopPlaces);

                try {
                    foundStopPlaces = checkIfQuaysAlreadyPresentInOtherStopPlace(foundStopPlaces, incomingStopPlace);
                } catch (ExecutionException e) {
                    logger.error("Problem while moving/creating/deleting existing/new stop place", e);
                }
            }

            for (org.rutebanken.tiamat.model.StopPlace existingStopPlace : foundStopPlaces) {

                org.rutebanken.tiamat.model.StopPlace copy = versionCreator.createCopy(existingStopPlace, org.rutebanken.tiamat.model.StopPlace.class);

                logger.debug("Found matching stop place {}", existingStopPlace);

                boolean keyValuesChanged = (
                        keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_ID_KEY, incomingStopPlace, copy)
                                && keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_NAME_KEY, incomingStopPlace, copy)
                                && keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_STOPCODE_KEY, incomingStopPlace, copy)
                );

                if (incomingStopPlace.getTariffZones() != null) {
                    if (copy.getTariffZones() == null) {
                        copy.setTariffZones(new HashSet<>());
                    }
                    copy.getTariffZones().addAll(incomingStopPlace.getTariffZones());
                }

                boolean quayChanged = quayMerger.mergeQuays(incomingStopPlace, copy, CREATE_NEW_QUAYS, onMoveOnlyImport);

                boolean centroidChanged = stopPlaceCentroidComputer.computeCentroidForStopPlace(copy);

                boolean nameChanged = false;
                if(incomingStopPlace.getName() != null && !incomingStopPlace.getName().equals(existingStopPlace.getName()) && !onMoveOnlyImport){
                    copy.setName(incomingStopPlace.getName());
                    nameChanged = true;
                }

                boolean typeChanged = false;
                if (copy.getStopPlaceType() == null && incomingStopPlace.getStopPlaceType() != null) {
                    copy.setStopPlaceType(incomingStopPlace.getStopPlaceType());
                    logger.info("Updated stop place type to {} for stop place {}", copy.getStopPlaceType(), copy);
                    typeChanged = true;
                }

                boolean alternativeNameChanged = false;
                if(incomingStopPlace.getAlternativeNames() != null && incomingStopPlace.getAlternativeNames().size() != 0){
                    org.rutebanken.tiamat.model.StopPlace alternativeNamesToCopy = copy;
                    int sizeList = alternativeNamesToCopy.getAlternativeNames().size();
                    incomingStopPlace.getAlternativeNames().forEach(incomingAlternativeName -> {
                        if (!alternativeNamesToCopy.getAlternativeNames().contains(incomingAlternativeName)){
                            alternativeNamesToCopy.getAlternativeNames().add(incomingAlternativeName);
                        }
                    });
                    if(alternativeNamesToCopy.getAlternativeNames().size() != sizeList){
                        alternativeNameChanged = true;
                    }
                }

                if (quayChanged || keyValuesChanged || centroidChanged || typeChanged || alternativeNameChanged || nameChanged) {
                    copy = stopPlaceVersionedSaverService.saveNewVersion(existingStopPlace, copy);
                }

                String netexId = copy.getNetexId();

                matchedStopPlaces.removeIf(stopPlace -> stopPlace.getId().equals(netexId));

                matchedStopPlaces.add(netexMapper.mapToNetexModel(copy));

                stopPlacesCreatedOrUpdated.incrementAndGet();

            }
        }
    }

    private List<org.rutebanken.tiamat.model.StopPlace> checkIfQuaysAlreadyPresentInOtherStopPlace(List<org.rutebanken.tiamat.model.StopPlace> foundStopPlaces, org.rutebanken.tiamat.model.StopPlace incomingStopPlace) throws ExecutionException {
        quayMover.doMove(foundStopPlaces, incomingStopPlace);
        return stopPlaceByIdFinder.findStopPlace(incomingStopPlace);
    }
}
