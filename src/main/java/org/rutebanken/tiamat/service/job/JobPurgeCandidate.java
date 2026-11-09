package org.rutebanken.tiamat.service.job;

import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobType;

import java.time.Instant;

/**
 * Lightweight projection of a job, used to decide which completed jobs can be purged.
 *
 * @param completedAt finished date, or started date when finished is missing
 */
public record JobPurgeCandidate(Long id,
                                String subFolder,
                                JobType type,
                                JobAction action,
                                String fileName,
                                Instant completedAt) {
}
