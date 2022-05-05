package org.rutebanken.tiamat.rest.parkings;


import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.general.ParkingsCSVHelper;
import org.rutebanken.tiamat.general.PointOfInterestCSVHelper;
import org.rutebanken.tiamat.general.RentalBikesHelper;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.rest.dto.DtoParking;
import org.rutebanken.tiamat.rest.dto.DtoPointOfInterest;
import org.rutebanken.tiamat.rest.exception.ErrorResponseEntity;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.service.parking.ParkingsImportedService;
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
    private ParkingsImportedService parkingsImportedService;

    private static final Logger logger = LoggerFactory.getLogger(ImportRentalBikeResource.class);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importRentalBikesFile(@FormDataParam("file") InputStream inputStream) throws IOException, IllegalArgumentException {

            parkingsImportedService.clearAllRentalBikes();

            List<DtoParking> dtoParkingCSV = RentalBikesHelper.parseDocument(inputStream);
            ParkingsCSVHelper.checkDuplicatedParkings(dtoParkingCSV);


            List<Parking> parkings = RentalBikesHelper.mapFromDtoToEntity(dtoParkingCSV);
            parkingsImportedService.createOrUpdateParkings(parkings);



        return Response.status(200).build();
    }



}