package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;

import java.util.List;

/**
 * Raised when required feeds are not declared in the gbfs.json file of a GBFS feed.
 * Each missing feed is exposed as a job error through {@link #toJobErrors()}.
 */
public class MissingGbfsFeedException extends AnalyzeImportException {

    private final List<String> missingFeeds;

    public MissingGbfsFeedException(List<String> missingFeeds) {
        super(AnalyzeImportErrorType.MISSING_REQUIRED_GBFS_FEED,
                "Required feed(s) missing in gbfs.json: " + String.join(", ", missingFeeds));
        this.missingFeeds = List.copyOf(missingFeeds);
    }

    public List<String> getMissingFeeds() {
        return missingFeeds;
    }

    /**
     * @return one MISSING_REQUIRED_GBFS_FEED job error per missing feed
     */
    public List<JobError> toJobErrors() {
        return missingFeeds.stream()
                .map(feed -> JobError.error(AnalyzeImportErrorType.MISSING_REQUIRED_GBFS_FEED).withParam("feed", feed))
                .toList();
    }
}
