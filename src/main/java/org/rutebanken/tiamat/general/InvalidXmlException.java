package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;
import org.xml.sax.SAXParseException;

/**
 * Raised when an imported NeTEx file is not well-formed XML or does not respect the NeTEx XSD.
 * The location of the error (line, column) and the parser message are exposed as a job error
 * through {@link #toJobError()}.
 */
public class InvalidXmlException extends AnalyzeImportException {

    private final Integer line;
    private final Integer column;
    private final String detail;

    public InvalidXmlException(String message, Integer line, Integer column, String detail) {
        super(AnalyzeImportErrorType.TEMPLATE, message);
        this.line = line;
        this.column = column;
        this.detail = detail;
    }

    public static InvalidXmlException from(SAXParseException parseException) {
        Integer line = parseException.getLineNumber() > 0 ? parseException.getLineNumber() : null;
        Integer column = parseException.getColumnNumber() > 0 ? parseException.getColumnNumber() : null;
        String detail = parseException.getMessage();
        String message = "Le fichier NeTEx est invalide ou mal formé"
                + (line != null ? " (ligne " + line + (column != null ? ", colonne " + column : "") + ")" : "")
                + " : " + detail;
        return new InvalidXmlException(message, line, column, detail);
    }

    public Integer getLine() {
        return line;
    }

    public Integer getColumn() {
        return column;
    }

    public String getDetail() {
        return detail;
    }

    /**
     * @return a TEMPLATE job error giving the location of the XML error and the parser message
     */
    public JobError toJobError() {
        JobError jobError = JobError.error(AnalyzeImportErrorType.TEMPLATE);
        if (line != null) {
            jobError.withParam("line", String.valueOf(line));
        }
        if (column != null) {
            jobError.withParam("column", String.valueOf(column));
        }
        if (detail != null) {
            jobError.withParam("detail", detail);
        }
        return jobError;
    }
}
