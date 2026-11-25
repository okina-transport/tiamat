package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;

/**
 * Raised when a file of a GBFS feed cannot be retrieved because the server answers with an HTTP error status
 * (ex: 503 Service Unavailable). The whole import is cancelled before anything is written in database.
 * The file, its url and the HTTP status are exposed as a job error through {@link #toJobError()}.
 */
public class GbfsHttpErrorException extends AnalyzeImportException {

    private final String file;
    private final String url;
    private final int httpStatus;

    public GbfsHttpErrorException(String file, String url, int httpStatus) {
        super(AnalyzeImportErrorType.HTTP_ERROR, "Error requesting GBFS " + file + " from " + url + ": HTTP status " + httpStatus);
        this.file = file;
        this.url = url;
        this.httpStatus = httpStatus;
    }

    public String getFile() {
        return file;
    }

    public String getUrl() {
        return url;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * @return an HTTP_ERROR job error giving the requested file, its url and the HTTP status
     */
    public JobError toJobError() {
        return JobError.error(AnalyzeImportErrorType.HTTP_ERROR)
                .withParam("file", file)
                .withParam("url", url)
                .withParam("httpStatus", String.valueOf(httpStatus));
    }
}
