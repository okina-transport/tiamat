package org.rutebanken.tiamat.service.parking;

import org.rutebanken.tiamat.general.ParkingsCSVHelper;
import org.rutebanken.tiamat.model.AccessibilityAssessment;
import org.rutebanken.tiamat.model.AccessibilityLimitation;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingPaymentProcessEnumeration;
import org.rutebanken.tiamat.model.ParkingProperties;
import org.rutebanken.tiamat.model.ParkingVehicleEnumeration;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.ParkingVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


@Service
@Transactional
public class BikeParkingsImportedService {

    private static final Logger logger = LoggerFactory.getLogger(ParkingsImportedService.class);

    private ParkingRepository parkingRepository;
    private NetexIdMapper netexIdMapper;
    private ParkingVersionedSaverService parkingVersionedSaverService;
    private VersionCreator versionCreator;

    @Autowired
    BikeParkingsImportedService(ParkingRepository parkingRepository, NetexIdMapper netexIdMapper, ParkingVersionedSaverService parkingVersionedSaverService, VersionCreator versionCreator) {
        this.parkingRepository = parkingRepository;
        this.netexIdMapper = netexIdMapper;
        this.parkingVersionedSaverService = parkingVersionedSaverService;
        this.versionCreator = versionCreator;
    }

    public void createOrUpdateBikeParkings(List<Parking> bikeParkingsToSave) {

        Parking updatedParking;
        boolean founded = false;

        for (Parking bikeParkingToSave : bikeParkingsToSave) {

            Parking parkingInBDD = retrieveBikeParkingInBDD(bikeParkingToSave);

            if (parkingInBDD != null && parkingInBDD.getNetexId() != null) {
                founded = true;
                updatedParking = versionCreator.createCopy(parkingInBDD, Parking.class);

                boolean isParkingUpdated = populateParking(parkingInBDD, updatedParking);

                if (isParkingUpdated) {
                    parkingVersionedSaverService.saveNewVersion(updatedParking);
                }
            }

            if (!founded) {
                netexIdMapper.moveOriginalNameToKeyValueList(bikeParkingToSave, bikeParkingToSave.getName().getValue());

                bikeParkingToSave.setName(new EmbeddableMultilingualString(bikeParkingToSave.getName().getValue()));
                parkingVersionedSaverService.saveNewVersion(bikeParkingToSave);
            }
        }
    }

    private Parking retrieveBikeParkingInBDD(Parking parking) {
        String parkingId = getIdAvailableInCSV(parking.getName().getValue());

        Set values= new HashSet(Arrays.asList(parkingId));

        String parkingNetexId = parkingRepository.findFirstByKeyValues(NetexIdMapper.ORIGINAL_ID_KEY, values);

        if (parkingNetexId != null && !parkingNetexId.isEmpty()) {
            return parkingRepository.findFirstByNetexIdOrderByVersionDesc(parkingNetexId);
        }

        return null;

    }

    private String getIdAvailableInCSV(String parkingName){
        return parkingName.split(ParkingsCSVHelper.DELIMETER_PARKING_ID_NAME)[0];
    }


    private boolean populateParking(Parking existingBikeParking, Parking updatedBikeParking) {
        boolean isUpdated = false;
        if (existingBikeParking.getName() != null) {
            updatedBikeParking.setName(existingBikeParking.getName());
            isUpdated = true;
        }

        if (existingBikeParking.getValidBetween() != null) {
            updatedBikeParking.setValidBetween(existingBikeParking.getValidBetween());
            isUpdated = true;
        }

        if (existingBikeParking.getCentroid() != null) {
            updatedBikeParking.setCentroid(existingBikeParking.getCentroid());
            isUpdated = true;
        }

        if (existingBikeParking.getParentSiteRef() != null) {
            updatedBikeParking.setParentSiteRef(existingBikeParking.getParentSiteRef());
            isUpdated = true;
        }

        if (existingBikeParking.getTotalCapacity() != null) {
            updatedBikeParking.setTotalCapacity(existingBikeParking.getTotalCapacity());
            isUpdated = true;
        }

        if (existingBikeParking.getPrincipalCapacity() != null) {
            updatedBikeParking.setPrincipalCapacity(existingBikeParking.getPrincipalCapacity());
            isUpdated = true;
        }

        if (existingBikeParking.getParkingType() != null) {
            updatedBikeParking.setParkingType(existingBikeParking.getParkingType());
        }
        if (existingBikeParking.getParkingVehicleTypes() != null) {
            List<ParkingVehicleEnumeration> vehicleTypes = existingBikeParking.getParkingVehicleTypes();

            updatedBikeParking.getParkingVehicleTypes().clear();
            updatedBikeParking.getParkingVehicleTypes().addAll(vehicleTypes);

            isUpdated = true;
        }

        if (existingBikeParking.getParkingLayout() != null) {
            updatedBikeParking.setParkingLayout(existingBikeParking.getParkingLayout());
            isUpdated = true;
        }

        if (existingBikeParking.isOvernightParkingPermitted() != null) {
            updatedBikeParking.setOvernightParkingPermitted(existingBikeParking.isOvernightParkingPermitted());
            isUpdated = true;
        }

        if (existingBikeParking.isRechargingAvailable() != null) {
            updatedBikeParking.setRechargingAvailable(existingBikeParking.isRechargingAvailable());
            isUpdated = true;
        }

        if (existingBikeParking.isCarpoolingAvailable() != null) {
            updatedBikeParking.setCarpoolingAvailable(existingBikeParking.isRechargingAvailable());
            isUpdated = true;
        }

        if (existingBikeParking.isCarsharingAvailable() != null) {
            updatedBikeParking.setCarsharingAvailable(existingBikeParking.isCarsharingAvailable());
        }

        if (existingBikeParking.isSecure() != null) {
            updatedBikeParking.setSecure(existingBikeParking.isSecure());
        }

        if (existingBikeParking.isRealTimeOccupancyAvailable() != null) {
            updatedBikeParking.setRealTimeOccupancyAvailable(existingBikeParking.isRealTimeOccupancyAvailable());
            isUpdated = true;
        }

        if (existingBikeParking.isFreeParkingOutOfHours() != null) {
            updatedBikeParking.setFreeParkingOutOfHours(existingBikeParking.isFreeParkingOutOfHours());
        }

        if (existingBikeParking.getParkingPaymentProcess() != null) {

            List<ParkingPaymentProcessEnumeration> parkingPaymentProcessTypes = existingBikeParking.getParkingPaymentProcess();

            updatedBikeParking.getParkingPaymentProcess().clear();
            updatedBikeParking.getParkingPaymentProcess().addAll(parkingPaymentProcessTypes);

            isUpdated = true;
        }

        if (existingBikeParking.getParkingReservation() != null) {
            updatedBikeParking.setParkingReservation(existingBikeParking.getParkingReservation());
        }

        if (existingBikeParking.getBookingUrl() != null) {
            updatedBikeParking.setBookingUrl(existingBikeParking.getBookingUrl());
        }

        if (existingBikeParking.getParkingProperties() != null) {
            List<ParkingProperties> parkingPropertiesList = existingBikeParking.getParkingProperties();
            int total_capacity = parkingPropertiesList.stream()
                    .map(ParkingProperties::getSpaces)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(space -> space.getNumberOfSpaces() != null)
                    .mapToInt(space -> space.getNumberOfSpaces().intValue())
                    .sum();
            isUpdated = true;
            updatedBikeParking.getParkingProperties().clear();
            updatedBikeParking.getParkingProperties().addAll(parkingPropertiesList);
            if (total_capacity > 0) {
                updatedBikeParking.setTotalCapacity(BigInteger.valueOf(total_capacity));
            }
        }

        if (existingBikeParking.getParkingAreas() != null) {
            updatedBikeParking.getParkingAreas().clear();
            updatedBikeParking.getParkingAreas().addAll(existingBikeParking.getParkingAreas());
            isUpdated = true;
        }

        if (existingBikeParking.getAccessibilityAssessment() != null) {

            List<AccessibilityLimitation> limitations = existingBikeParking.getAccessibilityAssessment().getLimitations();

            AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
            accessibilityAssessment.setMobilityImpairedAccess(existingBikeParking.getAccessibilityAssessment().getMobilityImpairedAccess());

            AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();

            accessibilityLimitation.setWheelchairAccess(limitations.stream().findFirst().get().getWheelchairAccess());
            accessibilityLimitation.setAudibleSignalsAvailable(limitations.stream().findFirst().get().getAudibleSignalsAvailable());
            accessibilityLimitation.setEscalatorFreeAccess(limitations.stream().findFirst().get().getEscalatorFreeAccess());
            accessibilityLimitation.setLiftFreeAccess(limitations.stream().findFirst().get().getLiftFreeAccess());
            accessibilityLimitation.setStepFreeAccess(limitations.stream().findFirst().get().getStepFreeAccess());
            accessibilityLimitation.setVisualSignsAvailable(limitations.stream().findFirst().get().getVisualSignsAvailable());

            accessibilityAssessment.setLimitations(Arrays.asList(accessibilityLimitation));

            updatedBikeParking.setAccessibilityAssessment(accessibilityAssessment);
            isUpdated = true;

        }
        return isUpdated;
    }
}
