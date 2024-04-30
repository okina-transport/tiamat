package org.rutebanken.tiamat.repository;

import org.rutebanken.tiamat.model.job.JobStatus;

import java.util.List;

public interface JobRepositoryCustom<Job> {
    List<Job> findByReferential(String referential, JobStatus status);

    List<Job> findByReferentialAndAction(String referential, List<String> actions, JobStatus status);

    Job findByReferentialAndId(String referential, Long id);

    Job terminatedJob(String referential, Long id);
}
