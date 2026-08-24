package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeRequestDto;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.service.stopplace.StopPlaceMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StopPlaceMergeJobWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceMergeJobWorker.class);

    private final Job job;
    private final List<StopPlaceMergeRequestDto> couples;
    private final StopPlaceMerger stopPlaceMerger;
    private final JobRepository jobRepository;
    private final Authentication authentication;
    private final Map<String, String> netexIdRef = new HashMap<>();

    public StopPlaceMergeJobWorker(Job job,
                                   List<StopPlaceMergeRequestDto> couples,
                                   StopPlaceMerger stopPlaceMerger,
                                   JobRepository jobRepository,
                                   Authentication authentication) {
        this.job = job;
        this.couples = couples;
        this.stopPlaceMerger = stopPlaceMerger;
        this.jobRepository = jobRepository;
        this.authentication = authentication;
    }

    @Override
    public void run() {
        logger.info("Started stop place merge job: {}", job);
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        try {
            for (StopPlaceMergeRequestDto couple : couples) {
                String target = findNetexId(couple.getTarget());
                String origin = findNetexId(couple.getOrigin());
                if (!target.equals(origin)) {
                    String fromVersionComment = "Merged automatically via mergeable stop places job (job " + job.getId() + ")";
                    String toVersionComment = "Merged automatically via mergeable stop places job (job " + job.getId() + ")";
                    stopPlaceMerger.mergeStopPlaces(origin, target, fromVersionComment, toVersionComment, false);
                    netexIdRef.put(origin, target);
                } else {
                    logger.info("Skipping redundant merge couple target={}, origin={} for job {}", couple.getTarget(), couple.getOrigin(), job.getId());
                }

                job.setRemainingCount(job.getRemainingCount() - 1);
                jobRepository.save(job);
            }

            job.setStatus(JobStatus.FINISHED);
            job.setFinished(Instant.now());
            logger.info("Stop place merge job done: {}", job);
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setFinished(Instant.now());
            String message = "Error executing stop place merge job " + job.getId() + ". " + e.getClass().getSimpleName() + " - " + e.getMessage();
            logger.error("{}.\nStop place merge job was {}", message, job, e);
            job.setMessage(message);
        } finally {
            jobRepository.save(job);
        }
    }

    private String findNetexId(String id) {
        String root = id;
        while (netexIdRef.containsKey(root)) {
            root = netexIdRef.get(root);
        }
        netexIdRef.put(id, root);
        return root;
    }
}
