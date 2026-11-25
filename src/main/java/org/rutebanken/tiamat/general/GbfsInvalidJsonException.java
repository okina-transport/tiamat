package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;

/**
 * Raised when a feed of a GBFS feed cannot be read as a JSON object (truncated JSON, HTML page, empty file…).
 * The whole import is cancelled before anything is written in database.
 * The name of the file is exposed as a job error through {@link #toJobError()}.
 */
public class GbfsInvalidJsonException extends AnalyzeImportException {

    private final String file;

    public GbfsInvalidJsonException(String file, String detail) {
        super(AnalyzeImportErrorType.INVALID_JSON, "The GBFS file " + file + " is not a readable JSON object: " + detail);
        this.file = file;
    }

    public String getFile() {
        return file;
    }

    /**
     * @return an INVALID_JSON job error giving the name of the file (ex: system_information.json)
     */
    public JobError toJobError() {
        return JobError.error(AnalyzeImportErrorType.INVALID_JSON).withParam("file", file);
    }
}
