package org.rutebanken.tiamat.service.job;

/**
 * Summary of a completed jobs purge.
 */
public record JobPurgeResult(int deletedJobs,
                             int deletedLocalFiles,
                             int deletedBlobs,
                             int failedJobs) {
}
