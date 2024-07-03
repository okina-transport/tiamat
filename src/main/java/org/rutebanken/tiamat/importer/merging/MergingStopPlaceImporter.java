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

package org.rutebanken.tiamat.importer.merging;

import org.rutebanken.tiamat.exporter.params.TiamatVehicleModeStopPlacetypeMapping;
import org.rutebanken.tiamat.geo.StopPlaceCentroidComputer;
import org.rutebanken.tiamat.importer.finder.NearbyStopPlaceFinder;
import org.rutebanken.tiamat.importer.finder.StopPlaceFromOriginalIdFinder;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.QuaysVersionedSaverService;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Component
@Qualifier("mergingStopPlaceImporter")
@Transactional
public class MergingStopPlaceImporter {

    private static final Logger logger = LoggerFactory.getLogger(MergingStopPlaceImporter.class);

    private final StopPlaceFromOriginalIdFinder stopPlaceFromOriginalIdFinder;

    private final NearbyStopPlaceFinder nearbyStopPlaceFinder;

    private final StopPlaceCentroidComputer stopPlaceCentroidComputer;

    private final NetexMapper netexMapper;

    private final StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    private final VersionCreator versionCreator;

    private final ReferenceResolver referenceResolver;

    private final MergingUtils mergingUtils;

    private final QuaysVersionedSaverService quaysVersionedSaverService;

    @Autowired
    public MergingStopPlaceImporter(StopPlaceFromOriginalIdFinder stopPlaceFromOriginalIdFinder,
                                    NearbyStopPlaceFinder nearbyStopPlaceFinder,
                                    StopPlaceCentroidComputer stopPlaceCentroidComputer,
                                    NetexMapper netexMapper,
                                    StopPlaceVersionedSaverService stopPlaceVersionedSaverService,
                                    VersionCreator versionCreator,
                                    ReferenceResolver referenceResolver,
                                    MergingUtils mergingUtils,
                                    QuaysVersionedSaverService quaysVersionedSaverService) {
        this.stopPlaceFromOriginalIdFinder = stopPlaceFromOriginalIdFinder;
        this.nearbyStopPlaceFinder = nearbyStopPlaceFinder;
        this.stopPlaceCentroidComputer = stopPlaceCentroidComputer;
        this.netexMapper = netexMapper;
        this.stopPlaceVersionedSaverService = stopPlaceVersionedSaverService;
        this.versionCreator = versionCreator;
        this.referenceResolver = referenceResolver;
        this.mergingUtils = mergingUtils;
        this.quaysVersionedSaverService = quaysVersionedSaverService;
    }

    /**
     * When importing site frames in multiple threads, and those site frames might contain different stop places that will be merged,
     * we run into the risk of having multiple threads trying to save the same stop place.
     * <p>
     * That's why we use a striped semaphore to not work on the same stop place concurrently. (SiteFrameImporter)
     * it is important to flush the session between each stop place, *before* the semaphore has been released.
     * <p>
     * Attempts to use saveAndFlush or hibernate flush mode always have not been successful.
     */
    public org.rutebanken.netex.model.StopPlace importStopPlace(StopPlace newStopPlace, Boolean containsMobiitiIds) throws InterruptedException, ExecutionException {

        logger.debug("Transaction active: {}. Isolation level: {}", TransactionSynchronizationManager.isActualTransactionActive(), TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new RuntimeException("Transaction with required "
                    + "TransactionSynchronizationManager.isActualTransactionActive(): " + TransactionSynchronizationManager.isActualTransactionActive());
        }

        if (containsMobiitiIds) {
            return netexMapper.mapToNetexModel(importStopPlaceContainsMobiitiIdsWithoutNetexMapping(newStopPlace));
        }
        return netexMapper.mapToNetexModel(importStopPlaceWithoutNetexMapping(newStopPlace));
    }

    public StopPlace importStopPlaceWithoutNetexMapping(StopPlace incomingStopPlace) throws InterruptedException, ExecutionException {
        return handleCompletelyNewStopPlace(incomingStopPlace);
    }

    public StopPlace importStopPlaceContainsMobiitiIdsWithoutNetexMapping(StopPlace incomingStopPlace) {
        final StopPlace foundStopPlace = stopPlaceFromOriginalIdFinder.findStopPlace(incomingStopPlace);

        final StopPlace stopPlace;
        if (foundStopPlace != null) {
            stopPlace = handleAlreadyExistingStopPlaceContainsMobiitiIds(foundStopPlace, incomingStopPlace);

        } else {
            stopPlace = handleCompletelyNewStopPlaceContainsMobiitiIds(incomingStopPlace);
        }

        resolveAndFixParentSiteRef(stopPlace);

        return stopPlace;
    }

    public StopPlace handleCompletelyNewStopPlace(StopPlace incomingStopPlace) throws ExecutionException {

        if (incomingStopPlace.getNetexId() != null) {
            // This should not be necesarry.
            // Because this is a completely new stop.
            // And original netex ID should have been moved to key values.
            incomingStopPlace.setNetexId(null);
            if (incomingStopPlace.getQuays() != null) {
                incomingStopPlace.getQuays().forEach(q -> q.setNetexId(null));
            }
        }

        // TODO OKINA : to check
//        if (incomingStopPlace.getQuays() != null) {
//            Set<Quay> quays = quayMerger.mergeQuays(incomingStopPlace.getQuays(), null, new AtomicInteger(), new AtomicInteger(), ADD_NEW_QUAYS);
//            incomingStopPlace.setQuays(quays);
//            logger.trace("Importing quays for new stop place {}", incomingStopPlace);
//        }

        stopPlaceCentroidComputer.computeCentroidForStopPlace(incomingStopPlace);
        // Ignore incoming version. Always set version to 1 for new stop places.
        logger.debug("New stop place: {}. Setting version to \"1\"", incomingStopPlace.getName());
        versionCreator.createCopy(incomingStopPlace, StopPlace.class);
        StopTypeEnumeration incomingStopPlaceType = incomingStopPlace.getStopPlaceType();
        VehicleModeEnumeration incomingTransportMode = TiamatVehicleModeStopPlacetypeMapping.getVehicleModeEnumeration(incomingStopPlaceType);
        incomingStopPlace.setTransportMode(incomingTransportMode);

        incomingStopPlace = stopPlaceVersionedSaverService.saveNewVersion(incomingStopPlace);
        return updateCache(incomingStopPlace);
    }


    private StopPlace updateCache(StopPlace stopPlace) {
        // Keep the attached stop place reference in case it is merged.

        stopPlaceFromOriginalIdFinder.update(stopPlace);
        nearbyStopPlaceFinder.update(stopPlace);
        logger.info("Saved stop place {}", stopPlace);
        return stopPlace;
    }

    public StopPlace handleCompletelyNewStopPlaceContainsMobiitiIds(StopPlace incomingStopPlace) {
        stopPlaceCentroidComputer.computeCentroidForStopPlace(incomingStopPlace);
        // Ignore incoming version. Always set version to 1 for new stop places.
        logger.debug("New stop place: {}. Setting version to \"1\"", incomingStopPlace.getName());
        versionCreator.createCopy(incomingStopPlace, StopPlace.class);
        StopTypeEnumeration incomingStopPlaceType = incomingStopPlace.getStopPlaceType();
        VehicleModeEnumeration incomingTransportMode = TiamatVehicleModeStopPlacetypeMapping.getVehicleModeEnumeration(incomingStopPlaceType);
        incomingStopPlace.setTransportMode(incomingTransportMode);

        incomingStopPlace = stopPlaceVersionedSaverService.saveNewVersion(incomingStopPlace);
        return updateCache(incomingStopPlace);
    }

    public StopPlace handleAlreadyExistingStopPlaceContainsMobiitiIds(StopPlace existingStopPlace, StopPlace incomingStopPlace) {
        logger.debug("Found existing stop place {} from incoming {}", existingStopPlace, incomingStopPlace);

        StopPlace copyStopPlace = versionCreator.createCopy(existingStopPlace, StopPlace.class);
        String netexId = copyStopPlace.getNetexId();

        boolean validBetweenChanged = mergingUtils.updateProperty(copyStopPlace.getValidBetween(), incomingStopPlace.getValidBetween(), copyStopPlace::setValidBetween, "valid between", netexId);

        boolean keyValuesChanged = mergingUtils.updateKeyValues(copyStopPlace, incomingStopPlace, netexId);

        boolean nameChanged = mergingUtils.updateProperty(copyStopPlace.getName(), incomingStopPlace.getName(), copyStopPlace::setName, "name", netexId);
        boolean descriptionChanged = mergingUtils.updateProperty(copyStopPlace.getDescription(), incomingStopPlace.getDescription(), copyStopPlace::setDescription, "description", netexId);
        boolean centroidChanged = mergingUtils.updateProperty(copyStopPlace.getCentroid(), incomingStopPlace.getCentroid(), copyStopPlace::setCentroid, "centroid", netexId);

        boolean accessibilityAssessmentChanged = mergingUtils.updateAccessibilityAccessment(copyStopPlace, incomingStopPlace, netexId);

        boolean transportModeChanged = mergingUtils.updateProperty(copyStopPlace.getTransportMode(), incomingStopPlace.getTransportMode(), copyStopPlace::setTransportMode, "transport mode", netexId);
        boolean typeChanged = mergingUtils.updateProperty(copyStopPlace.getStopPlaceType(), incomingStopPlace.getStopPlaceType(), copyStopPlace::setStopPlaceType, "type", netexId);
        boolean weightingChanged = mergingUtils.updateProperty(copyStopPlace.getWeighting(), incomingStopPlace.getWeighting(), copyStopPlace::setWeighting, "weighting", netexId);

        boolean quaysChanged = false;
        if (incomingStopPlace.getQuays() != null && (!new HashSet<>(copyStopPlace.getQuays()).containsAll(incomingStopPlace.getQuays()) ||
                !new HashSet<>(incomingStopPlace.getQuays()).containsAll(copyStopPlace.getQuays()))) {
            Set<Quay> copyQuays = new HashSet<>();

            for (Quay quay : incomingStopPlace.getQuays()) {
                Optional<Quay> copyOptional = copyStopPlace.getQuays().stream()
                        .filter(copyQuay -> copyQuay.getNetexId().equals(quay.getNetexId()))
                        .findFirst();

                if (copyOptional.isPresent()) {
                    Quay copy = copyOptional.get();
                    mergingUtils.updateAccessibilityAccessment(copy, quay, netexId);
                    copyQuays.add(quaysVersionedSaverService.saveNewVersion(copy));
                } else {
                    copyQuays.add(quaysVersionedSaverService.saveNewVersion(quay));
                }
            }

            copyStopPlace.getQuays().clear();
            copyStopPlace.setQuays(copyQuays);
            logger.info("Updated quays for {}", netexId);
            quaysChanged = true;
        }

        if (validBetweenChanged || keyValuesChanged || nameChanged || descriptionChanged || centroidChanged || accessibilityAssessmentChanged ||
                transportModeChanged || typeChanged || weightingChanged || quaysChanged) {
            logger.info("Updated existing stop place {}. ", copyStopPlace);
            copyStopPlace = stopPlaceVersionedSaverService.saveNewVersion(existingStopPlace, copyStopPlace);
            return updateCache(copyStopPlace);
        }

        logger.debug("No changes. Returning existing stop place {}", existingStopPlace);
        return existingStopPlace;
    }

    private void resolveAndFixParentSiteRef(StopPlace stopPlace) {
        if (stopPlace != null && stopPlace.getParentSiteRef() != null) {
            DataManagedObjectStructure referencedStopPlace = referenceResolver.resolve(stopPlace.getParentSiteRef());
            stopPlace.getParentSiteRef().setRef(referencedStopPlace.getNetexId());
        }
    }
}
