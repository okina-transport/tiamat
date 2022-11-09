package org.rutebanken.tiamat.rest.parkings;


import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.general.BikesCSVHelper;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.rest.dto.DtoBikeParking;
import org.rutebanken.tiamat.service.parking.RentalBikeParkingsImportedService;
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
@Path("/rental_bike_import")
public class ImportRentalBikeResource {

    @Autowired
    private RentalBikeParkingsImportedService rentalBikeparkingsImportedService;

    private static final Logger logger = LoggerFactory.getLogger(ImportRentalBikeResource.class);

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importRentalBikesFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {

        logger.info("Import VLS par " + user + " du fichier " + fileName);


        List<DtoBikeParking> dtoParkingCSV = BikesCSVHelper.parseDocument(inputStream);
        BikesCSVHelper.checkDuplicatedBikeParkings(dtoParkingCSV);


        List<Parking> parkings = BikesCSVHelper.mapFromDtoToEntityParking(dtoParkingCSV, true);
        rentalBikeparkingsImportedService.createOrUpdateParkings(parkings);


        return Response.status(200).build();
    }



}
