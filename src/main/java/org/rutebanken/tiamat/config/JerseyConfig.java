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
import org.glassfish.jersey.servlet.ServletContainer;
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
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;

@Configuration
@ApplicationPath("/")
public class JerseyConfig extends ResourceConfig {

    public static final String SERVICES_PATH = "/services";

    public static final String SERVICES_ADMIN_PATH = SERVICES_PATH + "/admin";

    public static final String SERVICES_STOP_PLACE_PATH = SERVICES_PATH + "/stop_places";

    private static final String PUBLIC_SWAGGER_SCANNER_ID = "public-scanner";
    private static final String PUBLIC_SWAGGER_CONFIG_ID = "public-swagger-doc";

    private static final String ADMIN_SWAGGER_SCANNER_ID = "admin-scanner";
    private static final String ADMIN_SWAGGER_CONFIG_ID = "admin-swagger-doc";

    @Bean
    public ServletRegistrationBean publicJersey() {

        ServletRegistrationBean publicServicesJersey = new ServletRegistrationBean(new ServletContainer(new StopPlaceServicesConfig()));
        publicServicesJersey.addUrlMappings(SERVICES_STOP_PLACE_PATH + "/*");
        publicServicesJersey.setName("PublicJersey");

        publicServicesJersey.setLoadOnStartup(0);
        publicServicesJersey.getInitParameters().put("swagger.scanner.id", PUBLIC_SWAGGER_SCANNER_ID);
        publicServicesJersey.getInitParameters().put("swagger.config.id", PUBLIC_SWAGGER_CONFIG_ID);
        return publicServicesJersey;
    }

    @Bean
    public ServletRegistrationBean adminJersey() {

        ServletRegistrationBean adminServicesJersey = new ServletRegistrationBean(new ServletContainer(new AdminServicesConfig()));
        adminServicesJersey.addUrlMappings(SERVICES_ADMIN_PATH + "/*");
        adminServicesJersey.setName("AdminJersey");

        adminServicesJersey.setLoadOnStartup(0);
        adminServicesJersey.getInitParameters().put("swagger.scanner.id", ADMIN_SWAGGER_SCANNER_ID);
        adminServicesJersey.getInitParameters().put("swagger.config.id", ADMIN_SWAGGER_CONFIG_ID);
        return adminServicesJersey;
    }

    private class StopPlaceServicesConfig extends ResourceConfig {

        public StopPlaceServicesConfig() {

            register(DtoStopPlaceResource.class);
            register(DtoQuayResource.class);
            register(ImportResource.class);
            register(AsyncExportResource.class);
            register(ExportResource.class);
            register(GraphQLResource.class);

            register(GeneralExceptionMapper.class);
            register(HealthResource.class);
            configureSwagger();
        }

        private void configureSwagger() {
            // Available at localhost:port/api/swagger.json
            this.register(ApiListingResource.class);
            this.register(SwaggerSerializers.class);

            BeanConfig config = new BeanConfig();
            config.setConfigId(PUBLIC_SWAGGER_CONFIG_ID);
            config.setTitle("Tiamat Public API");
            config.setVersion("v1");
            config.setSchemes(new String[]{"http", "https"});
            config.setBasePath(SERVICES_STOP_PLACE_PATH);
            config.setResourcePackage("org.rutebanken.tiamat");
            config.setPrettyPrint(true);
            config.setScan(true);
        }
    }

    private class AdminServicesConfig extends ResourceConfig {

        public AdminServicesConfig() {
            register(DtoJbvCodeMappingResource.class);
            register(RestoringImportResource.class);

            register(GeneralExceptionMapper.class);
            register(HealthResource.class);
            configureSwagger();
        }

        private void configureSwagger() {
            // Available at localhost:port/api/swagger.json
            this.register(ApiListingResource.class);
            this.register(SwaggerSerializers.class);

            BeanConfig config = new BeanConfig();
            config.setConfigId(ADMIN_SWAGGER_CONFIG_ID);
            config.setTitle("Tiamat Admin API");
            config.setVersion("v1");
            config.setSchemes(new String[]{"http", "https"});
            config.setBasePath(SERVICES_ADMIN_PATH);
            config.setResourcePackage("org.rutebanken.tiamat");
            config.setPrettyPrint(true);
            config.setScan(true);
        }
    }
}
