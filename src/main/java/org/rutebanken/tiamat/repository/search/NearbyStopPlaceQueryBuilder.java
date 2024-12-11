package org.rutebanken.tiamat.repository.search;

import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.rutebanken.tiamat.repository.utils.NearByStopPlaceQueryConstants.*;

@Component
public class NearbyStopPlaceQueryBuilder {

    private static final Logger logger = LoggerFactory.getLogger(NearbyStopPlaceQueryBuilder.class);

    //50m ~  0,000449166666667°
    private final double DEFAULT_NEARBY_THRESHOLD = 0.000449166666667;


    @Autowired
    private SearchHelper searchHelper;


    public Pair<String, Map<String, Object>> buildNearbyQuery(StopPlaceSearch stopPlaceSearch) {
        Map<String, Object> parameters = generateParametersMap(stopPlaceSearch);


        String generatedSql = WITH_CLAUSE_MAX_STOP_PLACE_VERSION_BY_ID +
                WITH_CLAUSE_STOP_POINT_ONLY_LATEST_VERSION +
                generateWithClauseTargetStop(stopPlaceSearch) +
                WITH_CLAUSE_TARGET_STOP_WITHIN_DISTANCE +
                SELECT_NEARBY_STOP_POINT;


        searchHelper.logIfLoggable(generatedSql, parameters, stopPlaceSearch, logger);
        return Pair.of(generatedSql, parameters);
    }


    private Map<String, Object> generateParametersMap(StopPlaceSearch stopPlaceSearch) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("pointInTime", stopPlaceSearch.getPointInTime() == null ? Date.from(Instant.now()) : Timestamp.from(stopPlaceSearch.getPointInTime()));


        double nearbyThreshold = stopPlaceSearch.getNearbyRadius() != 0 ? getThresholdInDegrees(stopPlaceSearch.getNearbyRadius()) : DEFAULT_NEARBY_THRESHOLD;
        parameters.put("nearbyThreshold", nearbyThreshold);

        if (!stopPlaceSearch.getOrganisationName().isEmpty()) {
            parameters.put("importedIdPattern", generateImportedIdPattern(stopPlaceSearch));
        }

        if (!stopPlaceSearch.getQuery().isEmpty()) {
            parameters.put("namePattern", "%" + stopPlaceSearch.getQuery().toLowerCase() + "%");
        }


        return parameters;
    }

    /**
     * Converts threshold in meters to threshold in degrees
     * (using 50m = 0,000449166666667° base)
     *
     * @param thresholdInMeters the threshold, in meters
     * @return
     */
    private double getThresholdInDegrees(double thresholdInMeters) {
        return thresholdInMeters * 0.000449166666667 / 50;
    }

    private String generateImportedIdPattern(StopPlaceSearch stopPlaceSearch) {
        StringBuilder importedIdPattern = new StringBuilder();


        importedIdPattern.append(stopPlaceSearch.getOrganisationName().toLowerCase());
        importedIdPattern.append(":stopplace:%");
        return importedIdPattern.toString();
    }


    private String generateWithClauseTargetStop(StopPlaceSearch stopPlaceSearch) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("""
            target_stops AS (
              SELECT DISTINCT s.netex_id, s.version, stop_place_type, centroid
              FROM stop_point_without_history s
        """
        );

        if (!stopPlaceSearch.getOrganisationName().isEmpty() || !stopPlaceSearch.getQuery().isEmpty()) {
            queryBuilder.append("""
              LEFT JOIN stop_place_key_values spkv ON s.id = spkv.stop_place_id
              LEFT JOIN value_items vi ON vi.value_id = spkv.key_values_id
            """);
        }
        queryBuilder.append("""
                    WHERE
                        s.parent_stop_place = false
                        AND s.parent_site_ref IS NULL
                        AND (
                              (s.from_date IS NULL AND s.to_date IS NULL)
                              OR
                              (s.from_date <= :pointInTime
                                AND (s.to_date IS NULL OR s.to_date > :pointInTime)
                              )
                        )
                """);
        if (!stopPlaceSearch.getOrganisationName().isEmpty() || !stopPlaceSearch.getQuery().isEmpty()) {

            queryBuilder.append(" AND spkv.key_values_key = 'imported-id' ");

            if (!stopPlaceSearch.getQuery().isEmpty()) {
                queryBuilder.append(" AND ( lower(s.name_value) like :namePattern  or lower(vi.items) like :namePattern )");
            }

            if (!stopPlaceSearch.getOrganisationName().isEmpty()) {
                queryBuilder.append("  AND lower(vi.items) like :importedIdPattern  ");
            }


        }
        queryBuilder.append("),");
        return queryBuilder.toString();
    }

}
