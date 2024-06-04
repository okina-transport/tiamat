package org.rutebanken.tiamat.rest.stopPlacesNetex;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.helper.organisation.NotAuthenticatedException;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.importer.ParkingsImporter;
import org.rutebanken.tiamat.importer.StopPlacesImporter;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;

@Component
@Path("/stop_places_netex_import_xml")
public class ImportStopPlacesNetexResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportStopPlacesNetexResource.class);

    private final PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;
    private final StopPlacesImporter stopPlacesImporter;



    @Autowired
    ImportStopPlacesNetexResource(PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller,
                                  StopPlacesImporter stopPlacesImporter){
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.stopPlacesImporter = stopPlacesImporter;
    }

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importStopPlacesNetexFile(@FormDataParam("file") InputStream inputStream,
                                          @FormDataParam("file_name") String fileName,
                                          @FormDataParam("provider") String provider,
                                          @FormDataParam("folder") String folder)
            throws IOException, IllegalArgumentException, JAXBException, SAXException {
        logger.info("Received Stop Places Netex publication delivery, starting to parse...");
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        try {
            Response.ResponseBuilder builder = stopPlacesImporter.importStopPlaces(incomingPublicationDelivery, null, provider, fileName, folder);
            return builder.build();
        } catch (NotAuthenticatedException | NotAuthorizedException e) {
            logger.debug("Access denied for publication delivery: " + e.getMessage(), e);
            throw e;
        } catch (RuntimeException e) {
            logger.warn("Caught exception while importing publication delivery: " + incomingPublicationDelivery, e);
            throw e;
        } catch (TiamatBusinessException e) {
            throw new RuntimeException(e);
        }
    }
}
