package org.rutebanken.tiamat.rest.inseecode;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.general.ImportJobWorkerBuilder;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Transactional
@Path("/get_missing_inseecode")
public class InseeCodeResource {

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    private final JobRepository jobRepository;
    private final ImportJobWorkerBuilder importJobWorkerBuilder;

    public InseeCodeResource(JobRepository jobRepository, ImportJobWorkerBuilder importJobWorkerBuilder) {
        this.jobRepository = jobRepository;
        this.importJobWorkerBuilder = importJobWorkerBuilder;
    }

    @POST
    @PreAuthorize("@rolesChecker.hasRoleEdit()")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMissingInseecode() throws IllegalArgumentException {
        Job job = new Job();
        job.setType(JobType.MISSING_INSEE_CODE);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);

        ImportJobWorker importJobWorker = importJobWorkerBuilder
                .init(job)
                .build();

        Runnable secureRunnable = new DelegatingSecurityContextRunnable(
                importJobWorker,
                SecurityContextHolder.getContext()
        );

        importService.submit(secureRunnable);

        return Response.status(200).build();
    }
}
