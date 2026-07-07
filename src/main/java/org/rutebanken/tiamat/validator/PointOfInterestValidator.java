package org.rutebanken.tiamat.validator;

import org.jetbrains.annotations.NotNull;
import org.rutebanken.netex.model.PointOfInterest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static org.rutebanken.tiamat.config.Messages.VALIDATION_INVALID_ID_FORMAT;
import static org.rutebanken.tiamat.config.Messages.VALIDATION_POI_CENTROID_REQUIRED;
import static org.rutebanken.tiamat.netex.id.NetexIdHelper.GENERIC_NETEX_PATTERN;
import static org.rutebanken.tiamat.netex.id.NetexIdHelper.isNetexIdOfType;
import static org.springframework.validation.ValidationUtils.rejectIfEmpty;

@Component
public class PointOfInterestValidator implements Validator {

    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return PointOfInterest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        PointOfInterest poi = (PointOfInterest) target;
        if (!isNetexIdOfType(poi.getId(), "PointOfInterest")) {
            errors.rejectValue("id", VALIDATION_INVALID_ID_FORMAT, new Object[]{poi.getId(), String.format(GENERIC_NETEX_PATTERN, "PointOfInterest")}, null);
        }
        Object[] poiIdArgs = {poi.getId()};
        rejectIfEmpty(errors, "centroid.location.longitude", VALIDATION_POI_CENTROID_REQUIRED, poiIdArgs, null);
        rejectIfEmpty(errors, "centroid.location.latitude", VALIDATION_POI_CENTROID_REQUIRED, poiIdArgs, null);
    }
}
