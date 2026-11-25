package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.externalapis.gbfs.GbfsFieldError;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;
import org.rutebanken.tiamat.model.job.JobErrorSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Raised when fields of the feeds of a GBFS feed are in error: required fields missing, fields which are not numbers,
 * or invalid coordinates of stations. The whole import is cancelled before anything is written in database. Each field
 * in error is exposed as a job error through {@link #toJobErrors()}.
 */
public class GbfsFieldErrorException extends AnalyzeImportException {

    private final List<GbfsFieldError> fieldErrors;
    private final boolean otherSchemaErrors;

    /**
     * @param fieldErrors       the fields in error
     * @param otherSchemaErrors true when the validator also reported other errors (wrong value, wrong format…)
     * @param detail            all the errors of the validator, for the logs
     */
    public GbfsFieldErrorException(List<GbfsFieldError> fieldErrors, boolean otherSchemaErrors, String detail) {
        super(AnalyzeImportErrorType.MISSING_REQUIRED_FIELD, "Field(s) in error in GBFS feeds: "
                + fieldErrors.stream().map(GbfsFieldErrorException::describe).collect(Collectors.joining(", "))
                + (detail != null ? "\n" + detail : ""));
        this.fieldErrors = List.copyOf(fieldErrors);
        this.otherSchemaErrors = otherSchemaErrors;
    }

    public List<GbfsFieldError> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * @return one job error per field in error (MISSING_REQUIRED_FIELD, INVALID_FORMAT_NUMBER or
     * INVALID_FORMAT_COORDINATE), followed by a
     * MISSING_DATA job error when the validator also reported other errors
     */
    public List<JobError> toJobErrors() {
        List<JobError> jobErrors = new ArrayList<>();
        for (GbfsFieldError fieldError : fieldErrors) {
            jobErrors.add(toJobError(fieldError, JobErrorSeverity.ERROR));
        }
        if (otherSchemaErrors) {
            jobErrors.add(JobError.error(AnalyzeImportErrorType.MISSING_DATA));
        }
        return jobErrors;
    }

    /**
     * @return a MISSING_REQUIRED_FIELD or INVALID_FORMAT_NUMBER job error giving the file, the object holding the field
     * (entity and entityId, or its position when it has no id), the field and, for a number, the expected type
     */
    public static JobError toJobError(GbfsFieldError fieldError, JobErrorSeverity severity) {
        AnalyzeImportErrorType code = switch (fieldError.kind()) {
            case MISSING -> AnalyzeImportErrorType.MISSING_REQUIRED_FIELD;
            case INVALID_NUMBER, INVALID_INTEGER -> AnalyzeImportErrorType.INVALID_FORMAT_NUMBER;
            case INVALID_COORDINATES -> AnalyzeImportErrorType.INVALID_FORMAT_COORDINATE;
        };
        JobError jobError = new JobError(code, severity).withParam("file", fieldError.file());
        if (fieldError.entity() != null) {
            jobError.withParam("entity", fieldError.entity());
            if (fieldError.entityId() != null) {
                jobError.withParam("entityId", fieldError.entityId());
            } else {
                jobError.withParam("position", String.valueOf(fieldError.position()));
            }
        }
        if (fieldError.kind() == GbfsFieldError.Kind.INVALID_COORDINATES) {
            return jobError.withParam("coordinates", fieldError.value());
        }
        jobError.withParam("field", fieldError.field());
        if (fieldError.kind() != GbfsFieldError.Kind.MISSING) {
            jobError.withParam("expected", fieldError.kind() == GbfsFieldError.Kind.INVALID_INTEGER ? "integer" : "number");
        }
        return jobError;
    }

    private static String describe(GbfsFieldError fieldError) {
        String what = switch (fieldError.kind()) {
            case MISSING -> "missing ";
            case INVALID_NUMBER, INVALID_INTEGER -> "not a number ";
            case INVALID_COORDINATES -> "invalid coordinates (" + fieldError.value() + ") ";
        };
        if (fieldError.entity() == null) {
            return fieldError.file() + " " + what + fieldError.field();
        }
        String ref = fieldError.entityId() != null ? fieldError.entityId() : "#" + fieldError.position();
        return fieldError.file() + " " + fieldError.entity() + " " + ref + " " + what + fieldError.field();
    }
}
