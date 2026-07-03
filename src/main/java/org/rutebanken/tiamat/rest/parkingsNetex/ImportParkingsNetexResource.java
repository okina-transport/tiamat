package org.rutebanken.tiamat.rest.parkingsNetex;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.changelog.LoggingService;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.general.ImportJobWorkerBuilder;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Path("/netex_parking")
public class ImportParkingsNetexResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportParkingsNetexResource.class);
    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    private final ImportJobWorkerBuilder importJobWorkerBuilder;
    private final JobRepository jobRepository;
    private final LoggingService loggingService;
    private final UsernameFetcher usernameFetcher;

    public ImportParkingsNetexResource(ImportJobWorkerBuilder importJobWorkerBuilder, JobRepository jobRepository, LoggingService loggingService, UsernameFetcher usernameFetcher) {
        this.importJobWorkerBuilder = importJobWorkerBuilder;
        this.jobRepository = jobRepository;
        this.loggingService = loggingService;
        this.usernameFetcher = usernameFetcher;
    }

    @PreAuthorize("@rolesChecker.hasRoleEdit()")
    @POST
    @Path("parking_async_import_netex")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAsyncParkingsNetexFile(@FormDataParam("file") InputStream inputStream,
                                                 @FormDataParam("file_name") String fileName,
                                                 @FormDataParam("provider") String provider,
                                                 @FormDataParam("folder") String folder) throws IOException {
        logger.info("Received Parking Netex publication delivery, starting to parse...");
        loggingService.logParkingNetexImport(usernameFetcher.getUserNameForAuthenticatedUser(), fileName);

        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.NETEX_PARKING);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        job.setSubFolder(folder);
        jobRepository.save(job);

        ImportJobWorker importJobWorker = importJobWorkerBuilder
                .init(job)
                .withInputStream(inputStream)
                .build();
        importService.submit(importJobWorker);

        Response.ResponseBuilder builder = Response.accepted();

        if (provider != null) {
            return builder.location(URI.create(String.format("/services/stop_places/jobs/%s/scheduled_jobs/%d", folder, job.getId()))).build();
        } else {
            return builder.build();
        }
    }
}
