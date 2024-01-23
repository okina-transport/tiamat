package org.rutebanken.tiamat.rest.postcode;


import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.service.batch.PoiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Path("postcode")
public class PostcodeResource {

    @Autowired
    private PoiService poiService;
    @POST
    @Path("/get_missing_postcode/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMissingPostcode(@PathParam("type") String type) throws IllegalArgumentException {
        if(StringUtils.equalsIgnoreCase("poi", type)){
            poiService.getMissingPostCodePoi();
        }

        return Response.status(200).build();
    }


}
