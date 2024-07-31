package org.rutebanken.tiamat.rest.poi;



import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.general.PointOfInterestCSVHelper;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.dto.DtoPointOfInterest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
@Path("poi")
public class ImportPOIResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportPOIResource.class);

    @Autowired
    PointOfInterestCSVHelper poiHelper;

    @Autowired
    JobRepository jobRepository;

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());



    @POST
    @Path("/poi_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importPOIFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {

        logger.info("Import POI par " + user + " du fichier " + fileName);
        poiHelper.clearClassificationCache();

        List<DtoPointOfInterest> dtoPointOfInterest = poiHelper.parseDocument(inputStream);
        PointOfInterestCSVHelper.checkDuplicatedPois(dtoPointOfInterest);
        List<DtoPointOfInterest> poiWithClassification = poiHelper.filterPoisWithClassification(dtoPointOfInterest);

        //poiHelper.clearPOIExceptShop();

        try {
            poiHelper.persistPointsOfInterest(poiWithClassification);
        }catch(Exception e){
            logger.error(e.getMessage(),e);
        }

        logger.info("Import POI par " + user + " du fichier " + fileName + " terminé");

        return Response.status(200).build();
    }

    @GET
    @Path("/poi_import_list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPOIImportList() {
        List<JobType> poiTypes = Arrays.asList(JobType.CSV_POI,JobType.NETEX_POI);
        try {
            List<Job> foundJobs = jobRepository.findByTypesAndAction(poiTypes, JobAction.IMPORT);
            return Response.ok(foundJobs).build();
        }catch(Exception e){
            logger.error("Error while getting poi import list", e);

        }

      return Response.status(500).build();
    }

    @GET
    @Path("/shop_import_list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getShopImportList() {
        List<JobType> poiTypes = Arrays.asList(JobType.CSV_SHOP);
        try {
            List<Job> foundJobs = jobRepository.findByTypesAndAction(poiTypes, JobAction.IMPORT);
            return Response.ok(foundJobs).build();
        }catch(Exception e){
            logger.error("Error while getting shop import list", e);

        }

        return Response.status(500).build();
    }


    @POST
    @Path("/poi_async_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAsyncPOIFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {
        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.CSV_POI);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);
        logger.info("Import points de vente par " + user + " du fichier " + fileName);

        ImportJobWorker importJobWorker = new ImportJobWorker(job, poiHelper, inputStream, jobRepository);
        importService.submit(importJobWorker);



        return Response.status(200).build();
    }

    @POST
    @Path("/shop_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importShopCsvFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {

        logger.info("Import points de vente par " + user + " du fichier " + fileName);

        List<DtoPointOfInterest> dtoPointOfInterest = poiHelper.parseDocument(inputStream);
        PointOfInterestCSVHelper.checkDuplicatedPois(dtoPointOfInterest);
        poiHelper.checkShops(dtoPointOfInterest);

        //poiHelper.clearPOIForShopClassification();

        try {
            poiHelper.persistPointsOfInterest(dtoPointOfInterest);
        }catch(Exception e){
            logger.error(e.getMessage(),e);
        }
        return Response.status(200).build();
    }


    @POST
    @Path("/shop_async_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAsyncShopCsvFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {
        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.CSV_SHOP);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);

        ImportJobWorker importJobWorker = new ImportJobWorker(job, poiHelper, inputStream, jobRepository);
        importService.submit(importJobWorker);


        return Response.status(200).build();
    }


}
