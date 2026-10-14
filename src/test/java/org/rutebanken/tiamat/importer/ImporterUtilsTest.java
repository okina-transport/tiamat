package org.rutebanken.tiamat.importer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class ImporterUtilsTest {

    private static final double LON = -1.0512559;
    private static final double LAT = 43.72042;

    private static final String OKINA_URL = "https://api.adresse.okina.fr/reverse?lon=-1.0512559&lat=43.72042";
    private static final String DATA_GOUV_URL = "https://api-adresse.data.gouv.fr/reverse/?lat=43.72042&lon=-1.0512559";
    private static final String GEO_API_GOUV_URL = "https://geo.api.gouv.fr/communes?lat=43.72042&lon=-1.0512559&fields=nom,code,codesPostaux&format=json";

    private static final String FULL_ADDRESS_RESPONSE = "{\"features\":[{\"properties\":{\"citycode\":\"40088\",\"postcode\":\"40100\",\"name\":\"1 Rue du Test\",\"city\":\"Dax\"}}]}";
    private static final String NO_CITYCODE_RESPONSE = "{\"features\":[{\"properties\":{\"city\":\"Dax\"}}]}";
    private static final String GEO_API_RESPONSE = "[{\"nom\":\"Dax\",\"code\":\"40088\",\"codesPostaux\":[\"40100\"]}]";

    private static final double PARIS_LON = 2.3522;
    private static final double PARIS_LAT = 48.8566;
    private static final String PARIS_OKINA_URL = "https://api.adresse.okina.fr/reverse?lon=2.3522&lat=48.8566";
    private static final String PARIS_DATA_GOUV_URL = "https://api-adresse.data.gouv.fr/reverse/?lat=48.8566&lon=2.3522";
    private static final String PARIS_GEO_API_GOUV_URL = "https://geo.api.gouv.fr/communes?lat=48.8566&lon=2.3522&fields=nom,code,codesPostaux&format=json";
    private static final String PARIS_GEO_API_GOUV_ARRONDISSEMENT_URL = PARIS_GEO_API_GOUV_URL + "&type=arrondissement-municipal";
    private static final String PARIS_GEO_API_RESPONSE = "[{\"nom\":\"Paris\",\"code\":\"75056\",\"codesPostaux\":[\"75001\",\"75002\",\"75003\",\"75004\"]}]";
    private static final String PARIS_GEO_API_ARRONDISSEMENT_RESPONSE = "[{\"nom\":\"Paris 4e Arrondissement\",\"code\":\"75104\",\"codesPostaux\":[\"75004\"]}]";

    private ClientHttpRequestFactory originalRequestFactory;
    private MockRestServiceServer server;

    @BeforeEach
    public void setUp() {
        originalRequestFactory = ImporterUtils.restTemplate.getRequestFactory();
        server = MockRestServiceServer.bindTo(ImporterUtils.restTemplate).build();
    }

    @AfterEach
    public void tearDown() {
        ImporterUtils.restTemplate.setRequestFactory(originalRequestFactory);
    }

    @Test
    public void geocodeCombinesDataGouvAndGeoApiGouvWhenOkinaFails() {
        server.expect(requestTo(OKINA_URL)).andRespond(withServerError());
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withSuccess(
                "{\"features\":[{\"properties\":{\"citycode\":\"40088\",\"name\":\"1 Rue du Test\",\"city\":\"Dax\"}}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(GEO_API_GOUV_URL)).andRespond(withSuccess(GEO_API_RESPONSE, MediaType.APPLICATION_JSON));

        DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(LON, LAT);

        server.verify();
        assertThat(dtoGeocode.getCityCode()).isEqualTo("40088");
        assertThat(dtoGeocode.getPostCode()).isEqualTo("40100");
        assertThat(dtoGeocode.getCity()).isEqualTo("Dax");
    }

    @Test
    public void geocodeAcceptsCorsicanCityCode() {
        server.expect(requestTo(OKINA_URL)).andRespond(withSuccess(
                "{\"features\":[{\"properties\":{\"citycode\":\"2A004\",\"postcode\":\"20000\",\"name\":\"1 Cours Napoléon\",\"city\":\"Ajaccio\"}}]}", MediaType.APPLICATION_JSON));

        DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(LON, LAT);

        server.verify();
        assertThat(dtoGeocode.getCityCode()).isEqualTo("2A004");
    }

    @Test
    public void geocodeOnlyCallsOkinaWhenItReturnsEverything() {
        server.expect(requestTo(OKINA_URL)).andRespond(withSuccess(FULL_ADDRESS_RESPONSE, MediaType.APPLICATION_JSON));

        DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(LON, LAT);

        server.verify();
        assertThat(dtoGeocode.getCityCode()).isEqualTo("40088");
        assertThat(dtoGeocode.getPostCode()).isEqualTo("40100");
        assertThat(dtoGeocode.getAddress()).isEqualTo("1 Rue du Test");
        assertThat(dtoGeocode.getCity()).isEqualTo("Dax");
    }

    @Test
    public void geocodeKeepsOkinaValuesAndCompletesWithDataGouv() {
        server.expect(requestTo(OKINA_URL)).andRespond(withSuccess(
                "{\"features\":[{\"properties\":{\"citycode\":\"40088\",\"city\":\"Dax\"}}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withSuccess(
                "{\"features\":[{\"properties\":{\"citycode\":\"40001\",\"postcode\":\"40990\",\"name\":\"1 Rue du Test\",\"city\":\"Autre\"}}]}", MediaType.APPLICATION_JSON));

        DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(LON, LAT);

        // geo.api.gouv not called : the post code was given by data.gouv
        server.verify();
        assertThat(dtoGeocode.getCityCode()).isEqualTo("40088");
        assertThat(dtoGeocode.getCity()).isEqualTo("Dax");
        assertThat(dtoGeocode.getPostCode()).isEqualTo("40990");
        assertThat(dtoGeocode.getAddress()).isEqualTo("1 Rue du Test");
    }

    @Test
    public void geocodeEmptyWhenAllApisFail() {
        server.expect(requestTo(OKINA_URL)).andRespond(withServerError());
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withServerError());
        server.expect(requestTo(GEO_API_GOUV_URL)).andRespond(withServerError());

        DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(LON, LAT);

        server.verify();
        assertThat(dtoGeocode.getCityCode()).isNull();
        assertThat(dtoGeocode.getPostCode()).isNull();
    }

    @Test
    public void inseeFromOkina() {
        server.expect(requestTo(OKINA_URL)).andRespond(withSuccess(FULL_ADDRESS_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(ImporterUtils.getInseeFromLatLng(LON, LAT)).contains("40088");
        server.verify();
    }

    @Test
    public void inseeFallsBackOnDataGouvWhenOkinaHasNoCityCode() {
        server.expect(requestTo(OKINA_URL)).andRespond(withSuccess(NO_CITYCODE_RESPONSE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withSuccess(FULL_ADDRESS_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(ImporterUtils.getInseeFromLatLng(LON, LAT)).contains("40088");
        server.verify();
    }

    @Test
    public void inseeFallsBackOnGeoApiGouv() {
        server.expect(requestTo(OKINA_URL)).andRespond(withServerError());
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withSuccess(NO_CITYCODE_RESPONSE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(GEO_API_GOUV_URL)).andRespond(withSuccess(GEO_API_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(ImporterUtils.getInseeFromLatLng(LON, LAT)).contains("40088");
        server.verify();
    }

    @Test
    public void inseeFallsBackWhenCityCodesAreNotInseeCodes() {
        server.expect(requestTo(OKINA_URL)).andRespond(withSuccess(
                "{\"features\":[{\"properties\":{\"citycode\":\"Dax\"}}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withSuccess(
                "{\"features\":[{\"properties\":{\"citycode\":\"4008\"}}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(GEO_API_GOUV_URL)).andRespond(withSuccess(GEO_API_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(ImporterUtils.getInseeFromLatLng(LON, LAT)).contains("40088");
        server.verify();
    }

    @Test
    public void inseeEmptyWhenGeoApiGouvCodeIsNotInseeCode() {
        server.expect(requestTo(OKINA_URL)).andRespond(withServerError());
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withServerError());
        server.expect(requestTo(GEO_API_GOUV_URL)).andRespond(withSuccess(
                "[{\"nom\":\"Dax\",\"code\":\"Dax\",\"codesPostaux\":[\"40100\"]}]", MediaType.APPLICATION_JSON));

        assertThat(ImporterUtils.getInseeFromLatLng(LON, LAT)).isEmpty();
        server.verify();
    }

    @Test
    public void inseeFallsBackWhenResponseIsNotJson() {
        String htmlErrorPage = "<html>\n<head><title>502 Bad Gateway</title></head>\n<body>\n" + "<p>proxy error</p>\n".repeat(200) + "</body>\n</html>";
        server.expect(requestTo(OKINA_URL)).andRespond(withSuccess(htmlErrorPage, MediaType.TEXT_HTML));
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withSuccess(FULL_ADDRESS_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(ImporterUtils.getInseeFromLatLng(LON, LAT)).contains("40088");
        server.verify();
    }

    @Test
    public void inseeEmptyWhenAllApisFail() {
        server.expect(requestTo(OKINA_URL)).andRespond(withServerError());
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withServerError());
        server.expect(requestTo(GEO_API_GOUV_URL)).andRespond(withServerError());

        assertThat(ImporterUtils.getInseeFromLatLng(LON, LAT)).isEmpty();
        server.verify();
    }

    @Test
    public void inseeFromGeoApiGouvIsArrondissementLikeOkina() {
        server.expect(requestTo(PARIS_OKINA_URL)).andRespond(withServerError());
        server.expect(requestTo(PARIS_DATA_GOUV_URL)).andRespond(withServerError());
        server.expect(requestTo(PARIS_GEO_API_GOUV_URL)).andRespond(withSuccess(PARIS_GEO_API_RESPONSE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(PARIS_GEO_API_GOUV_ARRONDISSEMENT_URL)).andRespond(withSuccess(PARIS_GEO_API_ARRONDISSEMENT_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(ImporterUtils.getInseeFromLatLng(PARIS_LON, PARIS_LAT)).contains("75104");
        server.verify();
    }

    @Test
    public void inseeEmptyWhenArrondissementCannotBeRecovered() {
        server.expect(requestTo(PARIS_OKINA_URL)).andRespond(withServerError());
        server.expect(requestTo(PARIS_DATA_GOUV_URL)).andRespond(withServerError());
        server.expect(requestTo(PARIS_GEO_API_GOUV_URL)).andRespond(withSuccess(PARIS_GEO_API_RESPONSE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(PARIS_GEO_API_GOUV_ARRONDISSEMENT_URL)).andRespond(withServerError());

        assertThat(ImporterUtils.getInseeFromLatLng(PARIS_LON, PARIS_LAT)).isEmpty();
        server.verify();
    }

    @Test
    public void geocodePostCodeFromGeoApiGouvIsArrondissementOne() {
        server.expect(requestTo(PARIS_OKINA_URL)).andRespond(withServerError());
        server.expect(requestTo(PARIS_DATA_GOUV_URL)).andRespond(withServerError());
        server.expect(requestTo(PARIS_GEO_API_GOUV_URL)).andRespond(withSuccess(PARIS_GEO_API_RESPONSE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(PARIS_GEO_API_GOUV_ARRONDISSEMENT_URL)).andRespond(withSuccess(PARIS_GEO_API_ARRONDISSEMENT_RESPONSE, MediaType.APPLICATION_JSON));

        DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(PARIS_LON, PARIS_LAT);

        server.verify();
        assertThat(dtoGeocode.getPostCode()).isEqualTo("75004");
        assertThat(dtoGeocode.getCityCode()).isEqualTo("75104");
    }

    @Test
    public void geocodeCityCodeFromGeoApiGouvWhenNoAddressFound() {
        // e.g. in fields or mountains : no address near the point
        server.expect(requestTo(OKINA_URL)).andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(GEO_API_GOUV_URL)).andRespond(withSuccess(GEO_API_RESPONSE, MediaType.APPLICATION_JSON));

        DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(LON, LAT);

        server.verify();
        assertThat(dtoGeocode.getCityCode()).isEqualTo("40088");
        assertThat(dtoGeocode.getPostCode()).isEqualTo("40100");
        assertThat(dtoGeocode.getAddress()).isNull();
    }

    @Test
    public void geocodeCityCodeFromGeoApiGouvWhenOnlyPostCodeFound() {
        server.expect(requestTo(OKINA_URL)).andRespond(withServerError());
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withSuccess(
                "{\"features\":[{\"properties\":{\"postcode\":\"40100\"}}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(GEO_API_GOUV_URL)).andRespond(withSuccess(
                "[{\"nom\":\"Dax\",\"code\":\"40088\",\"codesPostaux\":[\"40990\"]}]", MediaType.APPLICATION_JSON));

        DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(LON, LAT);

        server.verify();
        assertThat(dtoGeocode.getCityCode()).isEqualTo("40088");
        // the post code of the address API is kept
        assertThat(dtoGeocode.getPostCode()).isEqualTo("40100");
    }

    @Test
    public void geocodeKeepsAddressApiCityCodeOverGeoApiGouv() {
        server.expect(requestTo(OKINA_URL)).andRespond(withSuccess(
                "{\"features\":[{\"properties\":{\"citycode\":\"40088\",\"city\":\"Dax\"}}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withServerError());
        server.expect(requestTo(GEO_API_GOUV_URL)).andRespond(withSuccess(
                "[{\"nom\":\"Autre\",\"code\":\"40001\",\"codesPostaux\":[\"40100\"]}]", MediaType.APPLICATION_JSON));

        DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(LON, LAT);

        server.verify();
        assertThat(dtoGeocode.getCityCode()).isEqualTo("40088");
        assertThat(dtoGeocode.getPostCode()).isEqualTo("40100");
    }

    @Test
    public void geocodeCityCodeEmptyWhenGeoApiGouvCodeIsNotInseeCode() {
        server.expect(requestTo(OKINA_URL)).andRespond(withServerError());
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withServerError());
        server.expect(requestTo(GEO_API_GOUV_URL)).andRespond(withSuccess(
                "[{\"nom\":\"Dax\",\"code\":\"Dax\",\"codesPostaux\":[\"40100\"]}]", MediaType.APPLICATION_JSON));

        DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(LON, LAT);

        server.verify();
        assertThat(dtoGeocode.getCityCode()).isNull();
        assertThat(dtoGeocode.getPostCode()).isEqualTo("40100");
    }

    @Test
    public void geocodeGeoApiGouvNotCalledWhenCityCodeAndPostCodeFound() {
        server.expect(requestTo(OKINA_URL)).andRespond(withSuccess(
                "{\"features\":[{\"properties\":{\"citycode\":\"40088\",\"postcode\":\"40100\"}}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(DATA_GOUV_URL)).andRespond(withServerError());

        DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(LON, LAT);

        server.verify();
        assertThat(dtoGeocode.getCityCode()).isEqualTo("40088");
        assertThat(dtoGeocode.getPostCode()).isEqualTo("40100");
    }

}
