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

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.rutebanken.tiamat.filter.LoggingFilter;
import org.rutebanken.tiamat.jersey.JerseyJava8TimeConverterProvider;
import org.rutebanken.tiamat.rest.accessibility.ImportAccessibilityResource;
import org.rutebanken.tiamat.rest.delete.DeleteRessource;
import org.rutebanken.tiamat.rest.dto.DtoJbvCodeMappingResource;
import org.rutebanken.tiamat.rest.dto.DtoQuayResource;
import org.rutebanken.tiamat.rest.dto.DtoStopPlaceResource;
import org.rutebanken.tiamat.rest.merge.MergeableStopPlacesResource;
import org.rutebanken.tiamat.rest.exception.*;
import org.rutebanken.tiamat.rest.graphql.GraphQLResource;
import org.rutebanken.tiamat.rest.health.HealthResource;
import org.rutebanken.tiamat.rest.inseecode.InseeCodeResource;
import org.rutebanken.tiamat.rest.jobs.JobsResources;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.AsyncExportResource;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.ExportResource;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.ImportResource;
import org.rutebanken.tiamat.rest.parkings.GbfsImportResource;
import org.rutebanken.tiamat.rest.parkings.ImportBikeParkingsResource;
import org.rutebanken.tiamat.rest.parkings.ImportParkingsResource;
import org.rutebanken.tiamat.rest.parkings.ImportRentalBikeResource;
import org.rutebanken.tiamat.rest.parkingsNetex.ImportParkingsNetexResource;
import org.rutebanken.tiamat.rest.poi.ImportPOIResource;
import org.rutebanken.tiamat.rest.poiNetex.ImportPointOfInterestsNetexResource;
import org.rutebanken.tiamat.rest.stopPlacesNetex.ImportStopPlacesNetexResource;
import org.rutebanken.tiamat.rest.tad.ImportTADRessource;
import org.rutebanken.tiamat.rest.tariffzone.TariffZoneRessource;
import org.springdoc.webmvc.api.OpenApiResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class JerseyConfig {

    /**
     * Client ID header.
     * Is used for identifiying clients calling our API
     */
    public static final String ET_CLIENT_ID_HEADER = "ET-Client-ID";

    /**
     * Client Name header.
     * Is used for getting the name of clients calling our API.
     */
    public static final String ET_CLIENT_NAME_HEADER = "ET-Client-Name";

    public static final String SERVICES_PATH = "/services";

    public static final String SERVICES_ADMIN_PATH = SERVICES_PATH + "/admin";

    public static final String SERVICES_STOP_PLACE_PATH = SERVICES_PATH + "/stop_places";

    public static final String SERVICES_HEALTH_PATH = "/health";
    public static final String SWAGGER_SCANNER_ID = "swagger.scanner.id";
    public static final String SWAGGER_CONFIG_ID = "swagger.config.id";
    private static final String PUBLIC_SWAGGER_SCANNER_ID = "public-scanner";
    private static final String PUBLIC_SWAGGER_CONFIG_ID = "public-swagger-doc";
    private static final String ADMIN_SWAGGER_SCANNER_ID = "admin-scanner";
    private static final String ADMIN_SWAGGER_CONFIG_ID = "admin-swagger-doc";
    private static final String HEALTH_SWAGGER_SCANNER_ID = "health-scanner";
    private static final String HEALTH_SWAGGER_CONFIG_ID = "health-swagger-doc";

    private static void registerExceptionMappers(ResourceConfig resourceConfig) {
        resourceConfig.register(JsonMappingExceptionMapper.class);
        resourceConfig.register(JsonParseExceptionMapper.class);
        resourceConfig.register(JAXBExceptionMapper.class);
        resourceConfig.register(TiamatBusinessExceptionMapper.class);
        resourceConfig.register(GeneralExceptionMapper.class);
        resourceConfig.register(ErrorResponseEntityMessageBodyWriter.class);
    }

    @Bean
    public ServletRegistrationBean publicJersey() {

        Set<Class<?>> publicResources = new HashSet<>();
        publicResources.add(DtoStopPlaceResource.class);
        publicResources.add(DtoQuayResource.class);
        publicResources.add(ImportResource.class);
        publicResources.add(AsyncExportResource.class);
        publicResources.add(ExportResource.class);
        publicResources.add(ImportAccessibilityResource.class);
        publicResources.add(ImportParkingsResource.class);
        publicResources.add(ImportBikeParkingsResource.class);
        publicResources.add(ImportPOIResource.class);
        publicResources.add(ImportTADRessource.class);
        publicResources.add(ImportRentalBikeResource.class);
        publicResources.add(GraphQLResource.class);
        publicResources.add(DeleteRessource.class);
        publicResources.add(TariffZoneRessource.class);
        publicResources.add(InseeCodeResource.class);
        publicResources.add(ImportParkingsNetexResource.class);
        publicResources.add(JobsResources.class);
        publicResources.add(ImportPointOfInterestsNetexResource.class);
        publicResources.add(ImportStopPlacesNetexResource.class);
        publicResources.add(GbfsImportResource.class);
        publicResources.add(MergeableStopPlacesResource.class);
        publicResources.add(OpenApiResource.class);

        ResourceConfig resourceConfig = new ResourceConfig(publicResources);
        resourceConfig.register(JerseyJava8TimeConverterProvider.class);
        resourceConfig.register(MultiPartFeature.class);
        registerExceptionMappers(resourceConfig);

        ServletRegistrationBean publicServicesJersey = new ServletRegistrationBean(new ServletContainer(resourceConfig));

        publicServicesJersey.addUrlMappings(SERVICES_STOP_PLACE_PATH + "/*");
        publicServicesJersey.setName("PublicJersey");

        publicServicesJersey.setLoadOnStartup(0);
        publicServicesJersey.getInitParameters().put(SWAGGER_SCANNER_ID, PUBLIC_SWAGGER_SCANNER_ID);
        publicServicesJersey.getInitParameters().put(SWAGGER_CONFIG_ID, PUBLIC_SWAGGER_CONFIG_ID);

        return publicServicesJersey;
    }

    @Bean
    public ServletRegistrationBean healthJersey() {

        Set<Class<?>> resources = new HashSet<>();

        resources.add(HealthResource.class);
        resources.add(OpenApiResource.class);

        ResourceConfig resourceConfig = new ResourceConfig(resources);
        ServletRegistrationBean healthServicesJersey = new ServletRegistrationBean(new ServletContainer(resourceConfig));
        resourceConfig.register(MultiPartFeature.class);
        registerExceptionMappers(resourceConfig);

        healthServicesJersey.addUrlMappings(SERVICES_HEALTH_PATH + "/*");
        healthServicesJersey.setName("HealthJersey");

        healthServicesJersey.getInitParameters().put(SWAGGER_SCANNER_ID, HEALTH_SWAGGER_SCANNER_ID);
        healthServicesJersey.getInitParameters().put(SWAGGER_CONFIG_ID, HEALTH_SWAGGER_CONFIG_ID);
        healthServicesJersey.setLoadOnStartup(0);
        return healthServicesJersey;
    }

    @Bean
    public ServletRegistrationBean adminJersey() {

        Set<Class<?>> adminResources = new HashSet<>();
        adminResources.add(DtoJbvCodeMappingResource.class);
        adminResources.add(OpenApiResource.class);

        ResourceConfig resourceConfig = new ResourceConfig(adminResources);
        resourceConfig.register(MultiPartFeature.class);
        registerExceptionMappers(resourceConfig);

        ServletRegistrationBean adminServicesJersey = new ServletRegistrationBean(new ServletContainer(resourceConfig));


        adminServicesJersey.addUrlMappings(SERVICES_ADMIN_PATH + "/*");
        adminServicesJersey.setName("AdminJersey");

        adminServicesJersey.setLoadOnStartup(0);
        adminServicesJersey.getInitParameters().put(SWAGGER_SCANNER_ID, ADMIN_SWAGGER_SCANNER_ID);
        adminServicesJersey.getInitParameters().put(SWAGGER_CONFIG_ID, ADMIN_SWAGGER_CONFIG_ID);
        return adminServicesJersey;
    }

    @Bean
    public FilterRegistrationBean filterRegistrationBean(@Autowired LoggingFilter loggingFilter) {

        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(loggingFilter);
        registration.addUrlPatterns("/*");
        registration.setName("loggingFilter");
        registration.setOrder(1);
        return registration;
    }


}
