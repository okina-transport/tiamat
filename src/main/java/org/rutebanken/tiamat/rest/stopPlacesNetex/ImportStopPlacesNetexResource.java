package org.rutebanken.tiamat.rest.stopPlacesNetex;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.helper.organisation.NotAuthenticatedException;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.importer.NetexImporter;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Path("/netex_stops")
public class ImportStopPlacesNetexResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportStopPlacesNetexResource.class);

    private final PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;
    private final NetexImporter netexImporter;

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    @Autowired
    JobRepository jobRepository;

    @Autowired
    ImportStopPlacesNetexResource(PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller,
                                  NetexImporter netexImporter){
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.netexImporter = netexImporter;
    }

    @PreAuthorize("@rolesChecker.hasRoleEdit()")
    @POST
    @Path("/stops_import_netex")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importStopPlacesNetexFile(@FormDataParam("file") InputStream inputStream,
                                          @FormDataParam("file_name") String fileName,
                                          @FormDataParam("provider") String provider,
                                          @FormDataParam("folder") String folder,
                                          @FormDataParam("containsMobiitiIds") Boolean containsMobiitiIds)
            throws IOException, IllegalArgumentException, JAXBException, SAXException {

        logger.info("Received Stop Places Netex publication delivery, starting to parse...");
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        try {
           netexImporter.importProcess(incomingPublicationDelivery,  containsMobiitiIds);
           return null;
        } catch (NotAuthenticatedException | NotAuthorizedException e) {
            logger.debug("Access denied for publication delivery: " + e.getMessage(), e);
            throw e;
        } catch (RuntimeException e) {
            logger.warn("Caught exception while importing publication delivery: " + incomingPublicationDelivery, e);
            throw e;
        }

    }

    @PreAuthorize("@rolesChecker.hasRoleEdit()")
    @POST
    @Path("/stops_async_import_netex")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAsyncStopPlacesNetexFile(@FormDataParam("file") InputStream inputStream,
                                              @FormDataParam("file_name") String fileName,
                                              @FormDataParam("provider") String provider,
                                              @FormDataParam("folder") String folder,
                                              @FormDataParam("containsMobiitiIds") Boolean containsMobiitiIds) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Response.ResponseBuilder builder = Response.accepted();

        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.NETEX_STOP_PLACE_QUAY);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        job.setSubFolder(folder);
        jobRepository.save(job);
        logger.info("Import stop place netex: {}", fileName);
        ImportJobWorker importJobWorker = new ImportJobWorker(job, publicationDeliveryUnmarshaller, inputStream, containsMobiitiIds, jobRepository, netexImporter, provider, authentication);
        importService.submit(importJobWorker);



        if (provider != null) {
            return builder.location(URI.create(String.format("/services/stop_places/jobs/%s/scheduled_jobs/%d", folder, job.getId()))).build();
        } else {
            return builder.build();
        }
    }

    @GET
    @Path("/stop_place_import_list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStopPlaceImportList() {
        List<JobType> poiTypes = List.of(JobType.NETEX_STOP_PLACE_QUAY);
        try {
            List<Job> foundJobs = jobRepository.findByTypesAndAction(poiTypes, JobAction.IMPORT);
            return Response.ok(foundJobs).build();
        }catch(Exception e){
            logger.error("Error while getting poi import list", e);

        }

        return Response.status(500).build();
    }
}
