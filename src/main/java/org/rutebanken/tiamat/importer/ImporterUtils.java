package org.rutebanken.tiamat.importer;


import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.rutebanken.tiamat.model.AccessibilityAssessment;
import org.rutebanken.tiamat.model.AccessibilityLimitation;
import org.rutebanken.tiamat.model.LimitationStatusEnumeration;
import org.rutebanken.tiamat.model.SiteElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class ImporterUtils {

    private static final Logger logger = LoggerFactory.getLogger(ImporterUtils.class);

    private static final String OKINA_ENDPOINT = "https://api.adresse.okina.fr/reverse?lon=%s&lat=%s";
    private static final String DATA_GOUV_ENDPOINT = "https://api-adresse.data.gouv.fr/reverse/?lat=%s&lon=%s";
    private static final String GEO_API_GOUV_ENDPOINT = "https://geo.api.gouv.fr/communes?lat=%s&lon=%s&fields=nom,code,codesPostaux&format=json";
    private static final String GEO_API_GOUV_ARRONDISSEMENT_ENDPOINT = GEO_API_GOUV_ENDPOINT + "&type=arrondissement-municipal";

    /** Paris, Lyon, Marseille : Okina returns the code of the arrondissement, geo.api.gouv returns the one of the commune by default */
    private static final Set<String> COMMUNES_WITH_ARRONDISSEMENTS = Set.of("75056", "69123", "13055");

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    /** INSEE code : 5 digits, or 2A/2B followed by 3 digits for Corsica */
    private static final Pattern INSEE_CODE_PATTERN = Pattern.compile("\\d{5}|2[AB]\\d{3}");

    /**
     * Shared, with timeouts so that an API not responding does not block imports and batches : a point can need up to
     * 4 sequential calls when Okina does not return everything, some of them made synchronously from GraphQL. Package-private for tests
     */
    static final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        return new RestTemplate(requestFactory);
    }

    public static Optional<String> getInseeFromLatLng(double x, double y) {
        return getInseeFromAddressAPI(OKINA_ENDPOINT, x, y)
                .or(() -> getInseeFromAddressAPI(DATA_GOUV_ENDPOINT, y, x))
                .or(() -> getCommuneFromGeoApiGouv(x, y)
                        .map(commune -> commune.optString("code"))
                        .filter(ImporterUtils::isInseeCode));
    }

    public static DtoGeocode getGeocodeDataByReverseGeocoding(double x, double y) {
        DtoGeocode dtoGeocode = new DtoGeocode();
        getFirstFeatureProperties(String.format(OKINA_ENDPOINT, x, y), true)
                .ifPresent(properties -> fillGeocodeData(dtoGeocode, properties));
        if (isComplete(dtoGeocode)) {
            return dtoGeocode;
        }
        getFirstFeatureProperties(String.format(DATA_GOUV_ENDPOINT, y, x), true)
                .ifPresent(properties -> fillGeocodeData(dtoGeocode, properties));
        // geo.api.gouv knows the commune even far from any address (fields, mountains), but has neither address nor street
        if (StringUtils.isEmpty(dtoGeocode.getCityCode()) || StringUtils.isEmpty(dtoGeocode.getPostCode())) {
            getCommuneFromGeoApiGouv(x, y).ifPresent(commune -> fillGeocodeDataFromCommune(dtoGeocode, commune));
        }
        return dtoGeocode;
    }

    private static void fillGeocodeDataFromCommune(DtoGeocode dtoGeocode, JSONObject commune) {
        if (StringUtils.isEmpty(dtoGeocode.getCityCode()) && isInseeCode(commune.optString("code"))) {
            dtoGeocode.setCityCode(commune.optString("code"));
        }
        if (StringUtils.isEmpty(dtoGeocode.getPostCode())) {
            Optional.ofNullable(commune.optJSONArray("codesPostaux"))
                    .filter(postCodes -> postCodes.length() > 0)
                    .map(postCodes -> postCodes.optString(0))
                    .filter(StringUtils::isNumeric)
                    .ifPresent(dtoGeocode::setPostCode);
        }
    }

    private static boolean isComplete(DtoGeocode dtoGeocode) {
        return StringUtils.isNoneEmpty(dtoGeocode.getCityCode(), dtoGeocode.getPostCode(), dtoGeocode.getAddress(), dtoGeocode.getCity());
    }

    /**
     * Okina and data.gouv share the same response format, only the order of the coordinates in the URL differs.
     * geo.api.gouv is always called afterwards as a fallback
     */
    private static Optional<String> getInseeFromAddressAPI(String endpoint, double first, double second) {
        return getFirstFeatureProperties(String.format(endpoint, first, second), true)
                .map(properties -> properties.optString("citycode"))
                .filter(ImporterUtils::isInseeCode);
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

    /**
     * Only fills the fields that are still empty : the APIs are called by order of priority
     */
    private static void fillGeocodeData(DtoGeocode dtoGeocode, JSONObject properties) {
        if (StringUtils.isEmpty(dtoGeocode.getCityCode()) && isInseeCode(properties.optString("citycode"))) {
            dtoGeocode.setCityCode(properties.optString("citycode"));
        }
        if (StringUtils.isEmpty(dtoGeocode.getPostCode()) && StringUtils.isNumeric(properties.optString("postcode"))) {
            dtoGeocode.setPostCode(properties.optString("postcode"));
        }
        if (StringUtils.isEmpty(dtoGeocode.getAddress())) {
            dtoGeocode.setAddress(properties.optString("name"));
        }
        if (StringUtils.isEmpty(dtoGeocode.getCity())) {
            dtoGeocode.setCity(properties.optString("city"));
        }
    }

    private static Optional<JSONObject> getFirstFeatureProperties(String url, boolean hasFallback) {
        return callApi(url, hasFallback, body -> {
            JSONArray features = new JSONObject(body).getJSONArray("features");
            return features.length() > 0 ? features.getJSONObject(0).getJSONObject("properties") : null;
        });
    }

    /**
     * For Paris, Lyon and Marseille, geo.api.gouv returns the commune whereas Okina, which is the reference, returns the
     * arrondissement : in this case the arrondissement is requested. Returns empty if it cannot be recovered, so that no
     * code different from Okina's is stored
     */
    private static Optional<JSONObject> getCommuneFromGeoApiGouv(double x, double y) {
        Optional<JSONObject> commune = callApi(String.format(GEO_API_GOUV_ENDPOINT, y, x), false, ImporterUtils::getFirstCommune);
        if (commune.isPresent() && COMMUNES_WITH_ARRONDISSEMENTS.contains(commune.get().optString("code"))) {
            return callApi(String.format(GEO_API_GOUV_ARRONDISSEMENT_ENDPOINT, y, x), false, ImporterUtils::getFirstCommune);
        }
        return commune;
    }

    private static JSONObject getFirstCommune(String body) throws JSONException {
        JSONArray communes = new JSONArray(body);
        return communes.length() > 0 ? communes.getJSONObject(0) : null;
    }

    @FunctionalInterface
    private interface ResponseParser<T> {
        /** Returns null if the response does not contain the expected data */
        T parse(String body) throws JSONException;
    }

    /**
     * Calls a geocoding API and parses its response. Errors are logged at warn level if another API will be called
     * afterwards, at error level otherwise
     */
    private static <T> Optional<T> callApi(String url, boolean hasFallback, ResponseParser<T> parser) {
        String body = null;
        try {
            body = restTemplate.getForObject(url, String.class);
            if (body == null) {
                log(hasFallback, "Empty response from {}", url);
                return Optional.empty();
            }
            return Optional.ofNullable(parser.parse(body));
        } catch (RestClientException | JSONException | IllegalArgumentException e) {
            log(hasFallback, "Error on geocoding API call {} : {}", url, shortMessage(e));
            logger.debug("Error on geocoding API call", e);
            if (body != null) {
                log(hasFallback, "Unusable response ({} characters), beginning : {}", body.length(), StringUtils.abbreviate(StringUtils.normalizeSpace(body), 200));
                logger.debug("Full response : {}", body);
            }
        }
        return Optional.empty();
    }

    private static void log(boolean hasFallback, String format, Object... arguments) {
        if (hasFallback) {
            logger.warn(format, arguments);
        } else {
            logger.error(format, arguments);
        }
    }

    /**
     * Exception message on a single line and truncated : JSON parsing exceptions contain the whole unreadable response
     */
    static String shortMessage(Exception e) {
        return StringUtils.abbreviate(StringUtils.normalizeSpace(e.getMessage()), 200);
    }

    private static boolean isInseeCode(String code) {
        return code != null && INSEE_CODE_PATTERN.matcher(code).matches();
    }

    public static org.locationtech.jts.geom.Point createPoint(double lon, double lat){
        return createPoint(new Coordinate(lon, lat));
    }

    public static org.locationtech.jts.geom.Point createPoint(Coordinate coordinates){
        Point point = geometryFactory.createPoint(coordinates);
        point.setSRID(4326);
        return point;
    }



}
