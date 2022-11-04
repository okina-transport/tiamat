package org.rutebanken.tiamat.rest.parkings;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.general.BikesCSVHelper;
import org.rutebanken.tiamat.model.Parking;
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
import java.util.List;


@Component
@Path("/parkings_bike_import_csv")
public class ImportBikeParkingsResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportBikeParkingsResource.class);

    private BikeParkingsImportedService bikeParkingsImportedService;

    @Autowired
    ImportBikeParkingsResource(BikeParkingsImportedService bikeParkingsImportedService){
        this.bikeParkingsImportedService = bikeParkingsImportedService;
    }

    @POST
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
}
