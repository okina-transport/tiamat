package org.rutebanken.tiamat.importer;


import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.Optional;

public class ImporterUtils {

    private static final Logger logger = LoggerFactory.getLogger(ImporterUtils.class);

    private final static String OKINA_ENDPOINT = "https://api.adresse.okina.fr/reverse?lon=%s&lat=%s";
    private final static String DATA_GOUV_ENDPOINT = "https://api-adresse.data.gouv.fr/reverse/?lat=%s&lon=%s";
    private final static String GEO_API_GOUV_ENDPOINT = "https://geo.api.gouv.fr/communes?lat=%s&lon=%s&fields=nom,code,codesPostaux&format=json";

    public static Optional<String> getInseeFromLatLng(double x, double y) {

        Optional<String> inseeOpt = getInseeFromOkinaAPI(x, y);
        return inseeOpt.isPresent() ? inseeOpt : getInseeFromGovAPI(x,y);
    }


    private static Optional<String> getInseeFromOkinaAPI(double x, double y) {
        final String okinaUrl = String.format(OKINA_ENDPOINT, x,y);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity response = null;
        String city = "";

        try {
            response = restTemplate.exchange(okinaUrl, HttpMethod.GET, HttpEntity.EMPTY, String.class);
            JSONObject body = new JSONObject(Objects.requireNonNull(response.getBody()).toString());

            if (body.getJSONArray("features") != null && body.getJSONArray("features").length() > 0) {

                JSONObject properties = body.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
                city = properties.has("citycode") ? properties.getString("citycode") : "";
                return Optional.of(city);
            }
        } catch (RestClientException | JSONException | IllegalArgumentException e) {
            logger.error("Error on insee recovering", e);
            logger.error("okinaUrl : " + okinaUrl);
            if (response != null && response.getBody() != null){
                logger.error(response.getBody().toString());
            }

        }
        return Optional.empty();
    }

    private static Optional<String> getInseeFromGovAPI(double x, double y) {
        final String dataGouvUrl = String.format(DATA_GOUV_ENDPOINT, y,x);
        final String geoApiGouvUrl = String.format(GEO_API_GOUV_ENDPOINT, x,y);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity response = null;
        String city = "";

        try {
            response = restTemplate.exchange(dataGouvUrl, HttpMethod.GET, HttpEntity.EMPTY, String.class);
            JSONObject body = new JSONObject(Objects.requireNonNull(response.getBody()).toString());

            if (body.getJSONArray("features") != null && body.getJSONArray("features").length() > 0) {

                JSONObject properties = body.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
                city = properties.has("citycode") ? properties.getString("citycode") : "";
                return Optional.of(city);
            }
        } catch (RestClientException | JSONException | IllegalArgumentException e) {
            logger.error("Error on insee recovering", e);
            logger.error("dataGouvUrl : " + dataGouvUrl);
            logger.error("geoApiGouvUrl : " + geoApiGouvUrl);
            if (response != null && response.getBody() != null){
                logger.error(response.getBody().toString());
            }

        }


        //2ème essai avec la 2ème URL

        try {
            response = restTemplate.exchange(geoApiGouvUrl, HttpMethod.GET, HttpEntity.EMPTY, Object.class);
            JSONObject body = new JSONObject(Objects.requireNonNull(response.getBody()).toString());

            if (body.getString("nom") != null && !body.getString("nom").isEmpty()) {
                city = body.getString("nom");
                return Optional.of(city);
            }
        } catch (RestClientException | JSONException | IllegalArgumentException e) {
            logger.error("Error on insee recovering", e);
            logger.error("dataGouvUrl : " + dataGouvUrl);
            logger.error("geoApiGouvUrl : " + geoApiGouvUrl);
            if (response != null && response.getBody() != null){
                logger.error(response.getBody().toString());
            }
        }

        return Optional.empty();
    }
}
