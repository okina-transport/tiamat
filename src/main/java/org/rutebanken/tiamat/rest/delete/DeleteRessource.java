package org.rutebanken.tiamat.rest.delete;

import org.rutebanken.tiamat.service.delete.DeleteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Component
@Path("/deleteall")
public class DeleteRessource {

    @Autowired
    private DeleteService deleteService;

    private static final Logger logger = LoggerFactory.getLogger(DeleteRessource.class);

    @POST
    @Path("/parkings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAllParkings() throws IOException, IllegalArgumentException {

        deleteService.deleteAllParkings();
        logger.info("Suppression parkings réussi");
        return Response.status(200).build();
    }

    @POST
    @Path("/poi")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAllPoi() throws IOException, IllegalArgumentException {

        deleteService.deleteAllPoi();
        logger.info("Suppression poi réussi");
        return Response.status(200).build();
    }
}
