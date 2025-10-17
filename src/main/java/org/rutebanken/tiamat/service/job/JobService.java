package org.rutebanken.tiamat.service.job;

import com.amazonaws.services.directory.model.ServiceException;
import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;


@Service
@Transactional
public class JobService {

    private final JobRepository jobRepository;

    JobService(JobRepository jobRepository){
        this.jobRepository = jobRepository;
    }

    public Job getJobById(long id) {
        return jobRepository.findById(id).orElse(null);
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

    public Job getJobByFileNameAndSubFolder(String fileName, String subFolder) throws ServiceException {

        Job job = jobRepository.findByFileNameAndSubFolder(fileName, subFolder);
        if (job != null) {
            return job;
        }
        throw new ServiceException("fileName = " + fileName + ", subFolder = " + subFolder);
    }

    public Job findLatestJobBy(String referential, JobType jobType, JobStatus jobStatus) {
        List<Job> matchingJobs = jobRepository.findAllExportBy(referential, jobType, jobStatus);
        if (CollectionUtils.isNotEmpty(matchingJobs)) {
            return matchingJobs.get(0);
        }
        return null;
    }

    public List<Job> findAllJobBy(String referential, JobType jobType, int maxSize) {
        return jobRepository.findAllExportBy(referential, jobType, maxSize);
    }
}
