package org.rutebanken.tiamat.validator;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.rutebanken.tiamat.config.Messages.*;

class NetexValidationErrorClassifierTest {

    @Test
    void classifiesStructuralCodesAsTemplate() {
        BindException bindException = bindExceptionWithCodes(VALIDATION_TOF_ID_INVALID, VALIDATION_MAXIMUM_PARKING_DEPTH_EXCEEDED);

        assertThat(NetexValidationErrorClassifier.classify(bindException)).containsExactly(AnalyzeImportErrorType.TEMPLATE);
    }

    @Test
    void classifiesFieldCodesAsMissingData() {
        BindException bindException = bindExceptionWithCodes(VALIDATION_PARKING_NAME_REQUIRED, VALIDATION_PARKING_TOTAL_CAPACITY_REQUIRED);

        assertThat(NetexValidationErrorClassifier.classify(bindException)).containsExactly(AnalyzeImportErrorType.MISSING_DATA);
    }

    @Test
    void classifiesUnknownCodeAsMissingDataByDefault() {
        BindException bindException = bindExceptionWithCodes("some.unmapped.future.validation.code");

        assertThat(NetexValidationErrorClassifier.classify(bindException)).containsExactly(AnalyzeImportErrorType.MISSING_DATA);
    }

    @Test
    void classifiesMixedCodesAsBoth() {
        BindException bindException = bindExceptionWithCodes(VALIDATION_TOF_ID_INVALID, VALIDATION_PARKING_NAME_REQUIRED);

        assertThat(NetexValidationErrorClassifier.classify(bindException))
                .containsExactlyInAnyOrder(AnalyzeImportErrorType.TEMPLATE, AnalyzeImportErrorType.MISSING_DATA);
    }

    private BindException bindExceptionWithCodes(String... codes) {
        Object target = new Object();
        Errors errors = new BeanPropertyBindingResult(target, "target");
        for (String code : codes) {
            errors.reject(code, new Object[]{"FR:75056:Parking:1:LOC"}, code);
        }
        return new BindException((BeanPropertyBindingResult) errors);
    }
}
