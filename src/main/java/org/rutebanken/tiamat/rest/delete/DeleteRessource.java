package org.rutebanken.tiamat.rest.delete;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.changelog.LoggingService;
import org.rutebanken.tiamat.service.delete.DeleteService;
import org.springframework.stereotype.Component;


@Component
@Path("/deleteall")
public class DeleteRessource {

    private final DeleteService deleteService;
    private final UsernameFetcher usernameFetcher;
    private final LoggingService loggingService;

    public DeleteRessource(DeleteService deleteService, UsernameFetcher usernameFetcher, LoggingService loggingService) {
        this.deleteService = deleteService;
        this.usernameFetcher = usernameFetcher;
        this.loggingService = loggingService;
    }

    @POST
    @Path("/parkings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAllParkings() throws IllegalArgumentException {
        String username = usernameFetcher.getUserNameForAuthenticatedUser();
        loggingService.logParkingDeleteAll(username);
        deleteService.deleteAllParkings();
        return Response.status(200).build();
    }

    @POST
    @Path("/poi")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAllPoi() throws IllegalArgumentException {
        String username = usernameFetcher.getUserNameForAuthenticatedUser();
        loggingService.logPOIDeleteAll(username);
        deleteService.deleteAllPoi();
        return Response.status(200).build();
    }
}
