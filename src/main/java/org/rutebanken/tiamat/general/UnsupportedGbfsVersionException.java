package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;

import java.util.List;

/**
 * Raised when the gbfs.json file of a GBFS feed is valid but its version is not supported by the import.
 * The received version and the supported versions are exposed as a job error through {@link #toJobError()}.
 */
public class UnsupportedGbfsVersionException extends AnalyzeImportException {

    private final String version;
    private final List<String> supportedVersions;

    public UnsupportedGbfsVersionException(String version, List<String> supportedVersions) {
        super(AnalyzeImportErrorType.UNSUPPORTED_GBFS_VERSION,
                "Unsupported GBFS version: " + version + " (supported versions: " + String.join(", ", supportedVersions) + ")");
        this.version = version;
        this.supportedVersions = List.copyOf(supportedVersions);
    }

    public String getVersion() {
        return version;
    }

    public List<String> getSupportedVersions() {
        return supportedVersions;
    }

    /**
     * @return an UNSUPPORTED_GBFS_VERSION job error giving the received version and the supported versions
     */
    public JobError toJobError() {
        return JobError.error(AnalyzeImportErrorType.UNSUPPORTED_GBFS_VERSION)
                .withParam("version", String.valueOf(version))
                .withParam("supportedVersions", String.join(", ", supportedVersions));
    }
}
