/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.config;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.server.ResourceConfig;
import org.rutebanken.tiamat.dtoassembling.dto.JbvCodeMappingDto;
import org.rutebanken.tiamat.rest.dto.DtoJbvCodeMappingResource;
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
@ApplicationPath("/")
public class JerseyConfig extends ResourceConfig {

    public static final String SERVICES_PATH = "/services";

    public static final String SERVICES_ADMIN_PATH = SERVICES_PATH + "/admin";

    public static final String SERVICES_STOP_PLACE_PATH = SERVICES_PATH + "/stop_places";

    public JerseyConfig() {
        register(HealthResource.class);
        register(DtoStopPlaceResource.class);
        register(DtoQuayResource.class);
        register(DtoJbvCodeMappingResource.class);
        register(ImportResource.class);
        register(RestoringImportResource.class);
        register(AsyncExportResource.class);
        register(ExportResource.class);
        register(GraphQLResource.class);
        register(GeneralExceptionMapper.class);

        configureSwagger();
    }


    private void configureSwagger() {
        // Available at localhost:port/api/swagger.json
        this.register(ApiListingResource.class);
        this.register(SwaggerSerializers.class);

        BeanConfig config = new BeanConfig();
        config.setConfigId("tiamat-swagger-doc");
        config.setTitle("Tiamat API");
        config.setVersion("v1");
        config.setSchemes(new String[]{"http", "https"});
        config.setBasePath(SERVICES_PATH);
        config.setResourcePackage("org.rutebanken.tiamat");
        config.setPrettyPrint(true);
        config.setScan(true);
    }
}
