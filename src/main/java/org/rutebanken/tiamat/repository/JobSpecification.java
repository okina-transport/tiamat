package org.rutebanken.tiamat.repository;

import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobType;
import org.springframework.data.jpa.domain.Specification;

public class JobSpecification {

    private JobSpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<Job> jobTypeFilter(JobType jobType) {
        return (root, query, cb) -> cb.equal(root.get("type"), jobType);
    }

    public static Specification<Job> jobActionFilter(JobAction jobAction) {
        return (root, query, cb) -> cb.equal(root.get("action") ,jobAction);
    }

    public static Specification<Job> providerFilter(String provider) {
        return (root, query, cb) -> cb.equal(root.get("subFolder"), provider);
    }
}
