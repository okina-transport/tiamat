package org.rutebanken.tiamat.validator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.rutebanken.netex.model.*;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static org.rutebanken.tiamat.config.Messages.*;
import static org.rutebanken.tiamat.netex.id.NetexIdHelper.*;
import static org.springframework.validation.ValidationUtils.rejectIfEmpty;
import static org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace;


@Component
public class ParkingValidator implements Validator {

    public static final int MAXIMUM_PARKING_PROPERTIES = 1;

    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return Parking.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        Parking p = (Parking) target;
        if (!isParkingNetexId(p.getId())) {
            errors.rejectValue("id", VALIDATION_INVALID_ID_FORMAT, new Object[]{p.getId(), PARKING_ID_RE}, null);
        }
        Object[] pIdArgs = {p.getId()};
        rejectIfEmptyOrWhitespace(errors, "name.value", VALIDATION_PARKING_NAME_REQUIRED, pIdArgs, null);
        rejectIfEmpty(errors, "centroid.location.longitude", VALIDATION_PARKING_CENTROID_REQUIRED, pIdArgs, null);
        rejectIfEmpty(errors, "centroid.location.latitude", VALIDATION_PARKING_CENTROID_REQUIRED, pIdArgs, null);
        rejectIfEmpty(errors, "parkingType", VALIDATION_PARKING_TYPE_REQUIRED, pIdArgs, null);
        if (p.getParkingType() == ParkingTypeEnumeration.OTHER) {
            // when <ParkingType> is 'other' there must be a <TypeOfParkingRef>
            rejectIfEmptyOrWhitespace(errors, "typeOfParkingRef.ref",
                    VALIDATION_TOP_REQUIRED, pIdArgs, null);
        }
        if (CollectionUtils.isEmpty(p.getParkingVehicleTypes())) {
            errors.rejectValue("parkingVehicleTypes", VALIDATION_PARKING_VEHICLE_TYPES_REQUIRED, pIdArgs, null);
        } else if (p.getParkingVehicleTypes().contains(ParkingVehicleEnumeration.OTHER)) {
            if (p.getVehicleTypes() == null || CollectionUtils.isEmpty(p.getVehicleTypes().getTransportTypeRef())) {
                errors.rejectValue("vehicleTypes", VALIDATION_VEHICLE_TYPES_REQUIRED, pIdArgs, null);
            }
        }
        rejectIfEmpty(errors, "parkingLayout", VALIDATION_PARKING_LAYOUT_REQUIRED, pIdArgs, null);
        rejectIfEmpty(errors, "totalCapacity", VALIDATION_PARKING_TOTAL_CAPACITY_REQUIRED, pIdArgs, null);
        if (CollectionUtils.isEmpty(p.getParkingPaymentProcess())) {
            errors.rejectValue("parkingPaymentProcess", VALIDATION_PARKING_PAYMENT_PROCESS_REQUIRED, pIdArgs, null);
        }
        rejectIfEmptyOrWhitespace(errors, "postalAddress.postalRegion", VALIDATION_POSTAL_ADDRESS_POSTAL_REGION_REQUIRED, pIdArgs, null);
        validateParkingProperties(p, errors);
        validateParkingAreas(p, errors);
        validateVehicleEntrances(p, errors);
        validateEntrances(p, errors);
    }

    private void validateParkingAreas(@NotNull Parking p, @NotNull Errors errors) {
        if (p.getParkingAreas() == null || CollectionUtils.isEmpty(p.getParkingAreas().getParkingAreaRefOrParkingArea_())) {
            return;
        }
        errors.pushNestedPath("parkingAreas");
        for (int i = 0; i < p.getParkingAreas().getParkingAreaRefOrParkingArea_().size(); i++) {
            var parkingAreaOrRef = p.getParkingAreas().getParkingAreaRefOrParkingArea_().get(i);
            errors.pushNestedPath(String.format("parkingAreaRefOrParkingArea_[%d].value", i));
            if (parkingAreaOrRef.getValue() instanceof ParkingAreaRefStructure parkingAreaRef) {
                errors.rejectValue("", VALIDATION_MUST_NOT_CONTAIN_REFS, new Object[]{parkingAreaRef.getRef()}, null);
            } else if (parkingAreaOrRef.getValue() instanceof ParkingArea parkingArea) {
                validateParkingArea(parkingArea, errors);
            }
            errors.popNestedPath(); // parkingAreaRefOrParkingArea_[i].value
        }
        errors.popNestedPath(); // parkingAreas
    }

    private void validateParkingArea(@NotNull ParkingArea_VersionStructure parkingArea, @NotNull Errors errors) {
        if (!isNetexIdOfType(parkingArea.getId(), "ParkingArea")) {
            errors.rejectValue("id", VALIDATION_INVALID_ID_FORMAT, new Object[]{parkingArea.getId(), String.format(GENERIC_NETEX_PATTERN, "ParkingArea")}, null);
        }
        validateParkingComponent(parkingArea, errors);
        if (CollectionUtils.isEmpty(parkingArea.getRest())) {
            return;
        }
        for (int j = 0; j < parkingArea.getRest().size(); j++) {
            var rest = parkingArea.getRest().get(j);
            errors.pushNestedPath(String.format("rest[%d].value", j));
            if (rest.getValue() instanceof ParkingBayRefStructure ref) {
                errors.rejectValue("", VALIDATION_MUST_NOT_CONTAIN_REFS, new Object[]{ref.getRef()}, null);
            } else if (rest.getValue() instanceof ParkingBay parkingBay) {
                validateParkingComponent(parkingBay, errors);
            }
            errors.popNestedPath(); // rest[j].value
        }
    }

    private void validateSiteComponent(@NotNull SiteComponent_VersionStructure siteComponent, @NotNull Errors errors) {
        rejectIfEmptyOrWhitespace(errors, "siteRef.ref", VALIDATION_SITE_REF_REQUIRED, new Object[]{siteComponent.getId()});
    }

    private void validateParkingComponent(@NotNull ParkingComponent_VersionStructure parkingComponent, @NotNull Errors errors) {
        rejectIfEmpty(errors, "maximumHeight", VALIDATION_MAXIMUM_HEIGHT_REQUIRED, new Object[]{parkingComponent.getId()});
        validateSiteComponent(parkingComponent, errors);
    }

    private void validateParkingProperties(@NotNull Parking p, @NotNull Errors errors) {
        if (p.getParkingProperties() == null || CollectionUtils.isEmpty(p.getParkingProperties().getParkingProperties())) {
            return;
        }
        if (p.getParkingProperties().getParkingProperties().size() > MAXIMUM_PARKING_PROPERTIES) {
            errors.rejectValue("parkingProperties", VALIDATION_MAXIMUM_PARKING_PROPERTIES_EXCEEDED, new Object[]{p.getId(), MAXIMUM_PARKING_PROPERTIES}, null);
        }
        errors.pushNestedPath("parkingProperties");
        for (int i = 0; i < p.getParkingProperties().getParkingProperties().size(); i++) {
            var parkingProperties = p.getParkingProperties().getParkingProperties().get(i);
            errors.pushNestedPath(String.format("parkingProperties[%d]", i));
            validateParkingProperties(parkingProperties, errors);
            errors.popNestedPath();
        }
        errors.popNestedPath();
    }

    private void validateParkingProperties(@NotNull ParkingProperties_VersionedChildStructure parkingProperties, @NotNull Errors errors) {
        if (CollectionUtils.isEmpty(parkingProperties.getParkingVehicleTypes())) {
            errors.rejectValue("parkingVehicleTypes", VALIDATION_PARKING_VEHICLE_TYPES_REQUIRED, new Object[]{parkingProperties.getId()}, null);
        } else if (parkingProperties.getParkingVehicleTypes().contains(ParkingVehicleEnumeration.OTHER)) {
            rejectIfEmptyOrWhitespace(errors, "vehicleTypes", VALIDATION_VEHICLE_TYPES_REQUIRED, new Object[]{parkingProperties.getId()});
        }
        if (parkingProperties.getSpaces() == null || CollectionUtils.isEmpty(parkingProperties.getSpaces().getParkingCapacityRefOrParkingCapacity())) {
            return;
        }
        errors.pushNestedPath("spaces");
        var parkingCapacities = parkingProperties.getSpaces().getParkingCapacityRefOrParkingCapacity();
        for (int i = 0; i < parkingCapacities.size(); i++) {
            errors.pushNestedPath(String.format("parkingCapacityRefOrParkingCapacity[%d]", i));
            if (parkingCapacities.get(i) instanceof ParkingCapacityRefStructure ref) {
                errors.rejectValue("", VALIDATION_MUST_NOT_CONTAIN_REFS,
                        new Object[]{ref.getRef()}, null);
            } else if (parkingCapacities.get(i) instanceof ParkingCapacity parkingCapacity) {
                validateParkingCapacity(parkingCapacity, errors);
            }
            errors.popNestedPath();
        }
        errors.popNestedPath();
    }

    private void validateVehicleEntrances(Parking p, Errors errors) {
        if (p.getVehicleEntrances() == null || CollectionUtils.isEmpty(p.getVehicleEntrances().getParkingEntranceForVehiclesRefOrParkingEntranceForVehicles())) {
            return;
        }
        errors.pushNestedPath("vehicleEntrances");
        var parkingEntrances = p.getVehicleEntrances().getParkingEntranceForVehiclesRefOrParkingEntranceForVehicles();
        for (int i = 0; i < parkingEntrances.size(); i++) {
            errors.pushNestedPath(String.format("parkingEntranceForVehiclesRefOrParkingEntranceForVehicles[%d]", i));
            if (parkingEntrances.get(i) instanceof ParkingEntranceForVehiclesRefStructure ref) {
                errors.rejectValue("", VALIDATION_MUST_NOT_CONTAIN_REFS, new Object[]{ref.getRef()}, null);
            } else if (parkingEntrances.get(i) instanceof ParkingEntranceForVehicles parkingEntranceForVehicles) {
                validateParkingEntranceForVehicles(parkingEntranceForVehicles, errors);
            }
            errors.popNestedPath();
        }
        errors.popNestedPath();
    }

    private void validateEntrances(@NotNull Parking p, @NotNull Errors errors) {
        if (p.getEntrances() == null || CollectionUtils.isEmpty(p.getEntrances().getEntranceRefOrEntrance())) {
            return;
        }
        errors.pushNestedPath("entrances");
        var entrances = p.getEntrances().getEntranceRefOrEntrance();
        for (int i = 0; i < entrances.size(); i++) {
            errors.pushNestedPath(String.format("entranceRefOrEntrance[%d].value", i));
            if (entrances.get(i).getValue() instanceof ParkingPassengerEntranceRefStructure ref) {
                errors.rejectValue("", VALIDATION_MUST_NOT_CONTAIN_REFS, new Object[]{ref.getRef()}, null);
            } else if (entrances.get(i).getValue() instanceof ParkingPassengerEntrance_VersionStructure ppe) {
                validateParkingPassengerEntrance(ppe, errors);
            }
            errors.popNestedPath();
        }
        errors.popNestedPath();
    }

    private void validateParkingCapacity(@NotNull ParkingCapacity_VersionedChildStructure parkingCapacity, @NotNull Errors errors) {
        if (parkingCapacity.getParkingVehicleType() == ParkingVehicleEnumeration.OTHER) {
            if (parkingCapacity.getTransportTypeRef() == null || parkingCapacity.getTransportTypeRef().getValue() ==
                    null || StringUtils.isBlank(parkingCapacity.getTransportTypeRef().getValue().getRef()))
                errors.rejectValue("transportTypeRef", VALIDATION_TRANSPORT_TYPE_REF_REQUIRED,
                        new Object[]{parkingCapacity.getId()}, null);
        }
    }

    private void validateParkingEntranceForVehicles(@NotNull ParkingEntranceForVehicles__VersionStructure parkingEntranceForVehicle, @NotNull Errors errors) {
        rejectIfEmpty(errors, "centroid.location.longitude", VALIDATION_PARKING_CENTROID_REQUIRED, new Object[]{parkingEntranceForVehicle.getId()}, null);
        rejectIfEmpty(errors, "centroid.location.latitude", VALIDATION_PARKING_CENTROID_REQUIRED, new Object[]{parkingEntranceForVehicle.getId()}, null);
        validateSiteComponent(parkingEntranceForVehicle, errors);
    }

    private void validateParkingPassengerEntrance(@NotNull ParkingPassengerEntrance_VersionStructure parkingPassengerEntrance, @NotNull Errors errors) {
        rejectIfEmpty(errors, "centroid.location.longitude", VALIDATION_PARKING_CENTROID_REQUIRED, new Object[]{parkingPassengerEntrance.getId()}, null);
        rejectIfEmpty(errors, "centroid.location.latitude", VALIDATION_PARKING_CENTROID_REQUIRED, new Object[]{parkingPassengerEntrance.getId()}, null);
        validateSiteComponent(parkingPassengerEntrance, errors);
    }

}
