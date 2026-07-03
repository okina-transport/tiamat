package org.rutebanken.tiamat.rest.poi;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.changelog.LoggingService;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.general.ImportJobWorkerBuilder;
import org.rutebanken.tiamat.general.PointOfInterestCSVHelper;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.dto.DtoPointOfInterest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Path("poi")
public class ImportPOIResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportPOIResource.class);
    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    private final PointOfInterestCSVHelper poiHelper;
    private final JobRepository jobRepository;
    private final ImportJobWorkerBuilder importJobWorkerBuilder;
    private final LoggingService loggingService;

    public ImportPOIResource(PointOfInterestCSVHelper poiHelper, JobRepository jobRepository,
                             ImportJobWorkerBuilder importJobWorkerBuilder, LoggingService loggingService) {
        this.poiHelper = poiHelper;
        this.jobRepository = jobRepository;
        this.importJobWorkerBuilder = importJobWorkerBuilder;
        this.loggingService = loggingService;
    }

    @POST
    @Path("/poi_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importPOIFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {
        logger.info("Import POI par " + user + " du fichier " + fileName);
        loggingService.logPoiCsvImport(user, fileName);

        poiHelper.clearClassificationCache();

        List<DtoPointOfInterest> dtoPointOfInterest = poiHelper.parseDocument(inputStream);
        PointOfInterestCSVHelper.checkDuplicatedPois(dtoPointOfInterest);
        List<DtoPointOfInterest> poiWithClassification = poiHelper.filterPoisWithClassificationOrShop(dtoPointOfInterest, null);

        //poiHelper.clearPOIExceptShop();

        try {
            poiHelper.persistPointsOfInterest(poiWithClassification);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        logger.info("Import POI par " + user + " du fichier " + fileName + " terminé");

        return Response.status(200).build();
    }

    @GET
    @Path("/poi_import_list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPOIImportList() {
        List<JobType> poiTypes = Arrays.asList(JobType.CSV_POI, JobType.NETEX_POI);
        try {
            List<Job> foundJobs = jobRepository.findByTypesAndAction(poiTypes, JobAction.IMPORT);
            return Response.ok(foundJobs).build();
        } catch (Exception e) {
            logger.error("Error while getting poi import list", e);

        }

        return Response.status(500).build();
    }

    @POST
    @Path("/poi_async_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAsyncPOIFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {
        loggingService.logPoiCsvImport(user, fileName);
        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.CSV_POI);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);
        logger.info("Import points de vente par " + user + " du fichier " + fileName);

        ImportJobWorker importJobWorker = importJobWorkerBuilder
                .init(job)
                .withInputStream(inputStream)
                .build();
        importService.submit(importJobWorker);

        return Response.status(200).build();
    }
}
