package org.rutebanken.tiamat.rest.parkings;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.general.BikesCSVHelper;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.dto.DtoBikeParking;
import org.rutebanken.tiamat.service.parking.BikeParkingsImportedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Component
@Path("/bike_parking")
public class ImportBikeParkingsResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportBikeParkingsResource.class);

    private BikeParkingsImportedService bikeParkingsImportedService;


    @Autowired
    JobRepository jobRepository;

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());


    @Autowired
    ImportBikeParkingsResource(BikeParkingsImportedService bikeParkingsImportedService){
        this.bikeParkingsImportedService = bikeParkingsImportedService;
    }

    @POST
    @Path("/bike_parking_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importBikeParkingsCsvFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {
        try {

            logger.info("Import Parkings Velo par " + user + " du fichier " + fileName);

            List<DtoBikeParking> dtoBikeParkingsCSV = BikesCSVHelper.parseDocument(inputStream);

            BikesCSVHelper.checkDuplicatedBikeParkings(dtoBikeParkingsCSV);

            List<Parking> bikeParkings = BikesCSVHelper.mapFromDtoToEntityParking(dtoBikeParkingsCSV, false);

            bikeParkingsImportedService.createBikeParkings(bikeParkings);

            return Response.status(200).build();

        } catch (IOException e) {
            logger.debug("Access denied for csv File: " + e.getMessage(), e);
            throw e;
        }catch (IllegalArgumentException e) {
            logger.warn("Caught exception while processing data in the cvs file: " + e.getMessage(), e);
            throw e;
        }
    }

    @POST
    @Path("/bike_parking_sync_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAsyncBikeParkingsCsvFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {
        logger.info("Import Parkings Velo par " + user + " du fichier " + fileName);


        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.CSV_BIKE_PARKING);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);

        ImportJobWorker importJobWorker = new ImportJobWorker(job, inputStream, jobRepository);
        importJobWorker.setBikeParkingsImportedService(bikeParkingsImportedService);
        importService.submit(importJobWorker);

        return Response.status(200).build();

        }



}
