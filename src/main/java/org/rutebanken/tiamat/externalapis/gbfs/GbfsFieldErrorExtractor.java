package org.rutebanken.tiamat.externalapis.gbfs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.entur.gbfs.validation.model.FileValidationError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the field errors from the errors of the GBFS validator, which reports one error per field:
 * <ul>
 * <li>missing required field: "#/data/stations/1: required key [lat] not found"</li>
 * <li>field which is not a number: "#/data/stations/1/lon: expected type: Number, found: String" (or Integer)</li>
 * </ul>
 * The coordinates of the stations are checked by {@link #findInvalidStationCoordinates}: the validator only reports
 * the values out of range, and accepts (0, 0).
 */
public final class GbfsFieldErrorExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern REQUIRED_KEY_NOT_FOUND = Pattern.compile("^(#[^:]*): required key \\[([^\\]]+)] not found$");
    private static final Pattern NUMBER_EXPECTED = Pattern.compile("^(#[^:]*): expected type: (Number|Integer), found: .*$");
    private static final Pattern INDEX = Pattern.compile("\\d+");
    private static final Pattern OUT_OF_RANGE = Pattern.compile("^(#[^:]*): \\S+ is not (less|greater) or equal to \\S+$");
    private static final Pattern STATION_COORDINATE = Pattern.compile("^#/data/stations/\\d+/(lat|lon)$");

    /**
     * List name in "data" -> kind of object and name of its id field
     */
    private static final Map<String, Entity> ENTITIES = Map.of(
            "stations", new Entity("station", "station_id"),
            "vehicle_types", new Entity("vehicle_type", "vehicle_type_id"),
            "plans", new Entity("plan", "plan_id")
    );

    /**
     * max_range_meters is only required when propulsion_type is not human: when propulsion_type is missing, the
     * validator also reports max_range_meters as missing.
     */
    private static final String PROPULSION_TYPE = "propulsion_type";
    private static final String MAX_RANGE_METERS = "max_range_meters";

    private GbfsFieldErrorExtractor() {
    }

    /**
     * @param fieldErrors the field errors
     * @param otherErrors the other errors of the validator (wrong value, wrong format…)
     */
    public record Result(List<GbfsFieldError> fieldErrors, List<FileValidationError> otherErrors) {
    }

    private record Entity(String name, String idField) {
    }

    /**
     * @param file      name of the file (ex: station_information.json)
     * @param errors    errors reported by the GBFS validator on this file
     * @param fileBytes content of the file, to find the id of the objects holding the fields in error
     */
    public static Result extract(String file, List<FileValidationError> errors, byte[] fileBytes) {
        JsonNode root = readTree(fileBytes);
        List<GbfsFieldError> fieldErrors = new ArrayList<>();
        List<FileValidationError> otherErrors = new ArrayList<>();
        for (FileValidationError error : errors) {
            String message = error.message() != null ? error.message() : "";
            Matcher missing = REQUIRED_KEY_NOT_FOUND.matcher(message);
            Matcher notANumber = NUMBER_EXPECTED.matcher(message);
            if (missing.matches()) {
                fieldErrors.add(toFieldError(file, GbfsFieldError.Kind.MISSING, pathSegments(missing.group(1)), missing.group(2), root));
            } else if (notANumber.matches()) {
                List<String> segments = pathSegments(notANumber.group(1));
                GbfsFieldError.Kind kind = "Integer".equals(notANumber.group(2))
                        ? GbfsFieldError.Kind.INVALID_INTEGER
                        : GbfsFieldError.Kind.INVALID_NUMBER;
                fieldErrors.add(toFieldError(file, kind, segments.subList(0, segments.size() - 1), segments.getLast(), root));
            } else if (!isStationCoordinateOutOfRange(message)) {
                // the station coordinates out of range are reported by findInvalidStationCoordinates, with the coordinates of the station
                otherErrors.add(error);
            }
        }
        fieldErrors.removeIf(fieldError -> isMaxRangeMetersOfAVehicleTypeWithoutPropulsionType(fieldError, fieldErrors));
        return new Result(fieldErrors, otherErrors);
    }

    /**
     * Finds the stations whose coordinates are invalid: (0, 0), latitude out of [-90, 90] or longitude out of
     * [-180, 180]. The stations whose lat or lon is missing or is not a number are ignored: they are reported by
     * {@link #extract}.
     *
     * @param file      name of the file (station_information.json)
     * @param fileBytes content of the file
     * @return one INVALID_COORDINATES error per station, whose value is "lat, lon" as written in the file
     */
    public static List<GbfsFieldError> findInvalidStationCoordinates(String file, byte[] fileBytes) {
        JsonNode root = readTree(fileBytes);
        JsonNode stations = root != null ? root.path("data").path("stations") : null;
        List<GbfsFieldError> errors = new ArrayList<>();
        if (stations == null || !stations.isArray()) {
            return errors;
        }
        for (int index = 0; index < stations.size(); index++) {
            JsonNode station = stations.get(index);
            JsonNode lat = station.path("lat");
            JsonNode lon = station.path("lon");
            if (!lat.isNumber() || !lon.isNumber()) {
                continue;
            }
            double latitude = lat.doubleValue();
            double longitude = lon.doubleValue();
            boolean nullIsland = latitude == 0 && longitude == 0;
            boolean outOfRange = Math.abs(latitude) > 90 || Math.abs(longitude) > 180;
            if (nullIsland || outOfRange) {
                errors.add(new GbfsFieldError(file, GbfsFieldError.Kind.INVALID_COORDINATES, "station",
                        findEntityId(stations, index, "station_id"), index + 1, "lat, lon", lat.asText() + ", " + lon.asText()));
            }
        }
        return errors;
    }

    private static boolean isStationCoordinateOutOfRange(String message) {
        Matcher matcher = OUT_OF_RANGE.matcher(message);
        return matcher.matches() && STATION_COORDINATE.matcher(matcher.group(1)).matches();
    }

    private static List<String> pathSegments(String path) {
        return Arrays.stream(path.substring(1).split("/"))
                .filter(segment -> !segment.isEmpty())
                .toList();
    }

    /**
     * @param segments segments of the path of the object holding the field (ex: data, stations, 1)
     * @param key      name of the field
     */
    private static GbfsFieldError toFieldError(String file, GbfsFieldError.Kind kind, List<String> segments, String key, JsonNode root) {
        Entity entity = segments.size() >= 3 && "data".equals(segments.get(0)) && INDEX.matcher(segments.get(2)).matches()
                ? ENTITIES.get(segments.get(1))
                : null;
        if (entity == null) {
            List<String> parents = withoutIndexes(segments);
            if (!parents.isEmpty() && "data".equals(parents.get(0))) {
                parents = parents.subList(1, parents.size());
            }
            return new GbfsFieldError(file, kind, null, null, null, toField(parents, key));
        }

        int index = Integer.parseInt(segments.get(2));
        return new GbfsFieldError(file, kind, entity.name(), findEntityId(root, segments.get(1), index, entity.idField()),
                index + 1, toField(withoutIndexes(segments.subList(3, segments.size())), key));
    }

    private static List<String> withoutIndexes(List<String> segments) {
        return segments.stream().filter(segment -> !INDEX.matcher(segment).matches()).toList();
    }

    private static String toField(List<String> parents, String key) {
        return parents.isEmpty() ? key : String.join(".", parents) + "." + key;
    }

    private static String findEntityId(JsonNode root, String list, int index, String idField) {
        return root == null ? null : findEntityId(root.path("data").path(list), index, idField);
    }

    private static String findEntityId(JsonNode objects, int index, String idField) {
        JsonNode id = objects.path(index).path(idField);
        return id.isValueNode() && !id.isNull() && !id.asText().isBlank() ? id.asText() : null;
    }

    private static boolean isMaxRangeMetersOfAVehicleTypeWithoutPropulsionType(GbfsFieldError fieldError,
                                                                               List<GbfsFieldError> fieldErrors) {
        return fieldError.kind() == GbfsFieldError.Kind.MISSING
                && "vehicle_type".equals(fieldError.entity())
                && MAX_RANGE_METERS.equals(fieldError.field())
                && fieldErrors.stream().anyMatch(other -> other.kind() == GbfsFieldError.Kind.MISSING
                && "vehicle_type".equals(other.entity())
                && other.position().equals(fieldError.position())
                && PROPULSION_TYPE.equals(other.field()));
    }

    private static JsonNode readTree(byte[] fileBytes) {
        try {
            return fileBytes != null ? MAPPER.readTree(fileBytes) : null;
        } catch (IOException e) {
            return null;
        }
    }
}
