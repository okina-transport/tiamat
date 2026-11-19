package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;
import org.rutebanken.tiamat.model.job.JobErrorSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Maximum number of errors reported for an imported file.
 * <p>
 * The controls stop as soon as this limit is exceeded, and only the first errors are reported to the user,
 * followed by a {@link AnalyzeImportErrorType#TOO_MANY_ERRORS} alert telling that the file may contain other problems.
 */
public final class ImportErrorLimit {

    public static final int MAX_ERRORS = 200;

    public static final String MAX_ERRORS_PARAM = "maxErrors";

    private ImportErrorLimit() {
    }

    /**
     * @param errorCount number of errors found so far
     * @return true when the limit is exceeded and the controls must stop
     */
    public static boolean isExceeded(int errorCount) {
        return errorCount > MAX_ERRORS;
    }

    /**
     * @param errors errors and warnings of a job, may be null
     * @return the first {@link #MAX_ERRORS} errors followed by a {@link AnalyzeImportErrorType#TOO_MANY_ERRORS} alert
     * when the limit is exceeded, otherwise the errors unchanged
     */
    public static List<JobError> cap(List<JobError> errors) {
        if (errors == null || !isExceeded(errors.size())) {
            return errors;
        }
        List<JobError> capped = new ArrayList<>(errors.subList(0, MAX_ERRORS));
        boolean failed = capped.stream().anyMatch(error -> error.getSeverity() == JobErrorSeverity.ERROR);
        JobError alert = failed ? JobError.error(AnalyzeImportErrorType.TOO_MANY_ERRORS) : JobError.warning(AnalyzeImportErrorType.TOO_MANY_ERRORS);
        capped.add(alert.withParam(MAX_ERRORS_PARAM, String.valueOf(MAX_ERRORS)));
        return capped;
    }
}
