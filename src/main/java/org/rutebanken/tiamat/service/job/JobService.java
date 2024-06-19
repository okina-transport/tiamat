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

    public Job scheduledJob(String subFolder, Long id) throws ServiceException {
        return getJobService(subFolder, id);
    }

    public Job getJobService(String subFolder, Long id) throws ServiceException {

        Job job = jobRepository.findBySubFolderLikeReferentialAndId(subFolder, id);
        if (job != null) {
            return job;
        }
        throw new ServiceException("subFolder = " + subFolder + " ,id = " + id);
    }
}
