package org.rutebanken.tiamat.rest.poiNetex;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.general.PointOfInterestCSVHelper;
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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
@Path("netex_poi")
public class ImportPointOfInterestsNetexResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportPointOfInterestsNetexResource.class);
    private final PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;
    private final NetexImporter netexImporter;

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());



    @Autowired
    PointOfInterestCSVHelper poiHelper;

    @Autowired
    JobRepository jobRepository;

    public ImportPointOfInterestsNetexResource(PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller,
                                               NetexImporter netexImporter) {
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.netexImporter = netexImporter;
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
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        poiHelper.clearClassificationCache();
        try {
           // Response.ResponseBuilder builder = netexImporter.importProcess(incomingPublicationDelivery, provider, fileName, folder, false, JobType.NETEX_POI);
            return null;
        } catch(Exception e){
            logger.error(e.getMessage(),e);
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
            throws IOException, IllegalArgumentException, JAXBException, SAXException {
        logger.info("Lancement de l'import POI netex pour le fichier: " + fileName);

        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.NETEX_POI);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        job.setSubFolder(folder);
        job = jobRepository.save(job);
        Response.ResponseBuilder builder = Response.accepted();

        ImportJobWorker importJobWorker = new ImportJobWorker(job, publicationDeliveryUnmarshaller, netexImporter, provider,jobRepository ,inputStream, poiHelper);
        importService.submit(importJobWorker);

        if (provider != null) {
            return builder.location(URI.create(String.format("/services/stop_places/jobs/%s/scheduled_jobs/%d", folder, job.getId()))).build();
        } else {
            return builder.build();
        }
    }
}
