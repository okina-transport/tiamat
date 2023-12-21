package org.rutebanken.tiamat.importer;


import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.rutebanken.tiamat.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
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

    public static DtoGeocode getGeocodeDataByReverseGeocoding(double x, double y) {
        DtoGeocode dtoGeocode = getGeocodeDataFromOkinaAPI(x, y);
        getGeocodeDataFromDataGouvAPI(x, y, dtoGeocode);
        getGeocodeDataFromGeoGouvAPI(x, y, dtoGeocode);
        return dtoGeocode;
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

    /**
     * Update wheelchair limitation in a quay
     * @param siteElement
     *  quay on which wheelchair limitation must be udpated
     * @param wheelchairLimitation
     *  new value for wheelchair limitation
     */
    public static void updateWheelchairLimitation(SiteElement siteElement, LimitationStatusEnumeration wheelchairLimitation) {
        AccessibilityAssessment assessmentToUpdate = siteElement.getAccessibilityAssessment() == null ? new AccessibilityAssessment() : siteElement.getAccessibilityAssessment();
        List<AccessibilityLimitation> limitationsToUpdate = assessmentToUpdate.getLimitations() == null ? new ArrayList<>() : assessmentToUpdate.getLimitations();
        limitationsToUpdate.get(0).setWheelchairAccess(wheelchairLimitation);
    }

    /**
     * Read a siteElement/stopPlace and return the value for wheelchair limitation
     * @param siteElement
     * @return
     *  wheelchair limitation of the siteElement
     */
    public static Optional<LimitationStatusEnumeration> getWheelchairLimitation(SiteElement siteElement){
        if (siteElement.getAccessibilityAssessment() == null || siteElement.getAccessibilityAssessment().getLimitations() == null
                || siteElement.getAccessibilityAssessment().getLimitations().size() == 0){
            return Optional.empty();
        }

        return Optional.of(siteElement.getAccessibilityAssessment().getLimitations().get(0).getWheelchairAccess());
    }

    private static DtoGeocode getGeocodeDataFromOkinaAPI(double x, double y) {
        final String okinaUrl = String.format(OKINA_ENDPOINT, x, y);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity response = null;

        try {
            response = restTemplate.exchange(okinaUrl, HttpMethod.GET, HttpEntity.EMPTY, String.class);
            JSONObject body = new JSONObject(Objects.requireNonNull(response.getBody()).toString());

            if (body.getJSONArray("features") != null && body.getJSONArray("features").length() > 0) {

                JSONObject properties = body.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
                String cityCode = properties.has("citycode") ? properties.getString("citycode") : "";
                String postCode  = properties.has("postcode") ? properties.getString("postcode") : "";
                String address = properties.has("name") ? properties.getString("name") : "";
                String city = properties.has("city") ? properties.getString("city") : "";

                DtoGeocode geocodeResult = new DtoGeocode();
                if(StringUtils.isNumeric(cityCode)){
                    geocodeResult.setCityCode(cityCode);
                }
                if(StringUtils.isNumeric(postCode)) {
                    geocodeResult.setPostCode(postCode);
                }
                geocodeResult.setAddress(address);
                geocodeResult.setCity(city);
                return geocodeResult;
            }
        } catch (RestClientException | JSONException | IllegalArgumentException e) {
            logger.error("Error on geocode recovering", e);
            logger.error("okinaUrl : " + okinaUrl);
            if (response != null && response.getBody() != null){
                logger.error(response.getBody().toString());
            }

        }
        return new DtoGeocode();
    }

    private static DtoGeocode getGeocodeDataFromDataGouvAPI(double x, double y, DtoGeocode dtoGeocode) {
        final String dataGouvUrl = String.format(DATA_GOUV_ENDPOINT, y, x);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity response = null;

        try {
            response = restTemplate.exchange(dataGouvUrl, HttpMethod.GET, HttpEntity.EMPTY, String.class);
            JSONObject body = new JSONObject(Objects.requireNonNull(response.getBody()).toString());

            if (body.getJSONArray("features") != null && body.getJSONArray("features").length() > 0) {

                JSONObject properties = body.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
                if(org.apache.commons.lang3.StringUtils.isEmpty(dtoGeocode.getCityCode())){
                    String cityCode = properties.has("citycode") ? properties.getString("citycode") : "";
                    if(StringUtils.isNumeric(cityCode)){
                        dtoGeocode.setCityCode(cityCode);
                    }
                }
                if(org.apache.commons.lang3.StringUtils.isEmpty(dtoGeocode.getPostCode())){
                    String postCode = properties.has("postcode") ? properties.getString("postcode") : "";
                    if(StringUtils.isNumeric(postCode)) {
                        dtoGeocode.setPostCode(postCode);
                    }
                }
                if(org.apache.commons.lang3.StringUtils.isEmpty(dtoGeocode.getAddress())){
                    String address = properties.has("name") ? properties.getString("name") : "";
                    dtoGeocode.setAddress(address);
                }
                if(org.apache.commons.lang3.StringUtils.isEmpty(dtoGeocode.getCity())){
                    String city = properties.has("city") ? properties.getString("city") : "";
                    dtoGeocode.setCity(city);
                }

                return dtoGeocode;
            }
        } catch (RestClientException | JSONException | IllegalArgumentException e) {
            logger.error("Error on geocode recovering", e);
            logger.error("dataGouvUrl : " + dataGouvUrl);
            if (response != null && response.getBody() != null){
                logger.error(response.getBody().toString());
            }

        }
        return dtoGeocode;
    }

    private static DtoGeocode getGeocodeDataFromGeoGouvAPI(double x, double y, DtoGeocode dtoGeocode) {
        final String geoApiGouv = String.format(GEO_API_GOUV_ENDPOINT, y, x);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity response = null;

        try {
            response = restTemplate.exchange(geoApiGouv, HttpMethod.GET, HttpEntity.EMPTY, String.class);
            JSONArray body = new JSONArray(Objects.requireNonNull(response.getBody()).toString());

            if (body.length() > 0 &&
                    body.getJSONObject(0).getJSONArray("codesPostaux") != null &&
                    body.getJSONObject(0).getJSONArray("codesPostaux").length() > 0) {
                String postCode = (String) body.getJSONObject(0).getJSONArray("codesPostaux").get(0);
                if(org.apache.commons.lang3.StringUtils.isEmpty(dtoGeocode.getPostCode())){
                    if(StringUtils.isNumeric(postCode)) {
                        dtoGeocode.setPostCode(postCode);
                    }
                }

                return dtoGeocode;
            }
        } catch (RestClientException | JSONException | IllegalArgumentException e) {
            logger.error("Error on geocode recovering", e);
            logger.error("geoApiGouv : " + geoApiGouv);
            if (response != null && response.getBody() != null){
                logger.error(response.getBody().toString());
            }

        }
        return dtoGeocode;
    }



}
