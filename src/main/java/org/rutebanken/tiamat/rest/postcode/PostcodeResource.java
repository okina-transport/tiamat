package org.rutebanken.tiamat.rest.postcode;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.general.ImportJobWorkerBuilder;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Transactional
@Path("/get_missing_postcode")
public class PostcodeResource {

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    private final JobRepository jobRepository;
    private final ImportJobWorkerBuilder importJobWorkerBuilder;

    public PostcodeResource(JobRepository jobRepository, ImportJobWorkerBuilder importJobWorkerBuilder) {
        this.jobRepository = jobRepository;
        this.importJobWorkerBuilder = importJobWorkerBuilder;
    }

    @POST
    @PreAuthorize("@rolesChecker.hasRoleEdit()")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMissingPostcode() throws IllegalArgumentException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Job job = new Job();
        job.setType(JobType.MISSING_POSTAL_CODE);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);

        ImportJobWorker importJobWorker = importJobWorkerBuilder
                .init(job)
                .withAuthentication(authentication)
                .build();
        importService.submit(importJobWorker);

        return Response.status(200).build();
    }
}
