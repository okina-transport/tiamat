package org.rutebanken.tiamat.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.rutebanken.tiamat.rest.coordinates.CoordinatesFixerResource;
import org.rutebanken.tiamat.rest.dto.DtoQuayResource;
import org.rutebanken.tiamat.rest.dto.DtoStopPlaceResource;
import org.rutebanken.tiamat.rest.exception.GeneralExceptionMapper;
import org.rutebanken.tiamat.rest.graphql.GraphQLResource;
import org.rutebanken.tiamat.rest.health.HealthResource;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.AsyncExportResource;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.ExportResource;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.ImportResource;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.RestoringImportResource;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;

@Configuration
@ApplicationPath("/services")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        register(HealthResource.class);
        register(CoordinatesFixerResource.class);
        register(DtoStopPlaceResource.class);
        register(DtoQuayResource.class);
        register(ImportResource.class);
        register(RestoringImportResource.class);
        register(AsyncExportResource.class);
        register(ExportResource.class);
        register(GraphQLResource.class);
        register(GeneralExceptionMapper.class);
    }
}
