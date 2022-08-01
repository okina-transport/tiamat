package org.rutebanken.tiamat.service.parking;

import org.rutebanken.tiamat.general.ParkingsCSVHelper;
import org.rutebanken.tiamat.model.*;
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
import java.util.*;


@Service
@Transactional
public class ParkingsImportedService {

    private static final Logger logger = LoggerFactory.getLogger(ParkingsImportedService.class);

    private ParkingRepository parkingRepository;
    private NetexIdMapper netexIdMapper;
    private ParkingVersionedSaverService parkingVersionedSaverService;
    private VersionCreator versionCreator;

    @Autowired
    ParkingsImportedService(ParkingRepository parkingRepository, NetexIdMapper netexIdMapper, ParkingVersionedSaverService parkingVersionedSaverService, VersionCreator versionCreator){
        this.parkingRepository = parkingRepository;
        this.netexIdMapper = netexIdMapper;
        this.parkingVersionedSaverService = parkingVersionedSaverService;
        this.versionCreator = versionCreator;
    }

    public void createOrUpdateParkings(List<Parking> parkingsToSave){

        Parking updatedParking;
        boolean founded = false;

        for(Parking parkingToSave: parkingsToSave){

            Parking parkingInBDD = retrieveParkingInBDD(parkingToSave);

            if(parkingInBDD != null && parkingInBDD.getNetexId() != null){
                founded = true;
                updatedParking = versionCreator.createCopy(parkingInBDD, Parking.class);

                boolean isParkingUpdated = populateParking(parkingInBDD, updatedParking);

                if(isParkingUpdated) {
                    parkingVersionedSaverService.saveNewVersion(updatedParking);
                }
            }

            if(!founded){
                netexIdMapper.moveOriginalIdToKeyValueList(parkingToSave, parkingToSave.getOriginalId());
                netexIdMapper.moveOriginalNameToKeyValueList(parkingToSave, parkingToSave.getName().getValue());

                parkingToSave.setName(new EmbeddableMultilingualString(parkingToSave.getName().getValue()));
                parkingVersionedSaverService.saveNewVersion(parkingToSave);
            }
        }
    }

    private Parking retrieveParkingInBDD(Parking parking){

        String parkingId = getIdAvailableInCSV(parking.getName().getValue());

        Set values = new HashSet(Arrays.asList(parkingId));

        String parkingNetexId = parkingRepository.findFirstByKeyValues(NetexIdMapper.ORIGINAL_ID_KEY, values);

        if(parkingNetexId != null && !parkingNetexId.isEmpty()) {
            return parkingRepository.findFirstByNetexIdOrderByVersionDesc(parkingNetexId);
        }

        return null;

    }

    private String getNameAvailableInCSV(String parkingName){
        return parkingName.split(ParkingsCSVHelper.DELIMETER_PARKING_ID_NAME)[1];
    }

    private String getIdAvailableInCSV(String parkingName){
        return parkingName.split(ParkingsCSVHelper.DELIMETER_PARKING_ID_NAME)[0];
    }


    private boolean populateParking(Parking existingParking, Parking updatedParking) {
        boolean isUpdated = false;
        if (existingParking.getName() != null) {
            updatedParking.setName(existingParking.getName());
            isUpdated = true;
        }

        if (existingParking.getValidBetween()!= null) {
            updatedParking.setValidBetween(existingParking.getValidBetween());
            isUpdated = true;
        }

        if (existingParking.getCentroid() != null) {
            updatedParking.setCentroid(existingParking.getCentroid());
            isUpdated = true;
        }

        if (existingParking.getParentSiteRef() != null) {
            updatedParking.setParentSiteRef(existingParking.getParentSiteRef());
            isUpdated = true;
        }

        if (existingParking.getTotalCapacity() != null) {
            updatedParking.setTotalCapacity(existingParking.getTotalCapacity());
            isUpdated = true;
        }

        if (existingParking.getPrincipalCapacity() != null) {
            updatedParking.setPrincipalCapacity(existingParking.getPrincipalCapacity());
            isUpdated = true;
        }

        if (existingParking.getParkingType() != null) {
            updatedParking.setParkingType(existingParking.getParkingType());
        }
        if (existingParking.getParkingVehicleTypes() != null) {
            List<ParkingVehicleEnumeration> vehicleTypes = existingParking.getParkingVehicleTypes();

            updatedParking.getParkingVehicleTypes().clear();
            updatedParking.getParkingVehicleTypes().addAll(vehicleTypes);

            isUpdated = true;
        }

        if (existingParking.getParkingLayout() != null) {
            updatedParking.setParkingLayout(existingParking.getParkingLayout());
            isUpdated = true;
        }

        if (existingParking.isOvernightParkingPermitted() != null) {
            updatedParking.setOvernightParkingPermitted(existingParking.isOvernightParkingPermitted());
            isUpdated = true;
        }

        if (existingParking.isRechargingAvailable() != null) {
            updatedParking.setRechargingAvailable(existingParking.isRechargingAvailable());
            isUpdated = true;
        }

        if (existingParking.isCarpoolingAvailable() != null) {
            updatedParking.setCarpoolingAvailable(existingParking.isRechargingAvailable());
            isUpdated = true;
        }

        if (existingParking.isCarsharingAvailable() != null) {
            updatedParking.setCarsharingAvailable(existingParking.isCarsharingAvailable());
        }

        if (existingParking.isSecure() != null) {
            updatedParking.setSecure(existingParking.isSecure());
        }

        if (existingParking.isRealTimeOccupancyAvailable() != null) {
            updatedParking.setRealTimeOccupancyAvailable(existingParking.isRealTimeOccupancyAvailable());
            isUpdated = true;
        }

        if (existingParking.isFreeParkingOutOfHours() != null) {
            updatedParking.setFreeParkingOutOfHours(existingParking.isFreeParkingOutOfHours());
        }

        if (existingParking.getParkingPaymentProcess() != null) {

            List<ParkingPaymentProcessEnumeration> parkingPaymentProcessTypes = existingParking.getParkingPaymentProcess();

            updatedParking.getParkingPaymentProcess().clear();
            updatedParking.getParkingPaymentProcess().addAll(parkingPaymentProcessTypes);

            isUpdated = true;
        }

        if (existingParking.getParkingReservation() != null) {
            updatedParking.setParkingReservation(existingParking.getParkingReservation());
        }

        if (existingParking.getBookingUrl() != null) {
            updatedParking.setBookingUrl(existingParking.getBookingUrl());
        }

        if (existingParking.getParkingProperties() != null) {
            List<ParkingProperties> parkingPropertiesList = existingParking.getParkingProperties();
            int total_capacity = parkingPropertiesList.stream()
                    .map(ParkingProperties::getSpaces)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(space -> space.getNumberOfSpaces() != null)
                    .mapToInt(space -> space.getNumberOfSpaces().intValue())
                    .sum();
            isUpdated = true;
            updatedParking.getParkingProperties().clear();
            updatedParking.getParkingProperties().addAll(parkingPropertiesList);
            if (total_capacity > 0) {
                updatedParking.setTotalCapacity(BigInteger.valueOf(total_capacity));
            }
        }

        if (existingParking.getParkingAreas() != null) {
            updatedParking.getParkingAreas().clear();
            updatedParking.getParkingAreas().addAll(existingParking.getParkingAreas());
            isUpdated = true;
        }

        if(existingParking.getAccessibilityAssessment() != null){

            List<AccessibilityLimitation> limitations = existingParking.getAccessibilityAssessment().getLimitations();

            AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
            accessibilityAssessment.setMobilityImpairedAccess(existingParking.getAccessibilityAssessment().getMobilityImpairedAccess());

            AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();

            accessibilityLimitation.setWheelchairAccess(limitations.stream().findFirst().get().getWheelchairAccess());
            accessibilityLimitation.setAudibleSignalsAvailable(limitations.stream().findFirst().get().getAudibleSignalsAvailable());
            accessibilityLimitation.setEscalatorFreeAccess(limitations.stream().findFirst().get().getEscalatorFreeAccess());
            accessibilityLimitation.setLiftFreeAccess(limitations.stream().findFirst().get().getLiftFreeAccess());
            accessibilityLimitation.setStepFreeAccess(limitations.stream().findFirst().get().getStepFreeAccess());
            accessibilityLimitation.setVisualSignsAvailable(limitations.stream().findFirst().get().getVisualSignsAvailable());

            accessibilityAssessment.setLimitations(Arrays.asList(accessibilityLimitation));

            updatedParking.setAccessibilityAssessment(accessibilityAssessment);
            isUpdated = true;

        }
        return isUpdated;
    }

    public void clearAllRentalBikes(){
        parkingRepository.clearAllRentalbikeParkings();
    }


}
