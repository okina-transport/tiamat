package org.rutebanken.tiamat.rest.parkings;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.validation.Valid;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.general.ImportJobWorkerBuilder;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportParams;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@Path("/gbfs_parking")
public class GbfsImportResource {

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    private final JobRepository jobRepository;
    private final ImportJobWorkerBuilder importJobWorkerBuilder;

    public GbfsImportResource(JobRepository jobRepository, ImportJobWorkerBuilder importJobWorkerBuilder) {
        this.jobRepository = jobRepository;
        this.importJobWorkerBuilder = importJobWorkerBuilder;
    }

    @POST
    @Path("/async_import")
    @Consumes({MediaType.APPLICATION_JSON + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importParkingGbfs(@Valid GbfsParkingImportParams gbfsParkingImportParams) {
        Job job = new Job();
        job.setFileName(gbfsParkingImportParams.getGlobalUrl().toString());
        job.setType(JobType.GBFS_PARKING);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            job.setUserName(SecurityContextHolder.getContext().getAuthentication().getName());
        }
        jobRepository.save(job);

        ImportJobWorker importJobWorker = importJobWorkerBuilder
                .init(job)
                .withGbfsParkingImportParams(gbfsParkingImportParams)
                .build();
        importService.submit(importJobWorker);

        return Response.status(Response.Status.OK).build();
    }
}
