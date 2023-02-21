package org.rutebanken.tiamat.externalapis;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Service métier Stop Areas
 */

@Component
public class ApiProxyService {

    public static final Logger logger = LoggerFactory.getLogger(ApiProxyService.class);
    private final static String OKINA_ENDPOINT = "https://api.adresse.okina.fr/reverse?lon=%s&lat=%s";
    private final static String DATA_GOUV_ENDPOINT = "https://api-adresse.data.gouv.fr/reverse/?lon=%s&lat=%s";
    private final static String GEO_API_GOUV_ENDPOINT = "https://geo.api.gouv.fr/communes?lon=%s&lat=%s";

    @Autowired
    private QuayRepository quayRepository;

    @Scheduled(cron = "0 30 8 ? * MON-FRI")
    void populateCodeInsee() {
        logger.info("Starting insee recovering of quays service");
        List<Quay> quays = quayRepository.findQuaysWithoutZipcode();
        for (Quay quay : quays) {
            Optional<String> citycodeReverseGeocoding = ApiProxyService.getInseeFromLatLng(quay.getCentroid().getCoordinate().x, quay.getCentroid().getCoordinate().y);
            if (citycodeReverseGeocoding.isPresent()) {
                quay.setZipCode(citycodeReverseGeocoding.get());
                quayRepository.save(quay);
                logger.info("Adding insee code : " + quay.getZipCode() + " of quay : " + quay.getNetexId());
            } else {
                logger.error("Error on insee recovering of quay : " + quay.getNetexId());
            }
        }
        logger.info("Insee recovering of quays service done");
    }

    public static Optional<String> getInseeFromLatLng(double x, double y) {
        return getInseeFromOkinaAPI(x, y);
    }


    private static Optional<String> getInseeFromOkinaAPI(double x, double y) {
        String okinaUrl = String.format(OKINA_ENDPOINT, x, y);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity response = null;
        String city;

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
            if (response != null && response.getBody() != null) {
                logger.error(response.getBody().toString());
            }
            return getInseeFromGovAPI(x, y);
        }

        return getInseeFromGovAPI(x, y);
    }

    private static Optional<String> getInseeFromGovAPI(double x, double y) {
        String dataGouvUrl = String.format(DATA_GOUV_ENDPOINT, x, y);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity response = null;
        String city;

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
            if (response != null && response.getBody() != null) {
                logger.error(response.getBody().toString());
            }
            return getInseeFromGeoApiGouv(x, y);
        }

        return getInseeFromGeoApiGouv(x, y);
    }

    private static Optional<String> getInseeFromGeoApiGouv(double x, double y) {
        String geoApiGouvUrl = String.format(GEO_API_GOUV_ENDPOINT, x, y);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity response = null;
        String city;

        try {
            response = restTemplate.exchange(geoApiGouvUrl, HttpMethod.GET, HttpEntity.EMPTY, String.class);
            JSONArray body = new JSONArray(Objects.requireNonNull(response.getBody()).toString());

            if (body.length() > 0 && body.getJSONObject(0) != null && StringUtils.isNotEmpty(body.getJSONObject(0).get("code").toString())) {
                city = body.getJSONObject(0).get("code").toString();
                return Optional.of(city);
            }
        } catch (Exception e) {
            logger.error("Error on insee recovering", e);
            logger.error("geoApiGouvUrl : " + geoApiGouvUrl);
            if (response != null && response.getBody() != null) {
                logger.error(response.getBody().toString());
            }
        }

        return Optional.empty();
    }
}
