package org.rutebanken.tiamat.service.parking;

import jakarta.transaction.Transactional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.rutebanken.tiamat.client.mdm.ParkingIdentifier;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.model.AccessibilityAssessment;
import org.rutebanken.tiamat.model.AccessibilityLimitation;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Organisation;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingArea;
import org.rutebanken.tiamat.model.ParkingCapacity;
import org.rutebanken.tiamat.model.ParkingPaymentProcessEnumeration;
import org.rutebanken.tiamat.model.ParkingProperties;
import org.rutebanken.tiamat.model.ParkingVehicleEnumeration;
import org.rutebanken.tiamat.model.identification.IdentifiedEntity;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.ParkingPlaceEquipmentsRepository;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.service.merge.PlaceEquipmentMerger;
import org.rutebanken.tiamat.versioning.save.ParkingVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;


@Service
@Transactional
public class ParkingsImportedService {

    private static final Logger log = LoggerFactory.getLogger(ParkingsImportedService.class);
    private final ParkingRepository parkingRepository;
    private final NetexIdMapper netexIdMapper;
    private final ParkingVersionedSaverService parkingVersionedSaverService;
    private final OrganisationsImportedService organisationsImportedService;
    private final ParkingPlaceEquipmentsRepository parkingPlaceEquipmentsRepository;
    private final MdmService mdmService;

    @org.springframework.beans.factory.annotation.Value("${netex.validPrefix:MOBIITI}")
    String validNetexPrefix;

    ParkingsImportedService(ParkingRepository parkingRepository,
                            NetexIdMapper netexIdMapper,
                            ParkingVersionedSaverService parkingVersionedSaverService,
                            ParkingPlaceEquipmentsRepository parkingPlaceEquipmentsRepository,
                            MdmService mdmService,
                            OrganisationsImportedService organisationsImportedService) {
        this.parkingRepository = parkingRepository;
        this.netexIdMapper = netexIdMapper;
        this.parkingVersionedSaverService = parkingVersionedSaverService;
        this.organisationsImportedService = organisationsImportedService;
        this.parkingPlaceEquipmentsRepository = parkingPlaceEquipmentsRepository;
        this.mdmService = mdmService;
    }

    public void createOrUpdateParkings(List<Parking> parkingsToSave) {

        for (Parking inputParking : parkingsToSave) {
            boolean found = false;

            Parking databaseParking = retrieveDatabaseParking(inputParking);

            if (databaseParking != null && databaseParking.getNetexId() != null) {
                found = true;
                boolean isParkingUpdated = populateParking(inputParking, databaseParking);

                if (isParkingUpdated) {
                    parkingVersionedSaverService.saveNewVersion(databaseParking);
                } else {
                    log.warn("Skip parking {} update - no changes", databaseParking.getNetexId());
                }
            }

            if (!found) {
                netexIdMapper.moveOriginalIdToKeyValueList(inputParking, inputParking.getOriginalId());
                netexIdMapper.moveOriginalNameToKeyValueList(inputParking, inputParking.getName().getValue());

                if (inputParking.getOrganisation() != null) {
                    organisationsImportedService.createOrUpdateOrganisation(inputParking.getOrganisation());
                }

                inputParking.setName(new EmbeddableMultilingualString(inputParking.getName().getValue()));
                parkingVersionedSaverService.saveNewVersion(inputParking);
            }
        }
    }

    private Parking retrieveDatabaseParking(Parking parking) {
        String importedId = CollectionUtils.isNotEmpty(parking.getOriginalIds()) ?
                parking.getOriginalIds().iterator().next() : parking.getOriginalId();
        Optional<ParkingIdentifier> existingMdmId =
                mdmService.getExistingParkingMdmIdsFromImportedId(parking.getOperator(), importedId);
        if (existingMdmId.isPresent()){
            Parking foundParking =
                    parkingRepository.findFirstByNetexIdOrderByVersionDesc(existingMdmId.get().getSuperId());
            if (foundParking != null && areAtTheSamePlace(foundParking, parking)){
                return foundParking;
            }
        }
        return null;
    }

    /**
     * Check if the 2 parkings are located at the same place (coordinates are rounded with 4 digits)
     *
     * @param p1 first parking
     * @param p2 second parking
     * @return true : parking are at the same place
     * false : parking are not at the same place
     */
    private boolean areAtTheSamePlace(Parking p1, Parking p2) {
        return roundFourDigits(p1.getCentroid().getX()) == roundFourDigits(p2.getCentroid().getX()) &&
                roundFourDigits(p1.getCentroid().getY()) == roundFourDigits(p2.getCentroid().getY());
    }

    private double roundFourDigits(double inputValue) {
        return Math.round(inputValue * 10000.0) / 10000.0;
    }

    private boolean populateParking(Parking inputParking, Parking databaseParking) {
        boolean isUpdated = false;
        if (inputParking.getName() != null &&
                !StringUtils.equals(inputParking.getName().getValue(), databaseParking.getName().getValue())) {
            databaseParking.setName(inputParking.getName());
            isUpdated = true;
        }

        if (inputParking.getValidBetween() != null &&
                !Objects.equals(inputParking.getValidBetween().getToDate(), databaseParking.getValidBetween().getToDate()) &&
                !Objects.equals(inputParking.getValidBetween().getFromDate(), databaseParking.getValidBetween().getFromDate())) {
            databaseParking.setValidBetween(inputParking.getValidBetween());
            isUpdated = true;
        }

        if (inputParking.getCentroid() != null &&
                inputParking.getCentroid().getX() != databaseParking.getCentroid().getX() &&
                inputParking.getCentroid().getY() != databaseParking.getCentroid().getY()) {
            databaseParking.setCentroid(inputParking.getCentroid());
            isUpdated = true;
        }

        if (inputParking.getParentSiteRef() != null &&
                !StringUtils.equals(inputParking.getParentSiteRef().getRef(), databaseParking.getParentSiteRef().getRef())) {
            databaseParking.setParentSiteRef(inputParking.getParentSiteRef());
            isUpdated = true;
        }

        if (inputParking.getTotalCapacity() != null &&
                !inputParking.getTotalCapacity().equals(databaseParking.getTotalCapacity())) {
            databaseParking.setTotalCapacity(inputParking.getTotalCapacity());
            isUpdated = true;
        }

        if (inputParking.getPrincipalCapacity() != null &&
                !inputParking.getPrincipalCapacity().equals(databaseParking.getPrincipalCapacity())) {
            databaseParking.setPrincipalCapacity(inputParking.getPrincipalCapacity());
            isUpdated = true;
        }

        if (inputParking.getParkingType() != null &&
                inputParking.getParkingType() != databaseParking.getParkingType()) {
            databaseParking.setParkingType(inputParking.getParkingType());
        }
        if (CollectionUtils.isNotEmpty(inputParking.getParkingVehicleTypes())
                && CollectionUtils.isNotEmpty(databaseParking.getParkingVehicleTypes())
                && !CollectionUtils.isEqualCollection(inputParking.getParkingVehicleTypes(), databaseParking.getParkingVehicleTypes())) {
            List<ParkingVehicleEnumeration> vehicleTypes = inputParking.getParkingVehicleTypes();

            databaseParking.getParkingVehicleTypes().clear();
            databaseParking.getParkingVehicleTypes().addAll(vehicleTypes);

            isUpdated = true;
        }

        if (inputParking.getParkingLayout() != null &&
                inputParking.getParkingLayout() != databaseParking.getParkingLayout()) {
            databaseParking.setParkingLayout(inputParking.getParkingLayout());
            isUpdated = true;
        }

        if (!Objects.equals(inputParking.isOvernightParkingPermitted(), databaseParking.isOvernightParkingPermitted())) {
            databaseParking.setOvernightParkingPermitted(inputParking.isOvernightParkingPermitted());
            isUpdated = true;
        }

        if (!Objects.equals(inputParking.isRechargingAvailable(), databaseParking.isRechargingAvailable())) {
            databaseParking.setRechargingAvailable(inputParking.isRechargingAvailable());
            isUpdated = true;
        }

        if (!Objects.equals(inputParking.isCarpoolingAvailable(), databaseParking.isCarpoolingAvailable())) {
            databaseParking.setCarpoolingAvailable(inputParking.isCarpoolingAvailable());
            isUpdated = true;
        }

        if (!Objects.equals(inputParking.isCarsharingAvailable(), databaseParking.isCarsharingAvailable())) {
            databaseParking.setCarsharingAvailable(inputParking.isCarsharingAvailable());
            isUpdated = true;
        }

        if (!Objects.equals(inputParking.isSecure(), databaseParking.isSecure())) {
            databaseParking.setSecure(inputParking.isSecure());
            isUpdated = true;
        }

        if (!Objects.equals(inputParking.isFreeParkingOutOfHours(), databaseParking.isFreeParkingOutOfHours())) {
            databaseParking.setFreeParkingOutOfHours(inputParking.isFreeParkingOutOfHours());
        }

        if (!Objects.equals(inputParking.isRealTimeOccupancyAvailable(), databaseParking.isRealTimeOccupancyAvailable())) {
            databaseParking.setRealTimeOccupancyAvailable(inputParking.isRealTimeOccupancyAvailable());
            isUpdated = true;
        }

        if (!Objects.equals(inputParking.isFreeParkingOutOfHours(), databaseParking.isFreeParkingOutOfHours())) {
            databaseParking.setFreeParkingOutOfHours(inputParking.isFreeParkingOutOfHours());
            isUpdated = true;
        }

        if (CollectionUtils.isNotEmpty(inputParking.getParkingPaymentProcess())
                && CollectionUtils.isNotEmpty(databaseParking.getParkingPaymentProcess())
                && !CollectionUtils.isEqualCollection(inputParking.getParkingPaymentProcess(), databaseParking.getParkingPaymentProcess())) {
            List<ParkingPaymentProcessEnumeration> parkingPaymentProcessTypes = inputParking.getParkingPaymentProcess();
            databaseParking.getParkingPaymentProcess().clear();
            databaseParking.getParkingPaymentProcess().addAll(parkingPaymentProcessTypes);
            isUpdated = true;
        }

        if (inputParking.getParkingReservation() != null &&
                inputParking.getParkingReservation() != databaseParking.getParkingReservation()) {
            databaseParking.setParkingReservation(inputParking.getParkingReservation());
            isUpdated = true;
        }

        if (inputParking.getBookingUrl() != null &&
                !StringUtils.equals(inputParking.getBookingUrl(), databaseParking.getBookingUrl())) {
            databaseParking.setBookingUrl(inputParking.getBookingUrl());
            isUpdated = true;
        }

        if (isAnyParkingAreaChangeFromCsv(inputParking.getParkingAreas(), databaseParking.getParkingAreas())) {
            databaseParking.getParkingAreas().clear();
            databaseParking.getParkingAreas().addAll(inputParking.getParkingAreas());
            isUpdated = true;
        }

        if (inputParking.getAccessibilityAssessment() != null &&
               !inputParking.getAccessibilityAssessment().equals(databaseParking.getAccessibilityAssessment())) {

            List<AccessibilityLimitation> limitations = inputParking.getAccessibilityAssessment().getLimitations();

            AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
            accessibilityAssessment.setMobilityImpairedAccess(inputParking.getAccessibilityAssessment().getMobilityImpairedAccess());

            AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();

            Optional<AccessibilityLimitation> limitationFromCsv = limitations.stream().findFirst();
            if (limitationFromCsv.isPresent()) {
                accessibilityLimitation.setWheelchairAccess(limitationFromCsv.get().getWheelchairAccess());
                accessibilityLimitation.setAudibleSignalsAvailable(limitationFromCsv.get().getAudibleSignalsAvailable());
                accessibilityLimitation.setEscalatorFreeAccess(limitationFromCsv.get().getEscalatorFreeAccess());
                accessibilityLimitation.setLiftFreeAccess(limitationFromCsv.get().getLiftFreeAccess());
                accessibilityLimitation.setStepFreeAccess(limitationFromCsv.get().getStepFreeAccess());
                accessibilityLimitation.setVisualSignsAvailable(limitationFromCsv.get().getVisualSignsAvailable());
            }

            accessibilityAssessment.setLimitations(List.of(accessibilityLimitation));

            databaseParking.setAccessibilityAssessment(accessibilityAssessment);
            isUpdated = true;
        }

        if (inputParking.getOperator() != null &&
                !StringUtils.equals(inputParking.getOperator(), databaseParking.getOperator())) {
            databaseParking.setOperator(inputParking.getOperator());
            isUpdated = true;
        }

        if (inputParking.getDescription() != null &&
                !StringUtils.equals(inputParking.getDescription().getValue(), databaseParking.getDescription().getValue())) {
            databaseParking.setDescription(inputParking.getDescription());
            isUpdated = true;
        }

        if (inputParking.getParkingLayout() != null &&
                inputParking.getParkingLayout() != databaseParking.getParkingLayout()) {
            databaseParking.setParkingLayout(inputParking.getParkingLayout());
            isUpdated = true;
        }

        if (inputParking.getAddress() != null &&
                !StringUtils.equals(inputParking.getAddress(), databaseParking.getAddress())) {
            databaseParking.setAddress(inputParking.getAddress());
            isUpdated = true;
        }

        if (anyParkingPropertyChangeFromCsv(inputParking.getParkingProperties(), databaseParking.getParkingProperties())) {
            databaseParking.getParkingProperties().clear();
            databaseParking.getParkingProperties().addAll(inputParking.getParkingProperties());
            isUpdated = true;
            int totalCapacity = inputParking.getParkingProperties().stream()
                    .map(ParkingProperties::getSpaces)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(space -> space.getNumberOfSpaces() != null)
                    .mapToInt(space -> space.getNumberOfSpaces().intValue())
                    .sum();
            databaseParking.setTotalCapacity(BigInteger.valueOf(totalCapacity));
        }

        if (inputParking.getOrganisation() != null &&
                !inputParking.getOrganisation().equals(databaseParking.getOrganisation())) {
            Organisation organisation = organisationsImportedService.createOrUpdateOrganisation(inputParking.getOrganisation());
            databaseParking.setOrganisation(organisation);
            isUpdated = true;
        }

        if (inputParking.getPlaceEquipments() != null) {
            if (databaseParking.getPlaceEquipments() != null) {
                parkingPlaceEquipmentsRepository.delete(databaseParking.getPlaceEquipments());
            }
            databaseParking.setPlaceEquipments(inputParking.getPlaceEquipments());
            isUpdated = true;
        }

        return isUpdated;
    }

    private boolean anyParkingPropertyChangeFromCsv(List<ParkingProperties> input, List<ParkingProperties> database) {
        if (CollectionUtils.isEmpty(input) && CollectionUtils.isEmpty(database)) {
            return false;
        }
        if (input.size() != database.size()) {
            return true;
        }

        input.sort(Comparator.comparing(IdentifiedEntity::getNetexId));
        database.sort(Comparator.comparing(IdentifiedEntity::getNetexId));
        for (int i = 0; i < input.size(); i++) {
            ParkingProperties parkingProperty = input.get(i);
            ParkingProperties databaseParkingProperty = database.get(i);
            if (!CollectionUtils.isEqualCollection(parkingProperty.getParkingUserTypes(), databaseParkingProperty.getParkingUserTypes())) {
                return true;
            }
            if (!CollectionUtils.isEqualCollection(parkingProperty.getParkingVehicleTypes(), databaseParkingProperty.getParkingVehicleTypes())) {
                return true;
            }
            if (isAnyParkingCapacityChangeFromCsv(parkingProperty.getSpaces(), databaseParkingProperty.getSpaces())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAnyParkingAreaChangeFromCsv(List<ParkingArea> input, List<ParkingArea> database) {
        if (CollectionUtils.isEmpty(input) && CollectionUtils.isEmpty(database)) {
            return false;
        }
        if (input.size() != database.size()) {
            return true;
        }

        for (int i = 0; i < input.size(); i++) {
            ParkingArea parkingArea = input.get(i);
            ParkingArea databaseParkingArea = database.get(i);
            if (!parkingArea.getMaximumHeight().equals(databaseParkingArea.getMaximumHeight())) {
                return true;
            }
            if (parkingArea.getSpecificParkingAreaUsage() != databaseParkingArea.getSpecificParkingAreaUsage()) {
                return true;
            }
            if (parkingArea.getName() != null && databaseParkingArea.getName() != null &&
                    !parkingArea.getName().getValue().equals(databaseParkingArea.getName().getValue())) {
                return true;
            }
            if (!parkingArea.getTotalCapacity().equals(databaseParkingArea.getTotalCapacity())) {
                return true;
            }
            if (parkingArea.getPublicUse() != databaseParkingArea.getPublicUse()) {
                return true;
            }
            if (isAnyParkingPropertyFromCsv(parkingArea.getParkingProperties(), databaseParkingArea.getParkingProperties())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAnyParkingPropertyFromCsv(ParkingProperties inputParkingProperties, ParkingProperties databaseParkingProperties) {
        if (inputParkingProperties == databaseParkingProperties) {
            return false;
        }
        if (databaseParkingProperties == null) {
            return true;
        }
        return isAnyParkingCapacityChangeFromCsv(inputParkingProperties.getSpaces(), databaseParkingProperties.getSpaces());
    }

    private boolean isAnyParkingCapacityChangeFromCsv(List<ParkingCapacity> input, List<ParkingCapacity> database) {
        if (CollectionUtils.isEmpty(input) && CollectionUtils.isEmpty(database)) {
            return false;
        }
        if (input.size() != database.size()) {
            return true;
        }
        ParkingCapacity inputCapacity = input.getFirst();
        ParkingCapacity databaseCapacity = database.getFirst();
        return inputCapacity.getParkingUserType() != databaseCapacity.getParkingUserType()
                || inputCapacity.getParkingVehicleType() != databaseCapacity.getParkingVehicleType()
                || (inputCapacity.getNumberOfSpaces() != null && databaseCapacity.getNumberOfSpaces() != null
                    && !inputCapacity.getNumberOfSpaces().equals(databaseCapacity.getNumberOfSpaces()));
    }

}
