package org.rutebanken.tiamat.rest.parkingsNetex;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.helper.organisation.NotAuthenticatedException;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.importer.ParkingsImporter;
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
@Path("/parkings_netex_import_xml")
public class ImportParkingsNetexResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportParkingsNetexResource.class);

    private final PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;
    private final ParkingsImporter parkingsImporter;



    @Autowired
    ImportParkingsNetexResource(PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller,
                                ParkingsImporter parkingsImporter){
        this.publicationDeliveryUnmarshaller=publicationDeliveryUnmarshaller;
        this.parkingsImporter =parkingsImporter;
    }

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importParkingsNetexFile(@FormDataParam("file") InputStream inputStream,
                                          @FormDataParam("file_name") String fileName,
                                          @FormDataParam("user") String user,
                                          @FormDataParam("provider") String provider)
            throws IOException, IllegalArgumentException, JAXBException, SAXException {
        logger.info("Received Netex publication delivery, starting to parse...");
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        try {
            Response.ResponseBuilder builder = parkingsImporter.importParkings(incomingPublicationDelivery, null, provider, fileName);
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
