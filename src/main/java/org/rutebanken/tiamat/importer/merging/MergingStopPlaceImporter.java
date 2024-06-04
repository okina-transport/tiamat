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
import org.rutebanken.tiamat.geo.ZoneDistanceChecker;
import org.rutebanken.tiamat.importer.KeyValueListAppender;
import org.rutebanken.tiamat.importer.finder.NearbyStopPlaceFinder;
import org.rutebanken.tiamat.importer.finder.NearbyStopsWithSameTypeFinder;
import org.rutebanken.tiamat.importer.finder.StopPlaceFromOriginalIdFinder;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
@Qualifier("mergingStopPlaceImporter")
@Transactional
public class MergingStopPlaceImporter {

    private static final Logger logger = LoggerFactory.getLogger(MergingStopPlaceImporter.class);

    /**
     * Enable short distance check for quay merging when merging existing stop places
     */
    public static final boolean EXISTING_STOP_QUAY_MERGE_SHORT_DISTANCE_CHECK_BEFORE_ID_MATCH = false;

    /**
     * Allow the quay merger to add new quays if no match found
     */
    public static final boolean ADD_NEW_QUAYS = true;

    private final StopPlaceFromOriginalIdFinder stopPlaceFromOriginalIdFinder;

    private final NearbyStopsWithSameTypeFinder nearbyStopsWithSameTypeFinder;

    private final NearbyStopPlaceFinder nearbyStopPlaceFinder;

    private final StopPlaceCentroidComputer stopPlaceCentroidComputer;

    private final KeyValueListAppender keyValueListAppender;

    private final QuayMerger quayMerger;

    private final NetexMapper netexMapper;

    private final StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    private final ZoneDistanceChecker zoneDistanceChecker;

    private final VersionCreator versionCreator;

    private final StopPlaceRepository stopPlaceRepository;

    private final ReferenceResolver referenceResolver;

    @Autowired
    public MergingStopPlaceImporter(StopPlaceFromOriginalIdFinder stopPlaceFromOriginalIdFinder,
                                    NearbyStopsWithSameTypeFinder nearbyStopsWithSameTypeFinder, NearbyStopPlaceFinder nearbyStopPlaceFinder,
                                    StopPlaceCentroidComputer stopPlaceCentroidComputer,
                                    KeyValueListAppender keyValueListAppender, QuayMerger quayMerger, NetexMapper netexMapper,
                                    StopPlaceVersionedSaverService stopPlaceVersionedSaverService, ZoneDistanceChecker zoneDistanceChecker, VersionCreator versionCreator, StopPlaceRepository stopPlaceRepository, ReferenceResolver referenceResolver) {
        this.stopPlaceFromOriginalIdFinder = stopPlaceFromOriginalIdFinder;
        this.nearbyStopsWithSameTypeFinder = nearbyStopsWithSameTypeFinder;
        this.nearbyStopPlaceFinder = nearbyStopPlaceFinder;
        this.stopPlaceCentroidComputer = stopPlaceCentroidComputer;
        this.keyValueListAppender = keyValueListAppender;
        this.quayMerger = quayMerger;
        this.netexMapper = netexMapper;
        this.stopPlaceVersionedSaverService = stopPlaceVersionedSaverService;
        this.zoneDistanceChecker = zoneDistanceChecker;
        this.versionCreator = versionCreator;
        this.stopPlaceRepository = stopPlaceRepository;
        this.referenceResolver = referenceResolver;
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
    public org.rutebanken.netex.model.StopPlace importStopPlace(StopPlace newStopPlace) throws InterruptedException, ExecutionException {

        logger.debug("Transaction active: {}. Isolation level: {}", TransactionSynchronizationManager.isActualTransactionActive(), TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new RuntimeException("Transaction with required "
                    + "TransactionSynchronizationManager.isActualTransactionActive(): " + TransactionSynchronizationManager.isActualTransactionActive());
        }

        return netexMapper.mapToNetexModel(importStopPlaceWithoutNetexMapping(newStopPlace));
    }

    public StopPlace importStopPlaceWithoutNetexMapping(StopPlace incomingStopPlace) throws InterruptedException, ExecutionException {

        final StopPlace foundStopPlace = findNearbyOrExistingStopPlace(incomingStopPlace);

        final StopPlace stopPlace;
        if (foundStopPlace != null) {
            stopPlace = handleAlreadyExistingStopPlace(foundStopPlace, incomingStopPlace);

        } else {
            stopPlace = handleCompletelyNewStopPlace(incomingStopPlace, false);
        }

        resolveAndFixParentSiteRef(stopPlace);

        return stopPlace;
    }

    private void resolveAndFixParentSiteRef(StopPlace stopPlace) {
        if (stopPlace != null && stopPlace.getParentSiteRef() != null) {
            DataManagedObjectStructure referencedStopPlace = referenceResolver.resolve(stopPlace.getParentSiteRef());
            stopPlace.getParentSiteRef().setRef(referencedStopPlace.getNetexId());
        }
    }

    public StopPlace handleCompletelyNewStopPlace(StopPlace incomingStopPlace, Boolean resetNetexId) throws ExecutionException {

        if (incomingStopPlace.getNetexId() != null && resetNetexId) {
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

    private StopPlace findNearbyOrExistingStopPlace(StopPlace newStopPlace) {
        final StopPlace existingStopPlace = stopPlaceFromOriginalIdFinder.findStopPlace(newStopPlace);
        if (existingStopPlace != null) {
            return existingStopPlace;
        }

        if (newStopPlace.getName() != null) {
            final StopPlace nearbyStopPlace = nearbyStopPlaceFinder.find(newStopPlace);
            if (nearbyStopPlace != null) {
                logger.debug("Found nearby stop place with name: {}, id: {}", nearbyStopPlace.getName(), nearbyStopPlace.getNetexId());
                return nearbyStopPlace;
            }
        }
        return null;
    }

    public StopPlace handleAlreadyExistingStopPlace(StopPlace existingStopPlace, StopPlace incomingStopPlace) {
        logger.debug("Found existing stop place {} from incoming {}", existingStopPlace, incomingStopPlace);

        StopPlace copyStopPlace = versionCreator.createCopy(existingStopPlace, StopPlace.class);

        boolean keyValuesChanged = false;
        if ((copyStopPlace.getKeyValues() == null && incomingStopPlace.getKeyValues() != null) ||
                (copyStopPlace.getKeyValues() != null && incomingStopPlace.getKeyValues() != null
                        && !copyStopPlace.getKeyValues().equals(incomingStopPlace.getKeyValues()))) {
            for (Map.Entry<String, Value> entry : incomingStopPlace.getKeyValues().entrySet()) {
                keyValueListAppender.appendKeyValue(entry.getKey(), incomingStopPlace, copyStopPlace);
            }
            keyValuesChanged = true;
        }

        boolean centroidChanged = false;
        if ((copyStopPlace.getCentroid() == null && incomingStopPlace.getCentroid() != null) ||
                (copyStopPlace.getCentroid() != null && incomingStopPlace.getCentroid() != null
                        && !copyStopPlace.getCentroid().equals(incomingStopPlace.getCentroid()))) {
            copyStopPlace.setCentroid(incomingStopPlace.getCentroid());
            centroidChanged = true;
        }

        boolean allAreasWheelchairAccessibleChanged = false;
        if ((copyStopPlace.isAllAreasWheelchairAccessible() == null && incomingStopPlace.isAllAreasWheelchairAccessible() != null) ||
                (copyStopPlace.isAllAreasWheelchairAccessible() != null && incomingStopPlace.isAllAreasWheelchairAccessible() != null
                        && !copyStopPlace.isAllAreasWheelchairAccessible().equals(incomingStopPlace.isAllAreasWheelchairAccessible()))) {

            copyStopPlace.setAllAreasWheelchairAccessible(incomingStopPlace.isAllAreasWheelchairAccessible());
            logger.info("Updated allAreasWheelchairAccessible value to {} for parking {}", copyStopPlace.isAllAreasWheelchairAccessible(), copyStopPlace);
            allAreasWheelchairAccessibleChanged = true;
        }

//        boolean typeChanged = false;
//        if ((copyStopPlace.getParkingType() == null && incomingStopPlace.getParkingType() != null) ||
//                (copyStopPlace.getParkingType() != null && incomingStopPlace.getParkingType() != null
//                        && !copyStopPlace.getParkingType().equals(incomingStopPlace.getParkingType()))) {
//
//            copyStopPlace.setParkingType(incomingStopPlace.getParkingType());
//            logger.info("Updated parking type to {} for parking {}", copyStopPlace.getParkingType(), copyStopPlace);
//            typeChanged = true;
//        }
//
//        boolean vehicleType = false;
//        if (!copyStopPlace.getParkingVehicleTypes().containsAll(incomingStopPlace.getParkingVehicleTypes()) ||
//                !incomingStopPlace.getParkingVehicleTypes().containsAll(copyStopPlace.getParkingVehicleTypes()) ) {
//
//            copyStopPlace.getParkingVehicleTypes().clear();
//            copyStopPlace.getParkingVehicleTypes().addAll(incomingStopPlace.getParkingVehicleTypes());
//            logger.info("Updated parking vehicle types to {} for parking {}", copyStopPlace.getParkingVehicleTypes(), copyStopPlace);
//            vehicleType = true;
//        }
//
//        boolean totalCapacityChanged = false;
//        if ((copyStopPlace.getTotalCapacity() == null && incomingStopPlace.getTotalCapacity() != null) ||
//                (copyStopPlace.getTotalCapacity() != null && incomingStopPlace.getTotalCapacity() != null
//                        && !copyStopPlace.getTotalCapacity().equals(incomingStopPlace.getTotalCapacity()))) {
//
//            copyStopPlace.setTotalCapacity(incomingStopPlace.getTotalCapacity());
//            logger.info("Updated total capacity type to {} for parking {}", copyStopPlace.getTotalCapacity(), copyStopPlace);
//            totalCapacityChanged = true;
//        }
//
//        boolean rechargingAvailableChanged = false;
//        if ((copyStopPlace.isRechargingAvailable() == null && incomingStopPlace.isRechargingAvailable() != null) ||
//                (copyStopPlace.isRechargingAvailable() != null && incomingStopPlace.isRechargingAvailable() != null
//                        && !copyStopPlace.isRechargingAvailable().equals(incomingStopPlace.isRechargingAvailable()))) {
//
//            copyStopPlace.setRechargingAvailable(incomingStopPlace.isRechargingAvailable());
//            logger.info("Updated recharging available type to {} for parking {}", copyStopPlace.isRechargingAvailable(), copyStopPlace);
//            rechargingAvailableChanged = true;
//        }
//
//        boolean bookingUrlChanged = false;
//        if ((copyStopPlace.getBookingUrl() == null && incomingStopPlace.getBookingUrl() != null) ||
//                (copyStopPlace.getBookingUrl() != null && incomingStopPlace.getBookingUrl() != null
//                        && !copyStopPlace.getBookingUrl().equals(incomingStopPlace.getBookingUrl()))) {
//
//            copyStopPlace.setBookingUrl(incomingStopPlace.getBookingUrl());
//            logger.info("Updated booking url type to {} for parking {}", copyStopPlace.getBookingUrl(), copyStopPlace);
//            bookingUrlChanged = true;
//        }
//
//        boolean propertiesChanged = false;
//        List<ParkingProperties> copyParkingProperty = new ArrayList<>();
//        if (incomingStopPlace.getParkingProperties() != null && (!new HashSet<>(copyStopPlace.getParkingProperties()).containsAll(incomingStopPlace.getParkingProperties()) ||
//                !new HashSet<>(incomingStopPlace.getParkingProperties()).containsAll(copyStopPlace.getParkingProperties()))) {
//
//            copyStopPlace.getParkingProperties().clear();
//            for (ParkingProperties property : incomingStopPlace.getParkingProperties()) {
//                copyParkingProperty.add(parkingPropertiesVersionedSaverService.saveNewVersion(property));
//            }
//            copyStopPlace.getParkingProperties().addAll(copyParkingProperty);
//            logger.info("Updated parking properties to {} for parking {}", copyStopPlace.getParkingProperties(), copyStopPlace);
//            propertiesChanged = true;
//        }
//
//        boolean areasChanged = false;
//        List<ParkingArea> copyParkingAreas = new ArrayList<>();
//        if (incomingStopPlace.getParkingAreas()!= null && (!new HashSet<>(copyStopPlace.getParkingAreas()).containsAll(incomingStopPlace.getParkingAreas()) ||
//                !new HashSet<>(incomingStopPlace.getParkingAreas()).containsAll(copyStopPlace.getParkingAreas()))) {
//
//            copyStopPlace.getParkingAreas().clear();
//            for (ParkingArea area : incomingStopPlace.getParkingAreas()) {
//                copyParkingAreas.add(parkingAreasVersionedSaverService.saveNewVersion(area));
//            }
//            copyStopPlace.getParkingAreas().addAll(copyParkingAreas);
//            logger.info("Updated areas to {} for parking {}", copyStopPlace.getParkingAreas(), copyStopPlace);
//            areasChanged = true;
//        }
//
//        boolean equipmentChanged = false;
//        List<InstalledEquipment_VersionStructure> copyEquipments = new ArrayList<>();
//        if (incomingStopPlace.getPlaceEquipments() != null &&
//                (!new HashSet<>(copyStopPlace.getPlaceEquipments().getInstalledEquipment()).containsAll(incomingStopPlace.getPlaceEquipments().getInstalledEquipment()) ||
//                        !new HashSet<>(incomingStopPlace.getPlaceEquipments().getInstalledEquipment()).containsAll(copyStopPlace.getPlaceEquipments().getInstalledEquipment()))) {
//
//            for (InstalledEquipment_VersionStructure cycleStorageEquipment : incomingStopPlace.getPlaceEquipments().getInstalledEquipment()) {
//                copyEquipments.add(parkingInstalledEquipmentsVersionedSaverService.saveNewVersion(cycleStorageEquipment));
//            }
//            copyStopPlace.getPlaceEquipments().getInstalledEquipment().clear();
//            for (InstalledEquipment_VersionStructure equip : copyEquipments) {
//                copyStopPlace.getPlaceEquipments().getInstalledEquipment().add(equip);
//            }
//
//            copyStopPlace.setPlaceEquipments(parkingPlaceEquipmentsVersionedSaverService.saveNewVersion(copyStopPlace.getPlaceEquipments()));
//            logger.info("Updated equipments to {} for parking {}", copyStopPlace.getPlaceEquipments(), copyStopPlace);
//            equipmentChanged = true;
//        }

//        if (keyValuesChanged || allAreasWheelchairAccessibleChanged || typeChanged || centroidChanged || vehicleType || totalCapacityChanged ||
//                rechargingAvailableChanged || bookingUrlChanged || propertiesChanged || areasChanged || equipmentChanged) {
            logger.info("Updated existing parking {}. ", copyStopPlace);
            copyStopPlace = stopPlaceVersionedSaverService.saveNewVersion(copyStopPlace);
            return updateCache(copyStopPlace);
//        }

//        logger.debug("No changes. Returning existing parking {}", existingStopPlace);
//        return existingStopPlace;

    }
}
