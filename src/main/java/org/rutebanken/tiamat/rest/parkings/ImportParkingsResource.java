package org.rutebanken.tiamat.rest.parkings;

import com.google.common.io.ByteStreams;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.general.ParkingsCSVHelper;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingLayoutEnumeration;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.rest.dto.DtoParking;
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
@Path("/parkings_import_csv")
public class ImportParkingsResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportParkingsResource.class);

    private ParkingsImportedService parkingsImportedService;

    @Autowired
    ImportParkingsResource(ParkingsImportedService parkingsImportedService){
        this.parkingsImportedService=parkingsImportedService;
    }

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importParkingsCsvFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user,
                                          @FormDataParam("parking_type") String parkingTypeParam, @FormDataParam("parking_layout") String parkingLayoutParam,
                                          @FormDataParam("park_and_ride_detection") Boolean parkAndRideDetection) throws IOException, IllegalArgumentException {
        try {

            ParkingLayoutEnumeration  parkingLayoutEnumeration = ParkingLayoutEnumeration.fromValue(parkingLayoutParam);
            ParkingTypeEnumeration parkingTypeEnumeration = ParkingTypeEnumeration.fromValue(parkingTypeParam);

            logger.info("Import Parkings par " + user + " du fichier " + fileName);

            List<DtoParking> dtoParkingCSV = ParkingsCSVHelper.parseDocument(inputStream);

            ParkingsCSVHelper.checkDuplicatedParkings(dtoParkingCSV);

            List<Parking> parkings = ParkingsCSVHelper.mapFromDtoToEntity(dtoParkingCSV, parkingLayoutEnumeration, parkingTypeEnumeration, parkAndRideDetection);

            parkingsImportedService.createOrUpdateParkings(parkings);

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
