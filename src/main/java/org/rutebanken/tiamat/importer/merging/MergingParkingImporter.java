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

import org.rutebanken.tiamat.importer.finder.NearbyParkingFinder;
import org.rutebanken.tiamat.importer.finder.ParkingFromOriginalIdFinder;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.rutebanken.tiamat.versioning.save.*;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

@Component
@Qualifier("mergingParkingImporter")
@Transactional
public class MergingParkingImporter {

    private static final Logger logger = LoggerFactory.getLogger(MergingParkingImporter.class);

    private final NetexMapper netexMapper;

    private final NearbyParkingFinder nearbyParkingFinder;

    private final ParkingVersionedSaverService parkingVersionedSaverService;

    private final ParkingPropertiesVersionedSaverService parkingPropertiesVersionedSaverService;

    private final ParkingAreasVersionedSaverService parkingAreasVersionedSaverService;

    private final ParkingPlaceEquipmentsVersionedSaverService parkingPlaceEquipmentsVersionedSaverService;

    private final ParkingInstalledEquipmentsVersionedSaverService parkingInstalledEquipmentsVersionedSaverService;

    private final ParkingFromOriginalIdFinder parkingFromOriginalIdFinder;

    private final ReferenceResolver referenceResolver;

    private final VersionCreator versionCreator;

    private final MergingUtils mergingUtils;

    @Autowired
    public MergingParkingImporter(ParkingFromOriginalIdFinder parkingFromOriginalIdFinder,
                                  NearbyParkingFinder nearbyParkingFinder,
                                  ReferenceResolver referenceResolver,
                                  NetexMapper netexMapper,
                                  ParkingVersionedSaverService parkingVersionedSaverService,
                                  ParkingPropertiesVersionedSaverService parkingPropertiesVersionedSaverService,
                                  ParkingAreasVersionedSaverService parkingAreasVersionedSaverService,
                                  ParkingPlaceEquipmentsVersionedSaverService parkingPlaceEquipmentsVersionedSaverService,
                                  ParkingInstalledEquipmentsVersionedSaverService parkingInstalledEquipmentsVersionedSaverService,
                                  VersionCreator versionCreator,
                                  MergingUtils mergingUtils) {
        this.parkingFromOriginalIdFinder = parkingFromOriginalIdFinder;
        this.nearbyParkingFinder = nearbyParkingFinder;
        this.referenceResolver = referenceResolver;
        this.netexMapper = netexMapper;
        this.parkingVersionedSaverService = parkingVersionedSaverService;
        this.parkingPropertiesVersionedSaverService = parkingPropertiesVersionedSaverService;
        this.parkingAreasVersionedSaverService = parkingAreasVersionedSaverService;
        this.parkingPlaceEquipmentsVersionedSaverService = parkingPlaceEquipmentsVersionedSaverService;
        this.parkingInstalledEquipmentsVersionedSaverService = parkingInstalledEquipmentsVersionedSaverService;
        this.versionCreator = versionCreator;
        this.mergingUtils = mergingUtils;
    }

    /**
     * When importing site frames in multiple threads, and those site frames might contain different parkings that will be merged,
     * we run into the risk of having multiple threads trying to save the same parking.
     * <p>
     * That's why we use a striped semaphore to not work on the same parking concurrently. (SiteFrameImporter)
     * it is important to flush the session between each parking, *before* the semaphore has been released.
     * <p>
     * Attempts to use saveAndFlush or hibernate flush mode always have not been successful.
     */
    public org.rutebanken.netex.model.Parking importParking(Parking parking) {

        logger.debug("Transaction active: {}. Isolation level: {}", TransactionSynchronizationManager.isActualTransactionActive(), TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new RuntimeException("Transaction with required "
                    + "TransactionSynchronizationManager.isActualTransactionActive(): " + TransactionSynchronizationManager.isActualTransactionActive());
        }

        return netexMapper.mapToNetexModel(importParkingWithoutNetexMapping(parking));
    }

    public Parking importParkingWithoutNetexMapping(Parking newParking) {
        final Parking foundParking = findNearbyOrExistingParking(newParking);

        final Parking parking;
        if (foundParking != null) {
            parking = handleAlreadyExistingParking(foundParking, newParking);
        } else {
            parking = handleCompletelyNewParking(newParking);
        }

        resolveAndFixParentSiteRef(parking);

        return parking;
    }

    private void resolveAndFixParentSiteRef(Parking parking) {
        if (parking != null && parking.getParentSiteRef() != null) {
            DataManagedObjectStructure referencedStopPlace = referenceResolver.resolve(parking.getParentSiteRef());
            parking.getParentSiteRef().setRef(referencedStopPlace.getNetexId());
        }
    }


    public Parking handleCompletelyNewParking(Parking incomingParking) {
        // Ignore incoming version. Always set version to 1 for new parkings.
        logger.debug("New parking: {}. Setting version to \"1\"", incomingParking.getName());
        // versionCreator.createCopy(incomingParking, Parking.class);

        incomingParking = parkingVersionedSaverService.saveNewVersion(incomingParking);
        return updateCache(incomingParking);
    }

    public Parking handleAlreadyExistingParking(Parking existingParking, Parking incomingParking) {
        logger.debug("Found existing parking {} from incoming {}", existingParking, incomingParking);

        Parking copyParking = versionCreator.createCopy(existingParking, Parking.class);
        String netexId = copyParking.getNetexId();

        boolean keyValuesChanged = mergingUtils.updateKeyValues(copyParking, incomingParking, netexId);

        boolean nameChanged = mergingUtils.updateProperty(copyParking.getName(), incomingParking.getName(), copyParking::setName, "name", netexId);
        boolean centroidChanged = mergingUtils.updateProperty(copyParking.getCentroid(), incomingParking.getCentroid(), copyParking::setCentroid, "centroid", netexId);
        boolean allAreasWheelchairAccessibleChanged = mergingUtils.updateProperty(copyParking.isAllAreasWheelchairAccessible(), incomingParking.isAllAreasWheelchairAccessible(), copyParking::setAllAreasWheelchairAccessible, "all areas weelchair accessible", netexId);
        boolean typeChanged = mergingUtils.updateProperty(copyParking.getParkingType(), incomingParking.getParkingType(), copyParking::setParkingType, "type", netexId);

        boolean vehicleType = false;
        if (!copyParking.getParkingVehicleTypes().containsAll(incomingParking.getParkingVehicleTypes()) ||
                        !incomingParking.getParkingVehicleTypes().containsAll(copyParking.getParkingVehicleTypes()) ) {

            copyParking.getParkingVehicleTypes().clear();
            copyParking.getParkingVehicleTypes().addAll(incomingParking.getParkingVehicleTypes());
            logger.info("Updated parking vehicle types to {} for parking {}", copyParking.getParkingVehicleTypes(), copyParking);
            vehicleType = true;
        }

        boolean totalCapacityChanged = mergingUtils.updateProperty(copyParking.getTotalCapacity(), incomingParking.getTotalCapacity(), copyParking::setTotalCapacity, "total capacity", netexId);
        boolean paymentProcessChanged = mergingUtils.updateProperty(copyParking.getParkingPaymentProcess(), incomingParking.getParkingPaymentProcess(), copyParking::setParkingPaymentProcess, "payment process", netexId);
        boolean rechargingAvailableChanged = mergingUtils.updateProperty(copyParking.isRechargingAvailable(), incomingParking.isRechargingAvailable(), copyParking::setRechargingAvailable, "recharging available", netexId);
        boolean bookingUrlChanged = mergingUtils.updateProperty(copyParking.getBookingUrl(), incomingParking.getBookingUrl(), copyParking::setBookingUrl, "booking url", netexId);

        boolean propertiesChanged = false;
        List<ParkingProperties> copyParkingProperty = new ArrayList<>();
        if (incomingParking.getParkingProperties() != null && (!new HashSet<>(copyParking.getParkingProperties()).containsAll(incomingParking.getParkingProperties()) ||
                !new HashSet<>(incomingParking.getParkingProperties()).containsAll(copyParking.getParkingProperties()))) {

            copyParking.getParkingProperties().clear();
            for (ParkingProperties property : incomingParking.getParkingProperties()) {
                copyParkingProperty.add(parkingPropertiesVersionedSaverService.saveNewVersion(property));
            }
            copyParking.getParkingProperties().addAll(copyParkingProperty);
            logger.info("Updated parking properties to {} for parking {}", copyParking.getParkingProperties(), copyParking);
            propertiesChanged = true;
        }

        boolean areasChanged = false;
        List<ParkingArea> copyParkingAreas = new ArrayList<>();
        if (((copyParking.getParkingAreas() != null && incomingParking.getParkingAreas() != null) ||
                (copyParking.getParkingAreas() == null && incomingParking.getParkingAreas() != null)) &&
                incomingParking.getParkingAreas().equals(copyParking.getParkingAreas())) {

            copyParking.getParkingAreas().clear();
            for (ParkingArea area : incomingParking.getParkingAreas()) {
                copyParkingAreas.add(parkingAreasVersionedSaverService.saveNewVersion(area));
            }
            copyParking.getParkingAreas().addAll(copyParkingAreas);
            logger.info("Updated areas to {} for parking {}", copyParking.getParkingAreas(), copyParking);
            areasChanged = true;
        }

        boolean equipmentChanged = false;
        List<InstalledEquipment_VersionStructure> copyEquipments = new ArrayList<>();
        if (incomingParking.getPlaceEquipments() != null &&
                (!new HashSet<>(copyParking.getPlaceEquipments().getInstalledEquipment()).containsAll(incomingParking.getPlaceEquipments().getInstalledEquipment()) ||
                !new HashSet<>(incomingParking.getPlaceEquipments().getInstalledEquipment()).containsAll(copyParking.getPlaceEquipments().getInstalledEquipment()))) {

            for (InstalledEquipment_VersionStructure cycleStorageEquipment : incomingParking.getPlaceEquipments().getInstalledEquipment()) {
                copyEquipments.add(parkingInstalledEquipmentsVersionedSaverService.saveNewVersion(cycleStorageEquipment));
            }
            copyParking.getPlaceEquipments().getInstalledEquipment().clear();
            for (InstalledEquipment_VersionStructure equip : copyEquipments) {
                copyParking.getPlaceEquipments().getInstalledEquipment().add(equip);
            }

            copyParking.setPlaceEquipments(parkingPlaceEquipmentsVersionedSaverService.saveNewVersion(copyParking.getPlaceEquipments()));
            logger.info("Updated equipments to {} for parking {}", copyParking.getPlaceEquipments(), copyParking);
            equipmentChanged = true;
        }

        if (keyValuesChanged || nameChanged || allAreasWheelchairAccessibleChanged || typeChanged || centroidChanged || vehicleType || totalCapacityChanged ||
                paymentProcessChanged || rechargingAvailableChanged || bookingUrlChanged || propertiesChanged || areasChanged || equipmentChanged) {
            logger.info("Updated existing parking {}. ", copyParking);
            copyParking = parkingVersionedSaverService.saveNewVersion(copyParking);
            return updateCache(copyParking);
        }

        logger.debug("No changes. Returning existing parking {}", existingParking);
        return existingParking;

    }

    private Parking updateCache(Parking parking) {
        // Keep the attached parking reference in case it is merged.

        parkingFromOriginalIdFinder.update(parking);
        nearbyParkingFinder.update(parking);
        logger.info("Saved parking {}", parking);
        return parking;
    }


    private Parking findNearbyOrExistingParking(Parking newParking) {
        final Parking existingParking = parkingFromOriginalIdFinder.find(newParking);
        if (existingParking != null) {
            return existingParking;
        }

        if (newParking.getName() != null) {
            final Parking nearbyParking = nearbyParkingFinder.find(newParking);
            if (nearbyParking != null) {
                logger.debug("Found nearby parking with name: {}, id: {}", nearbyParking.getName(), nearbyParking.getNetexId());
                return nearbyParking;
            }
        }
        return null;
    }

}
