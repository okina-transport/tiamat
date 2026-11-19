package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;
import org.rutebanken.tiamat.validator.FieldLengthValidator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Raised when text values of an imported file exceed the size of their database column.
 * The whole file is rejected: the values are exposed as job errors through {@link #toJobErrors(NetexLineLocator)}.
 */
public class FieldTooLongException extends AnalyzeImportException {

    private final List<FieldLengthValidator.Violation> violations;

    public FieldTooLongException(List<FieldLengthValidator.Violation> violations) {
        super(AnalyzeImportErrorType.FIELD_TOO_LONG, "Values exceed the maximum length of their field: " + violations.stream()
                .map(violation -> violation.entity() + " " + violation.entityIds() + " " + violation.field()
                        + " (" + violation.length() + " > " + violation.maxLength() + ")")
                .collect(Collectors.joining(", ")));
        this.violations = List.copyOf(violations);
    }

    public List<FieldLengthValidator.Violation> getViolations() {
        return violations;
    }

    /**
     * @param lineLocator locator of the entities in the imported file, may be null
     * @return one FIELD_TOO_LONG job error per value exceeding its maximum length
     */
    public List<JobError> toJobErrors(NetexLineLocator lineLocator) {
        return violations.stream()
                .map(violation -> {
                    JobError error = JobError.error(AnalyzeImportErrorType.FIELD_TOO_LONG)
                            .withParam("field", violation.field());
                    if (violation.entity() != null) {
                        error.withParam("entity", violation.entity());
                    }
                    // the id reported is the one used in the file: the first candidate id found as an id attribute
                    String entityId = violation.entityIds().isEmpty() ? null : violation.entityIds().getFirst();
                    Integer line = null;
                    if (lineLocator != null) {
                        for (String candidateId : violation.entityIds()) {
                            line = lineLocator.findIdLine(candidateId);
                            if (line != null) {
                                entityId = candidateId;
                                break;
                            }
                        }
                    }
                    if (entityId != null) {
                        error.withParam("entityId", entityId);
                    }
                    if (line != null) {
                        error.withParam("line", String.valueOf(line));
                    }
                    return error
                            .withParam("length", String.valueOf(violation.length()))
                            .withParam("maxLength", String.valueOf(violation.maxLength()));
                })
                .distinct()
                .toList();
    }
}
