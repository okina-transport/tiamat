package org.rutebanken.tiamat.validator;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.springframework.validation.BindException;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.rutebanken.tiamat.config.Messages.*;

public class NetexValidationErrorClassifier {

    private static final Set<String> TEMPLATE_ERROR_CODES = Set.of(
            VALIDATION_INVALID_ID_FORMAT,
            VALIDATION_TOF_REQUIRED,
            VALIDATION_TOF_ID_INVALID,
            VALIDATION_TOF_VERSION_INVALID,
            VALIDATION_TOF_NAME_INVALID,
            VALIDATION_TOF_DESCRIPTION_INVALID,
            VALIDATION_MUST_NOT_CONTAIN_REFS,
            VALIDATION_MAXIMUM_PARKING_DEPTH_EXCEEDED,
            VALIDATION_MAXIMUM_PARKING_PROPERTIES_EXCEEDED
    );

    private NetexValidationErrorClassifier() {
    }

    public static Set<AnalyzeImportErrorType> classify(BindException bindException) {
        Set<AnalyzeImportErrorType> errorTypes = new LinkedHashSet<>();
        bindException.getAllErrors().forEach(error -> {
            if (TEMPLATE_ERROR_CODES.contains(error.getCode())) {
                errorTypes.add(AnalyzeImportErrorType.TEMPLATE);
            } else {
                errorTypes.add(AnalyzeImportErrorType.MISSING_DATA);
            }
        });
        return errorTypes;
    }
}
