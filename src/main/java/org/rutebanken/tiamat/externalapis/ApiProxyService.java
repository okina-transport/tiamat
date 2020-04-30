package org.rutebanken.tiamat.externalapis;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Strings;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.client.ClientBuilder;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


/**
 * Service métier Stop Areas
 */

public class ApiProxyService {

	private static final String FIRST_GOUV_API_REVERSE_GEOCODE_REQUEST = "https://api-adresse.data.gouv.fr/reverse/";
	private static final String SECOND_GOUV_API_REVERSE_GEOCODE_REQUEST = "https://geo.api.gouv.fr/communes?lon={lon}&lat={lat}";


	private static final BigDecimal LAT_MIN_BOUND = new BigDecimal(-90);
	private static final BigDecimal LAT_MAX_BOUND = new BigDecimal(90);
	private static final BigDecimal LON_MIN_BOUND = new BigDecimal(-180);
	private static final BigDecimal LON_MAX_BOUND = new BigDecimal(1800);

	public static final Logger logger = LoggerFactory.getLogger(ApiProxyService.class);


	/**
	 * "Base" de client jersey. Cette webtarget est ensuite dérivée spécifiquement dans les méthodes pour cibler les ressources voulues.
	 */
	private javax.ws.rs.client.Client client = ClientBuilder.newBuilder()
		.build()
		.register(JacksonJaxbJsonProvider.class)
		.register(JacksonJsonProvider.class);


	/**
	 * Reverse geocoding d'après long / lat
	 *
	 * @param latitude
	 * @param longitude
	 * @return Geojson Object
	 */
	public String getCitycodeByReverseGeocoding(@NotNull BigDecimal latitude, @NotNull BigDecimal longitude) throws Exception {
		if (latitude == null || longitude == null) {
			throw new Exception("Latitude / Longitude ne peuvent pas être null pour demander un reverse geocoding");
		}
		if (latitude.compareTo(LAT_MIN_BOUND) < 0
			|| latitude.compareTo(LAT_MAX_BOUND) > 0
			|| longitude.compareTo(LON_MIN_BOUND) < 0
			|| longitude.compareTo(LON_MAX_BOUND) > 0) {
			throw new Exception("Latitude / Longitude hors des valeurs permises (LAT : -90 -> 90; LON : -180 -> 180)");
		}

		URL apiUrl = new URL(FIRST_GOUV_API_REVERSE_GEOCODE_REQUEST + "?lon=" + longitude + "&lat=" + latitude);

		String citycode = getCitycode(apiUrl);

		if(Strings.isNullOrEmpty(citycode)){
			GouvApiReverseGeocoding gouvApiReverseGeocoding;
			try {
				gouvApiReverseGeocoding = Arrays.stream(client.target(SECOND_GOUV_API_REVERSE_GEOCODE_REQUEST)
						.resolveTemplate("lon", longitude)
						.resolveTemplate("lat", latitude)
						.request().get().readEntity(GouvApiReverseGeocoding[].class))
						.findFirst()
						.orElse(null);
			} catch (Exception e) {
				throw new Exception("Format de réponse du WS inattendu");
			}

			if(gouvApiReverseGeocoding != null){
				citycode = gouvApiReverseGeocoding.getCode();
			}
		}

		return citycode;
	}

	private String getCitycode(URL apiUrl) throws Exception {
		InputStream inputStream;
		try {
			inputStream = apiUrl.openStream();
		} catch (Exception e) {
			logger.error("Problème de connexion à l'API");
			return null;
		}

        StringWriter writer = new StringWriter();
        String encoding = StandardCharsets.UTF_8.name();

        try{
			IOUtils.copy(inputStream, writer, encoding);
			org.codehaus.jettison.json.JSONObject jsonObject = new JSONObject(writer.toString());
			org.codehaus.jettison.json.JSONArray features = jsonObject.optJSONArray("features");
			org.codehaus.jettison.json.JSONObject properties = (JSONObject) features.get(0);
			org.codehaus.jettison.json.JSONObject cityCode = properties.getJSONObject("properties");
			return cityCode.getString("citycode");

		}catch (Exception e){
        	logger.error("Problème sur la récupération du city code");
        	return null;
		}
	}
}
