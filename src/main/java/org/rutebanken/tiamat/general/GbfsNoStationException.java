package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;

/**
 * Raised when the station_information.json file of a GBFS feed does not contain any station: there is nothing to
 * import. The name of the file is exposed as a job error through {@link #toJobError()}.
 */
public class GbfsNoStationException extends AnalyzeImportException {

    private final String file;

    public GbfsNoStationException(String file) {
        super(AnalyzeImportErrorType.NO_DATA_ROWS, "No station in " + file);
        this.file = file;
    }

    public String getFile() {
        return file;
    }

    /**
     * @return a NO_DATA_ROWS job error giving the name of the file (station_information.json)
     */
    public JobError toJobError() {
        return JobError.error(AnalyzeImportErrorType.NO_DATA_ROWS).withParam("file", file);
    }
}
