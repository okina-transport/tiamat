package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.AnalyzeImportError;

import java.util.List;
import java.util.stream.Collectors;

public class AnalyzeImportException extends IllegalArgumentException {

    private final List<AnalyzeImportError> errors;

    public AnalyzeImportException(AnalyzeImportErrorType type, String message) {
        this(List.of(new AnalyzeImportError(type, message, null, null)));
    }

    public AnalyzeImportException(List<AnalyzeImportError> errors) {
        super(buildMessage(errors));
        this.errors = errors;
    }

    public List<AnalyzeImportError> getErrors() {
        return errors;
    }

    private static String buildMessage(List<AnalyzeImportError> errors) {
        return errors.stream()
                .map(AnalyzeImportException::formatError)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static String formatError(AnalyzeImportError error) {
        StringBuilder sb = new StringBuilder();
        if (error.getLine() != null) {
            sb.append("Line ").append(error.getLine()).append(": ");
        }
        sb.append(error.getMessage());
        return sb.toString();
    }
}
