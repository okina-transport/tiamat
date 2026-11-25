package org.rutebanken.tiamat.externalapis.gbfs;

import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.entur.gbfs.validation.model.FileValidationError;
import org.entur.gbfs.validation.model.ValidationResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.rutebanken.tiamat.externalapis.gbfs.GbfsFieldError.Kind.INVALID_COORDINATES;
import static org.rutebanken.tiamat.externalapis.gbfs.GbfsFieldError.Kind.INVALID_INTEGER;
import static org.rutebanken.tiamat.externalapis.gbfs.GbfsFieldError.Kind.INVALID_NUMBER;
import static org.rutebanken.tiamat.externalapis.gbfs.GbfsFieldError.Kind.MISSING;

/**
 * Runs the real GBFS validator, then checks the missing required fields extracted from its errors.
 */
class GbfsFieldErrorExtractorTest {

    private static final long NOW = Instant.now().getEpochSecond();
    private static final String NOW_ISO = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();

    @Test
    void extractsMissingFieldsOfStationsV23() {
        String stationInformation = """
                {"last_updated": %d, "ttl": 0, "version": "2.3", "data": {"stations": [
                  {"station_id": "ST1", "name": "A", "lat": 45.1, "lon": 5.7},
                  {"station_id": "42", "name": "B", "lon": 5.7},
                  {"name": "C", "lat": 45.1},
                  {"station_id": "ST4", "name": "D", "lat": 45.1, "lon": 5.7, "station_area": {"type": "MultiPolygon"}}
                ]}}
                """.formatted(NOW);

        GbfsFieldErrorExtractor.Result result = validateAndExtract("station_information", stationInformation);

        assertThat(result.fieldErrors()).containsExactlyInAnyOrder(
                new GbfsFieldError("station_information.json", MISSING, "station", "42", 2, "lat"),
                new GbfsFieldError("station_information.json", MISSING, "station", null, 3, "station_id"),
                new GbfsFieldError("station_information.json", MISSING, "station", null, 3, "lon"),
                new GbfsFieldError("station_information.json", MISSING, "station", "ST4", 4, "station_area.coordinates"));
        assertThat(result.otherErrors()).isEmpty();
    }

    @Test
    void extractsMissingFieldsOfStationsV30() {
        String stationInformation = """
                {"last_updated": "%s", "ttl": 0, "version": "3.0", "data": {"stations": [
                  {"station_id": "42", "name": [{"text": "B"}], "lon": 5.7},
                  {"station_id": "43", "name": [{"text": "C", "language": "fr"}], "lat": 45.1, "lon": 5.7,
                   "vehicle_types_capacity": [{"vehicle_type_ids": ["bike"]}]}
                ]}}
                """.formatted(NOW_ISO);

        GbfsFieldErrorExtractor.Result result = validateAndExtract("station_information", stationInformation);

        assertThat(result.fieldErrors()).containsExactlyInAnyOrder(
                new GbfsFieldError("station_information.json", MISSING, "station", "42", 1, "lat"),
                new GbfsFieldError("station_information.json", MISSING, "station", "42", 1, "name.language"),
                new GbfsFieldError("station_information.json", MISSING, "station", "43", 2, "vehicle_types_capacity.count"));
    }

    @Test
    void extractsMissingFieldsOutsideOfAListOfObjects() {
        String systemInformation = """
                {"last_updated": %d, "ttl": 0, "version": "2.3", "data": {"system_id": "s", "language": "fr", "name": "n"}}
                """.formatted(NOW);

        GbfsFieldErrorExtractor.Result result = validateAndExtract("system_information", systemInformation);

        assertThat(result.fieldErrors()).containsExactly(
                new GbfsFieldError("system_information.json", MISSING, null, null, null, "timezone"));
    }

    @Test
    void doesNotReportMaxRangeMetersWhenPropulsionTypeIsMissing() {
        String vehicleTypes = """
                {"last_updated": %d, "ttl": 0, "version": "2.3", "data": {"vehicle_types": [
                  {"vehicle_type_id": "ebike", "form_factor": "bicycle", "propulsion_type": "electric_assist"},
                  {"vehicle_type_id": "bike", "form_factor": "bicycle"}
                ]}}
                """.formatted(NOW);

        GbfsFieldErrorExtractor.Result result = validateAndExtract("vehicle_types", vehicleTypes);

        assertThat(result.fieldErrors()).containsExactlyInAnyOrder(
                new GbfsFieldError("vehicle_types.json", MISSING, "vehicle_type", "ebike", 1, "max_range_meters"),
                new GbfsFieldError("vehicle_types.json", MISSING, "vehicle_type", "bike", 2, "propulsion_type"));
    }

    @Test
    void keepsTheOtherErrorsApart() {
        String stationInformation = """
                {"last_updated": %d, "ttl": 0, "version": "2.3", "data": {"stations": [
                  {"station_id": "42", "name": "B", "lat": 45.1, "lon": 5.7, "rental_methods": ["bitcoin"]}
                ]}}
                """.formatted(NOW);

        GbfsFieldErrorExtractor.Result result = validateAndExtract("station_information", stationInformation);

        assertThat(result.fieldErrors()).isEmpty();
        assertThat(result.otherErrors()).isNotEmpty();
    }

    @Test
    void extractsFieldsWhichAreNotNumbersV23() {
        String stationInformation = """
                {"last_updated": %d, "ttl": 0, "version": "2.3", "data": {"stations": [
                  {"station_id": "42", "name": "B", "lat": 45.1, "lon": "5,7"},
                  {"station_id": "43", "name": "C", "lat": "45.1", "lon": null, "capacity": "12"},
                  {"station_id": "44", "name": "D", "lat": true, "lon": 5.7, "capacity": 12.5,
                   "vehicle_type_capacity": {"bike": "x"}}
                ]}}
                """.formatted(NOW);

        GbfsFieldErrorExtractor.Result result = validateAndExtract("station_information", stationInformation);

        assertThat(result.fieldErrors()).containsExactlyInAnyOrder(
                new GbfsFieldError("station_information.json", INVALID_NUMBER, "station", "42", 1, "lon"),
                new GbfsFieldError("station_information.json", INVALID_NUMBER, "station", "43", 2, "lat"),
                new GbfsFieldError("station_information.json", INVALID_NUMBER, "station", "43", 2, "lon"),
                new GbfsFieldError("station_information.json", INVALID_INTEGER, "station", "43", 2, "capacity"),
                new GbfsFieldError("station_information.json", INVALID_NUMBER, "station", "44", 3, "lat"),
                new GbfsFieldError("station_information.json", INVALID_INTEGER, "station", "44", 3, "capacity"),
                new GbfsFieldError("station_information.json", INVALID_NUMBER, "station", "44", 3, "vehicle_type_capacity.bike"));
        assertThat(result.otherErrors()).isEmpty();
    }

    @Test
    void extractsFieldsWhichAreNotNumbersV30() {
        String stationInformation = """
                {"last_updated": "%s", "ttl": 0, "version": "3.0", "data": {"stations": [
                  {"station_id": "42", "name": [{"text": "B", "language": "fr"}], "lat": 45.1, "lon": "abc",
                   "vehicle_types_capacity": [{"vehicle_type_ids": ["bike"], "count": "3"}]}
                ]}}
                """.formatted(NOW_ISO);

        GbfsFieldErrorExtractor.Result result = validateAndExtract("station_information", stationInformation);

        assertThat(result.fieldErrors()).containsExactlyInAnyOrder(
                new GbfsFieldError("station_information.json", INVALID_NUMBER, "station", "42", 1, "lon"),
                new GbfsFieldError("station_information.json", INVALID_INTEGER, "station", "42", 1, "vehicle_types_capacity.count"));
    }

    @Test
    void extractsFieldsWhichAreNotNumbersOutsideOfAListOfObjects() {
        String systemInformation = """
                {"last_updated": %d, "ttl": "60", "version": "2.3",
                 "data": {"system_id": "s", "language": "fr", "name": "n", "timezone": "Europe/Paris"}}
                """.formatted(NOW);

        GbfsFieldErrorExtractor.Result result = validateAndExtract("system_information", systemInformation);

        assertThat(result.fieldErrors()).containsExactly(
                new GbfsFieldError("system_information.json", INVALID_INTEGER, null, null, null, "ttl"));
    }

    @Test
    void findsInvalidStationCoordinatesV23() {
        String stationInformation = """
                {"last_updated": %d, "ttl": 0, "version": "2.3", "data": {"stations": [
                  {"station_id": "41", "name": "OK", "lat": 45.1, "lon": 0},
                  {"station_id": "42", "name": "A", "lat": 0, "lon": 0},
                  {"station_id": "43", "name": "B", "lat": 91.5, "lon": 5.7},
                  {"station_id": "44", "name": "C", "lat": 45.1, "lon": -181},
                  {"name": "D", "lat": 0.0, "lon": 0.0},
                  {"station_id": "46", "name": "E", "lat": "0", "lon": 0}
                ]}}
                """.formatted(NOW);
        byte[] bytes = stationInformation.getBytes(StandardCharsets.UTF_8);

        GbfsFieldErrorExtractor.Result result = validateAndExtract("station_information", stationInformation);
        List<GbfsFieldError> coordinates = GbfsFieldErrorExtractor.findInvalidStationCoordinates("station_information.json", bytes);

        // the values out of range are only reported with the coordinates of the station
        assertThat(result.fieldErrors()).containsExactlyInAnyOrder(
                new GbfsFieldError("station_information.json", MISSING, "station", null, 5, "station_id"),
                new GbfsFieldError("station_information.json", INVALID_NUMBER, "station", "46", 6, "lat"));
        assertThat(result.otherErrors()).isEmpty();
        assertThat(coordinates).containsExactly(
                new GbfsFieldError("station_information.json", INVALID_COORDINATES, "station", "42", 2, "lat, lon", "0, 0"),
                new GbfsFieldError("station_information.json", INVALID_COORDINATES, "station", "43", 3, "lat, lon", "91.5, 5.7"),
                new GbfsFieldError("station_information.json", INVALID_COORDINATES, "station", "44", 4, "lat, lon", "45.1, -181"),
                new GbfsFieldError("station_information.json", INVALID_COORDINATES, "station", null, 5, "lat, lon", "0.0, 0.0"));
    }

    @Test
    void findsInvalidStationCoordinatesV30() {
        String stationInformation = """
                {"last_updated": "%s", "ttl": 0, "version": "3.0", "data": {"stations": [
                  {"station_id": "43", "name": [{"text": "B", "language": "fr"}], "lat": -95, "lon": 200}
                ]}}
                """.formatted(NOW_ISO);

        GbfsFieldErrorExtractor.Result result = validateAndExtract("station_information", stationInformation);
        List<GbfsFieldError> coordinates = GbfsFieldErrorExtractor.findInvalidStationCoordinates("station_information.json",
                stationInformation.getBytes(StandardCharsets.UTF_8));

        assertThat(result.fieldErrors()).isEmpty();
        assertThat(result.otherErrors()).isEmpty();
        assertThat(coordinates).containsExactly(
                new GbfsFieldError("station_information.json", INVALID_COORDINATES, "station", "43", 1, "lat, lon", "-95, 200"));
    }

    private static GbfsFieldErrorExtractor.Result validateAndExtract(String feed, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Map<String, InputStream> files = Map.of(feed, new ByteArrayInputStream(bytes));
        ValidationResult validation = GbfsValidatorFactory.getGbfsJsonValidator().validate(files);
        List<FileValidationError> errors = validation.files().get(feed).errors();
        return GbfsFieldErrorExtractor.extract(feed + ".json", errors, bytes);
    }
}
