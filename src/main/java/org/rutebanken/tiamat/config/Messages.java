package org.rutebanken.tiamat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Util class to retrieve localized messages.
 */
@Component
public class Messages {

    public static final String VALIDATION_FROM_DATE_AFTER_TO_DATE = "validation.from-date-after-to-date";
    public static final String VALIDATION_FROM_DATE_NOT_SET = "validation.from-date-not-set";
    public static final String VALIDATION_TO_DATE_AFTER_NEXT_VERSION_FROM_DATE = "validation.existing-version-to-date-after-new-version-from-date";
    public static final String VALIDATION_FROM_DATE_AFTER_NEXT_VERSION_FROM_DATE = "validation.existing-version-from-date-after-new-version-from-date";
    public static final String VALIDATION_CANNOT_TERMINATE_FOR_NULL = "validation.cannot-terminate-for-null";
    public static final String ERRORS_QUAY_DOES_NOT_EXIST_ON_STOP_PLACE = "errors.quay-does-not-exist-on-stop-place";

    // parkings
    public static final String VALIDATION_INVALID_ID_FORMAT = "validation.invalid-id-format";
    public static final String VALIDATION_PARKING_NAME_REQUIRED = "validation.parking-name-required";
    public static final String VALIDATION_PARKING_CENTROID_REQUIRED = "validation.parking-centroid-required";
    public static final String VALIDATION_PARKING_TYPE_REQUIRED = "validation.parking-type-required";
    public static final String VALIDATION_TOP_REQUIRED = "validation.type-of-parking-ref-required";
    public static final String VALIDATION_PARKING_VEHICLE_TYPES_REQUIRED = "validation.parking-vehicle-types-required";
    public static final String VALIDATION_VEHICLE_TYPES_REQUIRED = "validation.vehicle-types-required";
    public static final String VALIDATION_PARKING_LAYOUT_REQUIRED = "validation.parking-layout-required";
    public static final String VALIDATION_PARKING_TOTAL_CAPACITY_REQUIRED = "validation.total-capacity-required";
    public static final String VALIDATION_PARKING_PAYMENT_PROCESS_REQUIRED = "validation.parking-payment-process-required";
    public static final String VALIDATION_MAXIMUM_PARKING_PROPERTIES_EXCEEDED = "validation.maximum-parking-properties-exceeded";
    public static final String VALIDATION_MUST_NOT_CONTAIN_REFS = "validation.must-not-contain-refs";
    public static final String VALIDATION_MAXIMUM_HEIGHT_REQUIRED = "validation.maximum-height-required";
    public static final String VALIDATION_TRANSPORT_TYPE_REF_REQUIRED = "validation.transport-type-ref-required";
    public static final String VALIDATION_MAXIMUM_PARKING_DEPTH_EXCEEDED = "validation.maximum-parking-depth-exceeded";
    public static final String VALIDATION_POSTAL_ADDRESS_POSTAL_REGION_REQUIRED = "validation.postal-address-postal-region-required";
    public static final String VALIDATION_COMPANY_NUMBER_REQUIRED = "validation.company-number-required";
    public static final String VALIDATION_SITE_REF_REQUIRED = "validation.site-ref-required";
    public static final String VALIDATION_TOF_REQUIRED = "validation.type-of-frame-required";
    public static final String VALIDATION_TOF_ID_INVALID = "validation.type-of-frame-id-invalid";
    public static final String VALIDATION_TOF_VERSION_INVALID = "validation.type-of-frame-version-invalid";
    public static final String VALIDATION_TOF_NAME_INVALID = "validation.type-of-frame-name-invalid";
    public static final String VALIDATION_TOF_DESCRIPTION_INVALID = "validation.type-of-frame-description-invalid";
    public static final String VALIDATION_IMPORTED_ID_REQUIRED = "validation.imported-id-required";

    public static final String VALIDATION_POI_CENTROID_REQUIRED = "validation.poi-centroid-required";

    private static final Logger log = LoggerFactory.getLogger(Messages.class);

    private final MessageSourceAccessor accessor;

    /**
     * @param messageSource message source
     */
    public Messages(MessageSource messageSource) {
        Locale defaultLocale = LocaleContextHolder.getLocale();
        log.info("Setting default locale for Messages component : {}", defaultLocale);
        this.accessor = new MessageSourceAccessor(messageSource, defaultLocale);
    }

    /**
     * Get a localized message by its key.
     *
     * @param messageKey       message messageKey.
     * @param messageArguments optional arguments needed to build message
     * @return localized message
     */
    public String get(String messageKey, Object... messageArguments) {
        return accessor.getMessage(messageKey, messageArguments);
    }


}
