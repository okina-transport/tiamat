package org.rutebanken.tiamat.rest.postcode;


import org.rutebanken.tiamat.service.batch.MissingPostCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Path("/get_missing_postcode")
public class PostcodeResource {

    @Autowired
    private MissingPostCodeService missingPostCodeService;

    @POST
    @PreAuthorize("@rolesChecker.hasRoleEdit()")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMissingPostcode() throws IllegalArgumentException {
        missingPostCodeService.getMissingPostCode();
        return Response.status(200).build();
    }
}
