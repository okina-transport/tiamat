package org.rutebanken.tiamat.rest.parkings;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.general.ImportJobWorkerBuilder;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.gbfs.GbfsImportLinks;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportData;
import org.rutebanken.tiamat.model.gbfs.GbfsValidationOutput;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.service.parking.GbfsInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@Path("/gbfs_parking")
public class GbfsImportResource {

    private static final Logger logger = LoggerFactory.getLogger(GbfsImportResource.class);

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    private final JobRepository jobRepository;
    private final GbfsInputValidator gbfsInputValidator;
    private final ImportJobWorkerBuilder importJobWorkerBuilder;

    public GbfsImportResource(JobRepository jobRepository, GbfsInputValidator gbfsInputValidator, ImportJobWorkerBuilder importJobWorkerBuilder) {
        this.jobRepository = jobRepository;
        this.gbfsInputValidator = gbfsInputValidator;
        this.importJobWorkerBuilder = importJobWorkerBuilder;
    }

    public static String getLastPartOfUrl(String url) {
        int lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
            return url.substring(lastSlashIndex + 1);
        }
        return "";
    }

    @POST
    @Path("/async_import")
    @Consumes({MediaType.APPLICATION_JSON + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importParkingGbfs(GbfsImportLinks gbfsImportLinks) {
        GbfsValidationOutput validation = gbfsInputValidator.validateInput(gbfsImportLinks);
        if (CollectionUtils.isNotEmpty(validation.getErrors())) {
            logger.error("GBFS parking import validation failed : {} error(s)", validation.getErrors().size());
            for (String error : validation.getErrors()) {
                logger.error(error);
            }
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        ParkingTypeEnumeration parkingTypeEnumeration = ParkingTypeEnumeration.fromValue(gbfsImportLinks.getParkingType());
        Job job = new Job();
        job.setFileName(getLastPartOfUrl(gbfsImportLinks.getGlobalUrl()));
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
                .withGbfsParkingImportData(new GbfsParkingImportData(validation.getStations(), validation.getVehicleTypes(), validation.getSystemInformation(), parkingTypeEnumeration))
                .build();
        importService.submit(importJobWorker);

        return Response.status(Response.Status.OK).build();
    }
}
