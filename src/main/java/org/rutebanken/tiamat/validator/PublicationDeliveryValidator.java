package org.rutebanken.tiamat.validator;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.netex.NetexConstants;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.*;

import static org.rutebanken.tiamat.config.Messages.*;
import static org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace;

@Component
public class PublicationDeliveryValidator implements Validator {

    public static final int MAXIMUM_PARKING_DEPTH = 3;

    private final ParkingValidator parkingValidator;
    private final PointOfInterestValidator pointOfInterestValidator;

    public PublicationDeliveryValidator(ParkingValidator parkingValidator, PointOfInterestValidator pointOfInterestValidator) {
        this.parkingValidator = parkingValidator;
        this.pointOfInterestValidator = pointOfInterestValidator;
    }

    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return PublicationDeliveryStructure.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        var publicationDelivery = (PublicationDeliveryStructure) target;
        errors.pushNestedPath("dataObjects");
        try {
            var frames = publicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();
            for (int i = 0; i < frames.size(); i++) {
                validateFrame(frames.get(i), i, errors);
            }
        } finally {
            errors.popNestedPath(); // dataObjects
        }
    }

    private void validateFrame(JAXBElement<? extends Common_VersionFrameStructure> frame, int index, @NotNull Errors errors) {
        errors.pushNestedPath(String.format("compositeFrameOrCommonFrame[%d].value", index));
        try {
            if (frame.getValue() instanceof GeneralFrame generalFrame) {
                validateGeneralFrame(generalFrame, errors);
            } else if (frame.getValue() instanceof SiteFrame siteFrame) {
                validatePointsOfInterest(siteFrame, errors);
            }
        } finally {
            errors.popNestedPath(); // compositeFrameOrCommonFrame[i].value
        }
    }

    private void validateGeneralFrame(@NotNull GeneralFrame generalFrame, @NotNull Errors errors) {
        if (generalFrame.getMembers() == null || CollectionUtils.isEmpty(generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity())) {
            return;
        }
        errors.pushNestedPath("members");
        try {
            var members = generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();
            if (members.stream().noneMatch(e -> e.getValue() instanceof Parking)) {
                return;
            }
            if (members.stream().noneMatch(e -> e.getValue() instanceof TypeOfFrame)) {
                errors.rejectValue("", VALIDATION_TOF_REQUIRED, new Object[]{generalFrame.getId()}, null);
            }
            validateGeneralFrameMembers(members, errors);
        } finally {
            errors.popNestedPath(); // members
        }
    }

    private void validateGeneralFrameMembers(List<JAXBElement<? extends EntityStructure>> members, @NotNull Errors errors) {
        Set<String> parkingTopLevelIds = new HashSet<>();
        Map<String, List<String>> parentToChildrenParking = new HashMap<>();
        for (int j = 0; j < members.size(); j++) {
            var entity = members.get(j);
            errors.pushNestedPath(String.format("generalFrameMemberOrDataManagedObjectOrEntity_Entity[%d].value", j));
            try {
                if (entity.getValue() instanceof Parking parking) {
                    if (parking.getParentZoneRef() != null) {
                        parentToChildrenParking.computeIfAbsent(parking.getParentZoneRef().getRef(), k -> new ArrayList<>()).add(parking.getId());
                    } else {
                        // parking has no ParentZoneRef, it may be top level of a multilevel parking
                        parkingTopLevelIds.add(parking.getId());
                    }
                    parkingValidator.validate(parking, errors);
                } else if (entity.getValue() instanceof GeneralOrganisation || entity.getValue() instanceof Organisation) {
                    rejectIfEmptyOrWhitespace(errors, "companyNumber", VALIDATION_COMPANY_NUMBER_REQUIRED,
                            new Object[]{entity.getValue().getId()});
                } else if (entity.getValue() instanceof SiteComponent_VersionStructure siteComponent) {
                    rejectIfEmptyOrWhitespace(errors, "siteRef.ref", VALIDATION_SITE_REF_REQUIRED,
                            new Object[]{siteComponent.getSiteRef()});
                } else if (entity.getValue() instanceof TypeOfFrame tof) {
                    validateTypeOfFrame(tof, errors);
                }
            } finally {
                errors.popNestedPath();
            }
        }
        for (String parkingTopLevelId : parkingTopLevelIds) {
            if (getParkingDepth(parkingTopLevelId, parentToChildrenParking, 1) > MAXIMUM_PARKING_DEPTH) {
                errors.rejectValue("", VALIDATION_MAXIMUM_PARKING_DEPTH_EXCEEDED, new Object[]{parkingTopLevelId, MAXIMUM_PARKING_DEPTH}, null);
            }
        }
    }

    private void validatePointsOfInterest(@NotNull SiteFrame siteFrame, @NotNull Errors errors) {
        if (siteFrame.getPointsOfInterest() == null || CollectionUtils.isEmpty(siteFrame.getPointsOfInterest().getPointOfInterest())) {
            return;
        }
        errors.pushNestedPath("pointsOfInterest");
        try {
            var pointsOfInterest = siteFrame.getPointsOfInterest().getPointOfInterest();
            for (int i = 0; i < pointsOfInterest.size(); i++) {
                errors.pushNestedPath(String.format("pointOfInterest[%d]", i));
                try {
                    pointOfInterestValidator.validate(pointsOfInterest.get(i), errors);
                } finally {
                    errors.popNestedPath();
                }
            }
        } finally {
            errors.popNestedPath();
        }
    }

    /**
     * Recursive function to retrieve parking depth
     *
     * @param parkingId               id of parking
     * @param parentToChildrenParking parent to children parking relationship (one parking may have 0 to N children)
     * @param depth                   depth of current parking
     * @return current parking depth
     */
    private int getParkingDepth(String parkingId, Map<String, List<String>> parentToChildrenParking, int depth) {
        if (CollectionUtils.isEmpty(parentToChildrenParking.get(parkingId))) {
            // lower level parking reached
            return depth;
        }
        for (var childParkingId : parentToChildrenParking.get(parkingId)) {
            depth = Math.max(depth, getParkingDepth(childParkingId, parentToChildrenParking, depth + 1));
        }
        return depth;
    }

    private void validateTypeOfFrame(@NotNull TypeOfFrame tof, @NotNull Errors errors) {
        if (!StringUtils.equals(NetexConstants.NETEX_PARKING_TOF_ID, tof.getId())) {
            errors.rejectValue("id", VALIDATION_TOF_ID_INVALID, new Object[]{NetexConstants.NETEX_PARKING_TOF_ID, tof.getId()}, null);
        }
        if (!StringUtils.equals(NetexConstants.NETEX_PARKING_TOF_VERSION, tof.getVersion())) {
            errors.rejectValue("version", VALIDATION_TOF_VERSION_INVALID, new Object[]{NetexConstants.NETEX_PARKING_TOF_VERSION, tof.getId()}, null);
        }
        if (tof.getName() == null || !StringUtils.equals(NetexConstants.NETEX_PARKING_TOF_NAME, tof.getName().getValue())) {
            errors.rejectValue("name.value", VALIDATION_TOF_NAME_INVALID, new Object[]{NetexConstants.NETEX_PARKING_TOF_NAME, tof.getId()}, null);
        }
        if (tof.getDescription() == null || !StringUtils.equals(NetexConstants.NETEX_PARKING_TOF_DESCRIPTION, tof.getDescription().getValue())) {
            errors.rejectValue("description.value", VALIDATION_TOF_DESCRIPTION_INVALID, new Object[]{NetexConstants.NETEX_PARKING_TOF_DESCRIPTION, tof.getId()}, null);
        }
    }

}
