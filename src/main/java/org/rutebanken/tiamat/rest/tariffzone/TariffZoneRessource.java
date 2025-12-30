package org.rutebanken.tiamat.rest.tariffzone;


import io.swagger.v3.oas.annotations.media.Schema;
import org.rutebanken.tiamat.repository.TariffZoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Component
@Schema(description = "TariffZone resource")
@Produces("application/json")
@Path("tariff-zone")
public class TariffZoneRessource {

    @Autowired
    TariffZoneRepository tariffZoneRepository;

    @GET
    @Path("/zone-id")
    @Produces("text/plain")
    public Response getTariffZoneId(@QueryParam(value = "netexId") String valueRef){
        String retour = tariffZoneRepository.findFirstByKeyValue("fare-zone", valueRef);

        return Response.ok().entity(retour).build();
    }

}
