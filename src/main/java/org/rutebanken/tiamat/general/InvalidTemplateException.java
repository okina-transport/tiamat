package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;

import java.util.List;

/**
 * Raised when the headers of an imported CSV file do not match the expected template.
 * The details are exposed as a job error through {@link #toJobError(String)}.
 */
public class InvalidTemplateException extends AnalyzeImportException {

    private final List<String> missingColumns;
    private final List<String> unexpectedColumns;
    private final boolean orderIssue;

    public InvalidTemplateException(String message, List<String> missingColumns, List<String> unexpectedColumns, boolean orderIssue) {
        super(AnalyzeImportErrorType.TEMPLATE, message);
        this.missingColumns = List.copyOf(missingColumns);
        this.unexpectedColumns = List.copyOf(unexpectedColumns);
        this.orderIssue = orderIssue;
    }

    public List<String> getMissingColumns() {
        return missingColumns;
    }

    public List<String> getUnexpectedColumns() {
        return unexpectedColumns;
    }

    public boolean isOrderIssue() {
        return orderIssue;
    }

    /**
     * @param fileName name of the imported file
     * @return a TEMPLATE job error detailing the header mismatch
     */
    public JobError toJobError(String fileName) {
        return JobError.error(AnalyzeImportErrorType.TEMPLATE)
                .withParam("fileName", fileName)
                .withParam("unExpectedColumns", String.join(", ", unexpectedColumns))
                .withParam("missingColumns", String.join(", ", missingColumns))
                .withParam("orderIssue", String.valueOf(orderIssue));
    }
}
