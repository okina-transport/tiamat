package org.rutebanken.tiamat.rest.parkingsNetex;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Path("/netex_parking")
public class ImportParkingsNetexResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportParkingsNetexResource.class);

    private final PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;
    private final NetexImporter netexImporter;

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    @Autowired
    JobRepository jobRepository;

    @Autowired
    ImportParkingsNetexResource(PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller,
                                NetexImporter netexImporter){
        this.publicationDeliveryUnmarshaller=publicationDeliveryUnmarshaller;
        this.netexImporter =netexImporter;
    }

    @PreAuthorize("@rolesChecker.hasRoleEdit()")
    @POST
    @Path("parking_import_netex")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importParkingsNetexFile(@FormDataParam("file") InputStream inputStream,
                                          @FormDataParam("file_name") String fileName,
                                          @FormDataParam("provider") String provider,
                                          @FormDataParam("folder") String folder)
            throws IOException, IllegalArgumentException, JAXBException, SAXException {
        logger.info("Received Parking Netex publication delivery, starting to parse...");
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        try {
         //   Response.ResponseBuilder builder = netexImporter.importProcess(incomingPublicationDelivery, provider, fileName, folder, false, JobType.NETEX_PARKING);
          //  return builder.build();
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
    @Path("parking_async_import_netex")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAsyncParkingsNetexFile(@FormDataParam("file") InputStream inputStream,
                                            @FormDataParam("file_name") String fileName,
                                            @FormDataParam("provider") String provider,
                                            @FormDataParam("folder") String folder) throws IOException {
        logger.info("Received Parking Netex publication delivery, starting to parse...");

        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.NETEX_PARKING);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        job.setSubFolder(folder);
        jobRepository.save(job);

        ImportJobWorker importJobWorker = new ImportJobWorker(job,publicationDeliveryUnmarshaller, netexImporter, inputStream, jobRepository);
        importService.submit(importJobWorker);

        Response.ResponseBuilder builder = Response.accepted();

        if (provider != null) {
            return builder.location(URI.create(String.format("/services/stop_places/jobs/%s/scheduled_jobs/%d", folder, job.getId()))).build();
        } else {
            return builder.build();
        }

    }
}
