package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;

/**
 * Raised when a file of a GBFS feed cannot be retrieved: the server answers with an HTTP error status
 * (ex: 503 Service Unavailable), or it cannot be reached (DNS, connection refused, timeout, no content…).
 * The whole import is cancelled before anything is written in database.
 * The file, its url and, when there is one, the HTTP status are exposed as a job error through {@link #toJobError()}.
 */
public class GbfsHttpErrorException extends AnalyzeImportException {

    private final String file;
    private final String url;
    /**
     * null when the server could not be reached
     */
    private final Integer httpStatus;

    public GbfsHttpErrorException(String file, String url, int httpStatus) {
        super(AnalyzeImportErrorType.HTTP_ERROR, "Error requesting GBFS " + file + " from " + url + ": HTTP status " + httpStatus);
        this.file = file;
        this.url = url;
        this.httpStatus = httpStatus;
    }

    /**
     * The server could not be reached, or did not send any content: there is no HTTP status
     */
    public GbfsHttpErrorException(String file, String url) {
        super(AnalyzeImportErrorType.HTTP_ERROR, "Error requesting GBFS " + file + " from " + url);
        this.file = file;
        this.url = url;
        this.httpStatus = null;
    }

    public String getFile() {
        return file;
    }

    public String getUrl() {
        return url;
    }

    /**
     * @return the HTTP status, null when the server could not be reached
     */
    public Integer getHttpStatus() {
        return httpStatus;
    }

    /**
     * @return an HTTP_ERROR job error giving the requested file, its url and the HTTP status when there is one
     */
    public JobError toJobError() {
        JobError jobError = JobError.error(AnalyzeImportErrorType.HTTP_ERROR)
                .withParam("file", file)
                .withParam("url", url);
        return httpStatus != null ? jobError.withParam("httpStatus", String.valueOf(httpStatus)) : jobError;
    }
}
