package org.rutebanken.tiamat.service.job;

import com.amazonaws.services.directory.model.ServiceException;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;


@Service
@Transactional
public class JobService {

    @Autowired
    JobRepository jobRepository;

    @Autowired
    JobService(){
    }

    public List<Job> jobs(String referential, List<String> actions, JobStatus status) throws ServiceException {

        List<Job> jobs;
        if (actions == null) {
            jobs = jobRepository.findByReferential(referential, status);
        } else {
            jobs = jobRepository.findByReferentialAndAction(referential, actions, status);
        }

        return jobs;
    }

    public Job scheduledJob(String referential, Long id) throws ServiceException {
        return getJobService(referential, id);
    }

    public Job getJobService(String referential, Long id) throws ServiceException {

        Job job = jobRepository.findByReferentialAndId(referential, id);
        if (job != null) {
            return job;
        }
        throw new ServiceException("referential = " + referential + " ,id = " + id);
    }
}
