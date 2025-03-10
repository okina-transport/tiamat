package org.rutebanken.tiamat.repository;

import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;

import java.util.List;

public interface JobRepositoryCustom<Job> {
    List<Job> findByReferential(String referential, JobStatus status);

    List<Job> findByReferentialAndAction(String referential, List<String> actions, JobStatus status);

    Job findBySubFolderLikeReferentialAndId(String subFolder, Long id);

    Job terminatedJob(String referential, Long id);

    List<Job> findAllExportBy(String referential, JobType jobType, JobStatus status);

    List<Job> findAllExportBy(String referential, JobType jobType, int maxSize);
}
