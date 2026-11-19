package org.rutebanken.tiamat.general;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;
import org.rutebanken.tiamat.model.job.JobErrorSeverity;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ImportErrorLimitTest {

    @Test
    void keepsErrorsUpToTheLimit() {
        List<JobError> errors = errors(ImportErrorLimit.MAX_ERRORS);

        assertThat(ImportErrorLimit.cap(errors)).isSameAs(errors);
    }

    @Test
    void keepsFirstErrorsAndAddsAlertBeyondTheLimit() {
        List<JobError> errors = errors(ImportErrorLimit.MAX_ERRORS + 50);

        List<JobError> capped = ImportErrorLimit.cap(errors);

        assertThat(capped).hasSize(ImportErrorLimit.MAX_ERRORS + 1);
        assertThat(capped.subList(0, ImportErrorLimit.MAX_ERRORS)).isEqualTo(errors.subList(0, ImportErrorLimit.MAX_ERRORS));
        assertThat(capped.getLast()).isEqualTo(JobError.error(AnalyzeImportErrorType.TOO_MANY_ERRORS).withParam("maxErrors", "200"));
    }

    @Test
    void alertIsAWarningWhenOnlyWarningsAreReported() {
        List<JobError> warnings = IntStream.range(0, ImportErrorLimit.MAX_ERRORS + 1)
                .mapToObj(i -> JobError.warning(AnalyzeImportErrorType.FIELD_TRUNCATED).withParam("entityId", String.valueOf(i)))
                .toList();

        assertThat(ImportErrorLimit.cap(warnings).getLast().getSeverity()).isEqualTo(JobErrorSeverity.WARNING);
    }

    @Test
    void acceptsNoErrors() {
        assertThat(ImportErrorLimit.cap(null)).isNull();
    }

    private static List<JobError> errors(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> JobError.error(AnalyzeImportErrorType.MISSING_DATA).withParam("line", String.valueOf(i)))
                .toList();
    }
}
