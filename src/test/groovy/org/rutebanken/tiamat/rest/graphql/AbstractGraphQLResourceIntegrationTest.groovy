package org.rutebanken.tiamat.rest.graphql;


import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.StopPlace;

import javax.transaction.Transactional;

import static io.restassured.RestAssured.given;


public abstract class AbstractGraphQLResourceIntegrationTest extends TiamatIntegrationTest {

    protected static final String BASE_URI_GRAPHQL = "/services/stop_places/graphql/";

    @Before
    public void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    protected ValidatableResponse executeGraphQL(String graphQlJsonQuery) {
        return executeGraphQL(graphQlJsonQuery,200);
    }

    /**
     * When sending empty parameters, specify 'query' directly.
     * Escapes quotes and newlines
     */
    protected ValidatableResponse executeGraphqQLQueryOnly(String query) {

        String graphQlJsonQuery = "{" +
                "\"query\":\"" +
                query.replaceAll("\"", "\\\\\"")
                        .replaceAll("\n", "\\\\n") +
                "\",\"variables\":\"\"}";

        return executeGraphQL(graphQlJsonQuery);
    }

    protected ValidatableResponse executeGraphQL(String graphQlJsonQuery,int httpStatusCode) {
        return given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(graphQlJsonQuery)
                .log().body()
           .when()
                .post(BASE_URI_GRAPHQL)
                .then()
                .log().body()
                .statusCode(httpStatusCode)
                .assertThat();
    }

    /*
    * Wrapping save-operation in separate method to complete transaction before GraphQL-request is called
    */
    @Transactional
    protected StopPlace saveStopPlaceTransactional(StopPlace stopPlace) {
        return stopPlaceVersionedSaverService.saveNewVersion(stopPlace);
    }
}
