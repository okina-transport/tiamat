package org.rutebanken.tiamat.rest.parkings;

import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.general.ParkingsCSVHelper;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingLayoutEnumeration;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.dto.DtoParking;
import org.rutebanken.tiamat.service.parking.ParkingsImportedService;
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


@Component
@Path("/parking")
public class ImportParkingsResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportParkingsResource.class);

    private ParkingsImportedService parkingsImportedService;

    @Autowired
    JobRepository jobRepository;

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    @Autowired
    ImportParkingsResource(ParkingsImportedService parkingsImportedService){
        this.parkingsImportedService=parkingsImportedService;
    }

    @POST
    @Path("/parking_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importParkingsCsvFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user,
                                          @FormDataParam("parking_type") String parkingTypeParam, @FormDataParam("parking_layout") String parkingLayoutParam,
                                          @FormDataParam("park_and_ride_detection") Boolean parkAndRideDetection) throws IOException, IllegalArgumentException {
        try {

            ParkingLayoutEnumeration  parkingLayoutEnumeration = ParkingLayoutEnumeration.fromValue(parkingLayoutParam);
            ParkingTypeEnumeration parkingTypeEnumeration = ParkingTypeEnumeration.fromValue(parkingTypeParam);

            logger.info("Import Parkings par " + user + " du fichier " + fileName);

            List<DtoParking> dtoParkingCSV = ParkingsCSVHelper.parseDocument(inputStream);

            ParkingsCSVHelper.checkDuplicatedParkings(dtoParkingCSV);

            List<Parking> parkings = ParkingsCSVHelper.mapFromDtoToEntity(dtoParkingCSV, parkingLayoutEnumeration, parkingTypeEnumeration, parkAndRideDetection);

            parkingsImportedService.createOrUpdateParkings(parkings);

            return Response.status(200).build();

        } catch (IOException e) {
            logger.debug("Access denied for csv File: " + e.getMessage(), e);
            throw e;
        }catch (IllegalArgumentException e) {
            logger.warn("Caught exception while processing data in the cvs file: " + e.getMessage(), e);
            throw e;
        }
    }

    @GET
    @Path("/parking_import_list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getParkingImportList() {
        List<JobType> poiTypes = Arrays.asList(JobType.NETEX_PARKING, JobType.CSV_PARKING, JobType.CSV_BIKE_PARKING,JobType.CSV_RENTAL_BIKE_PARKING);
        try {
            List<Job> foundJobs = jobRepository.findByTypesAndAction(poiTypes, JobAction.IMPORT);
            return Response.ok(foundJobs).build();
        }catch(Exception e){
            logger.error("Error while getting poi import list", e);

        }

        return Response.status(500).build();
    }



    @POST
    @Path("/parking_async_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAsyncParkingsCsvFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user,
                                          @FormDataParam("parking_type") String parkingTypeParam, @FormDataParam("parking_layout") String parkingLayoutParam,
                                          @FormDataParam("park_and_ride_detection") Boolean parkAndRideDetection) throws IOException {

        logger.info("Import Parkings par " + user + " du fichier " + fileName);

        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.CSV_PARKING);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);

        ImportJobWorker importJobWorker = new ImportJobWorker(job, inputStream, jobRepository);
        importJobWorker.setParkingLayoutParam(parkingLayoutParam);
        importJobWorker.setParkingTypeParam(parkingTypeParam);
        importJobWorker.setParkAndRideDetection(parkAndRideDetection);
        importJobWorker.setParkingsImportedService(parkingsImportedService);
        importService.submit(importJobWorker);

        return Response.status(200).build();
    }
}
