package org.rutebanken.tiamat.config;

import org.rutebanken.tiamat.netex.id.ValidPrefixList;
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
    public static final String ERROR_QUAY_DOES_NOT_EXIST_ON_STOP_PLACE = "errors.quay-does-not-exist-on-stop-place";

    private static final Logger logger = LoggerFactory.getLogger(ValidPrefixList.class);

    MessageSourceAccessor accessor;


    /**
     * @param messageSource
     */
    public Messages(MessageSource messageSource) {
        Locale defaultLocale = LocaleContextHolder.getLocale();
        logger.info("Setting default locale for Messages component : " + defaultLocale);
        this.accessor = new MessageSourceAccessor(messageSource, defaultLocale);
    }


    /**
     * Get a localized message by its key.
     *
     * @param messageKey       message messageKey.
     * @param messageArguments optional arguments needed to build message
     * @return
     */
    public String get(String messageKey, Object... messageArguments) {
        return accessor.getMessage(messageKey, messageArguments);
    }


}
