package org.rutebanken.tiamat.externalapis.gbfs;

/**
 * An error on a field of a GBFS file: missing required field, field which is not a number, or invalid coordinates.
 *
 * @param file     name of the file (ex: station_information.json)
 * @param kind     kind of error
 * @param entity   kind of the object holding the field: station, vehicle_type or plan; null when the field is outside
 *                 of these objects (ex: data.timezone in system_information.json)
 * @param entityId id of the object (station_id, vehicle_type_id or plan_id), null when it is itself missing
 * @param position position of the object in its list, starting at 1; null when entity is null
 * @param field    the field, prefixed by its parent fields inside the object (ex: lat, station_area.coordinates)
 * @param value    the value in error, when it is reported to the user (ex: "0, 0" for the coordinates of a station)
 */
public record GbfsFieldError(String file, Kind kind, String entity, String entityId, Integer position, String field,
                             String value) {

    public GbfsFieldError(String file, Kind kind, String entity, String entityId, Integer position, String field) {
        this(file, kind, entity, entityId, position, field, null);
    }

    public enum Kind {
        /**
         * required field missing
         */
        MISSING,
        /**
         * a number is expected (ex: lat, lon, price)
         */
        INVALID_NUMBER,
        /**
         * an integer is expected (ex: capacity, count)
         */
        INVALID_INTEGER,
        /**
         * invalid coordinates of a station: (0, 0), or out of the [-90, 90] / [-180, 180] ranges
         */
        INVALID_COORDINATES
    }
}
