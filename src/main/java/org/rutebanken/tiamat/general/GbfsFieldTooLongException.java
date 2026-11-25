package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.externalapis.gbfs.GbfsFieldLengthChecker;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Raised when text values of a GBFS feed exceed the size of their database column. The whole import is cancelled
 * before anything is written in database. Each value is exposed as a job error through {@link #toJobErrors()}.
 */
public class GbfsFieldTooLongException extends AnalyzeImportException {

    private final List<GbfsFieldLengthChecker.Violation> violations;

    public GbfsFieldTooLongException(List<GbfsFieldLengthChecker.Violation> violations) {
        super(AnalyzeImportErrorType.FIELD_TOO_LONG, "Values of GBFS feeds exceed the maximum length of their field: "
                + violations.stream().map(GbfsFieldTooLongException::describe).collect(Collectors.joining(", ")));
        this.violations = List.copyOf(violations);
    }

    public List<GbfsFieldLengthChecker.Violation> getViolations() {
        return violations;
    }

    /**
     * @return one FIELD_TOO_LONG job error per value, giving the file, the station (its id, or its position when it
     * has no id), the GBFS field, the length of the value and the maximum length
     */
    public List<JobError> toJobErrors() {
        return violations.stream().map(GbfsFieldTooLongException::toJobError).toList();
    }

    private static JobError toJobError(GbfsFieldLengthChecker.Violation violation) {
        JobError error = JobError.error(AnalyzeImportErrorType.FIELD_TOO_LONG).withParam("file", violation.file());
        if (violation.entity() != null) {
            error.withParam("entity", violation.entity());
            if (violation.entityId() != null) {
                error.withParam("entityId", violation.entityId());
            } else {
                error.withParam("position", String.valueOf(violation.position()));
            }
        }
        return error.withParam("field", violation.field())
                .withParam("length", String.valueOf(violation.length()))
                .withParam("maxLength", String.valueOf(violation.maxLength()));
    }

    private static String describe(GbfsFieldLengthChecker.Violation violation) {
        String ref = violation.entity() == null ? ""
                : " " + violation.entity() + " " + (violation.entityId() != null ? violation.entityId() : "#" + violation.position());
        return violation.file() + ref + " " + violation.field() + " (" + violation.length() + " > " + violation.maxLength() + ")";
    }
}
