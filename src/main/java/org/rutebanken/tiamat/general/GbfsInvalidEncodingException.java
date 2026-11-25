package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;

/**
 * Raised when a file of a GBFS feed is not valid UTF-8. The whole import is cancelled before anything is written in
 * database. The name of the file is exposed as a job error through {@link #toJobError()}.
 */
public class GbfsInvalidEncodingException extends AnalyzeImportException {

    private final String file;

    public GbfsInvalidEncodingException(String file) {
        super(AnalyzeImportErrorType.ENCODING, "The GBFS file " + file + " is not valid UTF-8");
        this.file = file;
    }

    public String getFile() {
        return file;
    }

    /**
     * @return an ENCODING job error giving the name of the file (ex: station_information.json)
     */
    public JobError toJobError() {
        return JobError.error(AnalyzeImportErrorType.ENCODING).withParam("file", file);
    }
}
