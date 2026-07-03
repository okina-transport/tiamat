package org.rutebanken.tiamat.rest.poiNetex;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.changelog.LoggingService;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.general.ImportJobWorkerBuilder;
import org.rutebanken.tiamat.general.PointOfInterestCSVHelper;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Path("netex_poi")
public class ImportPointOfInterestsNetexResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportPointOfInterestsNetexResource.class);
    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());
    private final PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;
    private final ImportJobWorkerBuilder importJobWorkerBuilder;
    private final PointOfInterestCSVHelper poiHelper;
    private final JobRepository jobRepository;
    private final LoggingService loggingService;
    private final UsernameFetcher usernameFetcher;


    public ImportPointOfInterestsNetexResource(PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller, ImportJobWorkerBuilder importJobWorkerBuilder, PointOfInterestCSVHelper poiHelper, JobRepository jobRepository, LoggingService loggingService, UsernameFetcher usernameFetcher) {
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.importJobWorkerBuilder = importJobWorkerBuilder;
        this.poiHelper = poiHelper;
        this.jobRepository = jobRepository;
        this.loggingService = loggingService;
        this.usernameFetcher = usernameFetcher;
    }

    @PreAuthorize("@rolesChecker.hasRoleEdit()")
    @POST
    @Path("/poi_import_netex")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importPOINetexFile(@FormDataParam("file") InputStream inputStream,
                                       @FormDataParam("file_name") String fileName,
                                       @FormDataParam("provider") String provider,
                                       @FormDataParam("folder") String folder)
            throws IOException, IllegalArgumentException, JAXBException, SAXException {
        logger.info("Received POI Netex publication delivery, starting to parse...");
        loggingService.logPoiNetexImport(usernameFetcher.getUserNameForAuthenticatedUser(), fileName);
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        poiHelper.clearClassificationCache();
        try {
            // Response.ResponseBuilder builder = netexImporter.importProcess(incomingPublicationDelivery, provider, fileName, folder, false, JobType.NETEX_POI);
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("Import point of interest par " + provider + " du fichier " + fileName + " terminé");
        return Response.status(200).build();
    }


    @PreAuthorize("@rolesChecker.hasRoleEdit()")
    @POST
    @Path("/poi_async_import_netex")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAsyncPOINetexFile(@FormDataParam("file") InputStream inputStream,
                                            @FormDataParam("file_name") String fileName,
                                            @FormDataParam("provider") String provider,
                                            @FormDataParam("folder") String folder)
            throws IOException, IllegalArgumentException {
        logger.info("Lancement de l'import POI netex pour le fichier: " + fileName);
        loggingService.logPoiNetexImport(usernameFetcher.getUserNameForAuthenticatedUser(), fileName);

        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.NETEX_POI);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        job.setSubFolder(folder);
        job = jobRepository.save(job);
        Response.ResponseBuilder builder = Response.accepted();

        ImportJobWorker importJobWorker = importJobWorkerBuilder
                .init(job)
                .withInputStream(inputStream)
                .build();
        importService.submit(importJobWorker);

        if (provider != null) {
            return builder.location(URI.create(String.format("/services/stop_places/jobs/%s/scheduled_jobs/%d", folder, job.getId()))).build();
        } else {
            return builder.build();
        }
    }
}
