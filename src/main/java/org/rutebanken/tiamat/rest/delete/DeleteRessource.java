package org.rutebanken.tiamat.rest.delete;

import org.rutebanken.tiamat.service.delete.DeleteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Component
@Path("/deleteall")
public class DeleteRessource {

    @Autowired
    private DeleteService deleteService;

    private static final Logger logger = LoggerFactory.getLogger(DeleteRessource.class);

    @POST
    @Path("/parkings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAllParkings() throws IllegalArgumentException {
        deleteService.deleteAllParkings();
        return Response.status(200).build();
    }

    @POST
    @Path("/poi")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAllPoi() throws IllegalArgumentException {
        deleteService.deleteAllPoi();
        return Response.status(200).build();
    }
}
