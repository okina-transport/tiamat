package org.rutebanken.tiamat.rest.stopPlacesNetex;

import io.restassured.http.ContentType;
import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class EndpointTest extends TiamatIntegrationTest {

    @Test
    public void testDownloadFileEndpointWithCorrectName() {


        // first case with correct name (test.zip). Status should be 500. It means the method has been called but it failed because of authentication
        given()
                .port(port)
                .contentType(ContentType.JSON)
                .when()
                .get("/services/stop_places/netex/export/stop-place-file-download/technique/test.zip")
                .then()
                .log().body()
                .statusCode(500)
                .assertThat();

    }

    @Test
    public void testDownloadFileEndpointWithIncorrectName() {


        // second case with empty name. Status should be 404. It means the method has  NOT been called because file pattern is not correct
        given()
                .port(port)
                .contentType(ContentType.JSON)
                .when()
                .get("/services/stop_places/netex/export/stop-place-file-download/technique/")
                .then()
                .log().body()
                .statusCode(404)
                .assertThat();

    }


}