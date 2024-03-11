package org.rutebanken.tiamat.rest.accessibility;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.general.AccessibilityCSVHelperQuay;
import org.rutebanken.tiamat.general.AccessibilityCSVHelperStopPlace;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.rest.dto.DtoAccessibilityQuay;
import org.rutebanken.tiamat.rest.dto.DtoAccessibilityStopPlace;
import org.rutebanken.tiamat.service.accessibility.AccessibilityImportedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;


@Component
@Path("/accessibility_import_csv")
public class ImportAccessibilityResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportAccessibilityResource.class);

    private final AccessibilityImportedService accessibilityImportedService;

    @Autowired
    ImportAccessibilityResource(AccessibilityImportedService accessibilityImportedService){
        this.accessibilityImportedService=accessibilityImportedService;
    }

    @POST
    @Path("/quay")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAccessibilityQuayCsvFile(@FormDataParam("file") InputStream inputStream,
                                               @FormDataParam("file_name") String fileName,
                                               @FormDataParam("user") String user) {
        logger.info("Import Accessibility Quay by : " + user + " of the file : " + fileName);
        try {
            List<DtoAccessibilityQuay> dtoAccessibilityQuay = AccessibilityCSVHelperQuay.parseDocument(inputStream);
            AccessibilityCSVHelperQuay.checkDuplicatedQuays(dtoAccessibilityQuay);
            List<Quay> quayList = AccessibilityCSVHelperQuay.mapFromDtoToEntity(dtoAccessibilityQuay);
            List<Quay> result = accessibilityImportedService.updateAccessibilityQuays(quayList);
            accessibilityImportedService.findAndUpdateAccessibilityStopPlacesToQuays(result);

            logger.info("End of updating accessibility process");
            return Response.status(Response.Status.OK).build();
        } catch (IndexOutOfBoundsException e) {
            String message = "Index error while mapping CSV file\n" + e.getMessage() + "\nThe problem may be due to the type of file chosen (verify the checked type) and/or the columns in the file.";
            logger.error(message, e);
            throw new WebApplicationException(message, Response.Status.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            String message = "Caught exception while processing data in the CSV file\n" + e.getMessage();
            logger.error(message, e);
            throw new WebApplicationException(message, Response.Status.BAD_REQUEST);
        } catch (Exception e) {
            String message = "Unexpected error during CSV file processing\n" + e.getMessage();
            logger.error(message, e);
            throw new WebApplicationException(message, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/commercial")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAccessibilityCommercialCsvFile(@FormDataParam("file") InputStream inputStream,
                                                         @FormDataParam("file_name") String fileName,
                                                         @FormDataParam("user") String user) {
        logger.info("Import Accessibility Stop Place by : " + user + " of the file : " + fileName);
        try {
            List<DtoAccessibilityStopPlace> dtoAccessibilityStopPlaces = AccessibilityCSVHelperStopPlace.parseDocument(inputStream);
            AccessibilityCSVHelperStopPlace.checkDuplicatedQuays(dtoAccessibilityStopPlaces);
            List<StopPlace> stopPlaceList = AccessibilityCSVHelperStopPlace.mapFromDtoToEntity(dtoAccessibilityStopPlaces);
            List<StopPlace> result = accessibilityImportedService.updateAccessibilityStopPlaces(stopPlaceList);
            accessibilityImportedService.findAndUpdateAccessibilityQuaysToStopPlaces(result);

            logger.info("End of updating accessibility process");
            return Response.status(Response.Status.OK).build();
        } catch (IndexOutOfBoundsException e) {
            String message = "Index error while mapping CSV file\n" + e.getMessage() + "\nThe problem may be due to the type of file chosen (verify the checked type) and/or the columns in the file.";
            logger.error(message, e);
            throw new WebApplicationException(message, Response.Status.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            String message = "Caught exception while processing data in the CSV file\n" + e.getMessage();
            logger.error(message, e);
            throw new WebApplicationException(message, Response.Status.BAD_REQUEST);
        } catch (Exception e) {
            String message = "Unexpected error during CSV file processing\n" + e.getMessage();
            logger.error(message, e);
            throw new WebApplicationException(message, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
