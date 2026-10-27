package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;

import java.util.List;

/**
 * Raised when an imported file contains several records sharing the same identifier.
 * The duplicated identifiers are exposed as job errors through {@link #toJobErrors()}.
 */
public class DuplicatedIdentifierException extends IllegalArgumentException {

    /**
     * A record whose identifier was already used by a previous record of the file
     *
     * @param line line number in the file, header included (may be null if unknown)
     */
    public record Duplicate(String id, Integer line) {
    }

    private final String fileName;
    private final String fieldName;
    private final List<Duplicate> duplicates;

    public DuplicatedIdentifierException(String message, String fileName, String fieldName, List<Duplicate> duplicates) {
        super(message);
        this.fileName = fileName;
        this.fieldName = fieldName;
        this.duplicates = List.copyOf(duplicates);
    }

    /**
     * @return one DUPLICATE_ID_FILE job error per duplicated record
     */
    public List<JobError> toJobErrors() {
        return duplicates.stream()
                .map(duplicate -> {
                    JobError error = JobError.error(AnalyzeImportErrorType.DUPLICATE_ID_FILE)
                            .withParam("fileName", fileName)
                            .withParam("field", fieldName)
                            .withParam("errorValue", duplicate.id());
                    if (duplicate.line() != null) {
                        error.withParam("line", String.valueOf(duplicate.line()));
                    }
                    return error;
                })
                .toList();
    }
}
