package org.rutebanken.tiamat.rest.stopPlacesNetex;

import org.entur.gbfs.http.GBFSHttpClient;
import org.entur.gbfs.mapper.GBFSMapper;
import org.entur.gbfs.validation.GbfsValidator;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.PostgresTestContainer;
import org.rutebanken.tiamat.TiamatTestApplication;
import org.rutebanken.tiamat.feign.mdm.MdmFeignClient;
import org.rutebanken.tiamat.security.RolesChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TiamatTestApplication.class)
@AutoConfigureWebTestClient
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EndpointTest {

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainer::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainer.INSTANCE::getPassword);
    }

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RolesChecker rolesChecker;

    @MockitoBean
    public GBFSMapper gbfsMapper;

    @MockitoBean
    public GbfsValidator gbfsValidator;

    @MockitoBean
    public GBFSHttpClient gbfsHttpClient;

    @MockitoBean
    public MdmFeignClient mdmFeignClient;


    @Test
    public void testDownloadFileEndpointWithCorrectName() throws Exception {

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user123")
                .claim("scope", "read")
                .claim("scope", "downloadNetexStopPlaceAdmin")
                .build();

        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        doReturn(true).when(rolesChecker).hasRole("downloadNetexStopPlaceAdmin");


        // first case with correct name (test.zip). Status should be 500. It means the method has been called but it failed because of authentication
        webTestClient.get()
                .uri("/services/stop_places/netex/export/stop-place-file-download/technique/test.zip")
                .header("Authorization", "Bearer un-token-bidon")
                .exchange()
                .expectStatus().is5xxServerError();


    }

    @Test
    public void testDownloadFileEndpointWithIncorrectName() {

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user123")
                .claim("scope", "read")
                .claim("scope", "downloadNetexStopPlaceAdmin")
                .build();

        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        doReturn(true).when(rolesChecker).hasRole("downloadNetexStopPlaceAdmin");


        // second case with empty name. Status should be 404. It means the method has  NOT been called because file pattern is not correct
        webTestClient.get()
                .uri("/services/stop_places/netex/export/stop-place-file-download/technique/")
                .header("Authorization", "Bearer un-token-bidon")
                .exchange()
                .expectStatus().is4xxClientError();





    }


}