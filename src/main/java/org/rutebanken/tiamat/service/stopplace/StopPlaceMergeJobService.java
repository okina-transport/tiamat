package org.rutebanken.tiamat.service.stopplace;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeRequestDto;
import org.rutebanken.tiamat.general.StopPlaceMergeJobWorker;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class StopPlaceMergeJobService {

    private static final ExecutorService mergeService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat("stop-place-merge-%d").build());

    private final JobRepository jobRepository;
    private final StopPlaceMerger stopPlaceMerger;
    private final UsernameFetcher usernameFetcher;

    public StopPlaceMergeJobService(JobRepository jobRepository, StopPlaceMerger stopPlaceMerger, UsernameFetcher usernameFetcher) {
        this.jobRepository = jobRepository;
        this.stopPlaceMerger = stopPlaceMerger;
        this.usernameFetcher = usernameFetcher;
    }

    public Job triggerMerge(List<StopPlaceMergeRequestDto> couples) {
        if (CollectionUtils.isEmpty(couples)) {
            throw new BadRequestException("Request body must contain at least one merge couple");
        }
        for (StopPlaceMergeRequestDto couple : couples) {
            if (StringUtils.isBlank(couple.getTarget())
                    || StringUtils.isBlank(couple.getOrigin())
                    || StringUtils.equals(couple.getTarget(), couple.getOrigin())) {
                throw new BadRequestException("Each couple must have a non-null 'target' and 'origin', and they must not be equal");
            }
        }

        if (CollectionUtils.isNotEmpty(jobRepository.findByTypeAndStatus(JobType.STOP_PLACE_MERGE, JobStatus.PROCESSING))) {
            throw new ClientErrorException("A stop place merge job is already running", Response.Status.CONFLICT);
        }

        Job job = new Job();
        job.setType(JobType.STOP_PLACE_MERGE);
        job.setAction(JobAction.MERGE);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        job.setUserName(usernameFetcher.getUserNameForAuthenticatedUser());
        job.setTotalCount(couples.size());
        job.setRemainingCount(couples.size());
        jobRepository.save(job);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        mergeService.submit(new StopPlaceMergeJobWorker(job, couples, stopPlaceMerger, jobRepository, authentication));

        return job;
    }

    public Job getMergeJobProgress(long jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getType() != JobType.STOP_PLACE_MERGE) {
            throw new NotFoundException("No stop place merge job found with id " + jobId);
        }
        return job;
    }
}
