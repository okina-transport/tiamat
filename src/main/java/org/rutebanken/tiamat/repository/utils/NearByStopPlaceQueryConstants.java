package org.rutebanken.tiamat.repository.utils;

public class NearByStopPlaceQueryConstants {

    public static final String WITH_CLAUSE_MAX_STOP_PLACE_VERSION_BY_ID = """
            WITH latest_stop_point_reference AS (
              SELECT netex_id, MAX(version) AS max_version
              FROM stop_place
              GROUP BY netex_id
            ),
            """;

    public static final String WITH_CLAUSE_STOP_POINT_ONLY_LATEST_VERSION = """
            stop_point_without_history AS (
              SELECT sp.*
              FROM stop_place sp
              JOIN latest_stop_point_reference ON
                sp.netex_id = latest_stop_point_reference.netex_id
                AND sp.version = latest_stop_point_reference.max_version
            ),
            """;

    public static final String WITH_CLAUSE_TARGET_STOP_WITHIN_DISTANCE = """
            corresponding_stops_id AS (
              SELECT *
              FROM target_stops
              WHERE EXISTS (
                SELECT nearby.id
                FROM stop_place nearby
                WHERE
                  nearby.netex_id != target_stops.netex_id
                  AND nearby.parent_stop_place = false
                  AND nearby.stop_place_type = target_stops.stop_place_type
                  AND ST_DWithin(target_stops.centroid, nearby.centroid, :nearbyThreshold)
                  AND (
                    (nearby.from_date IS NULL AND nearby.to_date IS NULL)
                      OR
                    (nearby.from_date <= :pointInTime
                      AND (nearby.to_date IS NULL OR nearby.to_date > :pointInTime)
                    )
                  )
              )
            )
            """;

    public static final String SELECT_NEARBY_STOP_POINT = """
            SELECT s2.* FROM stop_place s2
            JOIN corresponding_stops_id ON
              s2.netex_id = corresponding_stops_id.netex_id
              AND s2.version = corresponding_stops_id.version
            ORDER BY s2.centroid, s2.netex_id, s2.version ASC
            """;

    private NearByStopPlaceQueryConstants() {
        throw new IllegalStateException("Utility class");
    }
}
