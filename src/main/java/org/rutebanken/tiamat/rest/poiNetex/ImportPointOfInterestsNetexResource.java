package org.rutebanken.tiamat.rest.poiNetex;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.general.PointOfInterestCSVHelper;
import org.rutebanken.tiamat.importer.PoisImporter;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

@Component
@Path("poi_netex_import_xml")
public class ImportPointOfInterestsNetexResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportPointOfInterestsNetexResource.class);
    private final PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;
    private final PoisImporter poisImporter;


    @Autowired
    PointOfInterestCSVHelper poiHelper;

    public ImportPointOfInterestsNetexResource(PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller,
                                               PoisImporter poisImporter) {
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.poisImporter = poisImporter;
    }

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importPOINetexFile(@FormDataParam("file") InputStream inputStream,
                                       @FormDataParam("file_name") String fileName,
                                       @FormDataParam("user") String user,
                                       @FormDataParam("provider") String provider)
            throws IOException, IllegalArgumentException, JAXBException, SAXException {
        logger.info("Received Netex publication delivery, starting to parse...");
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        poiHelper.clearClassificationCache();
        try {
            Response.ResponseBuilder builder = poisImporter.importPointOfInterests(incomingPublicationDelivery, null, provider, fileName);
            return builder.build();
        } catch(Exception e){
            logger.error(e.getMessage(),e);
        }
        logger.info("Import point of interest par " + user + " du fichier " + fileName + " terminé");
        return Response.status(200).build();
    }
}
