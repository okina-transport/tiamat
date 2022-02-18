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

@Component
public class MultiModalStopPlaceQueryBuilder {

    private static final Logger logger = LoggerFactory.getLogger(MultiModalStopPlaceQueryBuilder.class);

    //50m ~  0,000449166666667°
    private final double DEFAULT_NEARBY_THRESHOLD = 0.000449166666667;


    @Autowired
    private SearchHelper searchHelper;


    public Pair<String, Map<String, Object>> buildQuery(StopPlaceSearch stopPlaceSearch) {
        Map<String, Object> parameters = generateParametersMap(stopPlaceSearch);
        String selectStatement = generateSelectStatement(stopPlaceSearch);
        String whereStatement = generateWhereStatement(stopPlaceSearch);
        String nearbySelectStatement = generateNearbySelectStatement(stopPlaceSearch);
        String nearbyWhereStatement = generateNearbyWhereStatement(stopPlaceSearch);
        String orderByStatement = generateOrderbyStatement();



        String generatedSql = selectStatement + whereStatement + nearbySelectStatement + nearbyWhereStatement + orderByStatement;


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


        importedIdPattern.append(stopPlaceSearch.getOrganisationName());
        importedIdPattern.append(":stopplace:%");
        return importedIdPattern.toString();
    }

    private String generateNearbySelectStatement(StopPlaceSearch stopPlaceSearch) {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append("  )TMP_STOPS  WHERE exists ( \n" +
                "            SELECT nearby.id \n" +
                "            FROM  stop_place nearby \n");

        return queryBuilder.toString();
    }


    private String generateOrderbyStatement() {
        return " order by\n" +
                "            TMP_STOPS.centroid ,\n" +
                "            TMP_STOPS.netex_id,\n" +
                "            TMP_STOPS.version asc ";
    }

    private String generateNearbyWhereStatement(StopPlaceSearch stopPlaceSearch) {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append("   WHERE \n" +
                "                nearby.netex_id != TMP_STOPS.netex_id  \n" +
                "                AND nearby.parent_stop_place = false  \n" +
                "                AND nearby.stop_place_type != TMP_STOPS.stop_place_type  \n" +
                "                AND ST_Distance(TMP_STOPS.centroid, nearby.centroid) < :nearbyThreshold               \n" +
                "                AND (\n" +
                "                    (                       \n" +
                "                            (\n" +
                "                                nearby.from_date IS NULL AND nearby.to_date IS NULL \n" +
                "                            ) \n" +
                "                            OR (\n" +
                "                                nearby.from_date <= :pointInTime\n" +
                "                                AND ( nearby.to_date IS NULL OR nearby.to_date > :pointInTime  )  \n" +
                "                            )        \n" +
                "                    )               \n" +
                "                )");


        queryBuilder.append(" )");
        return queryBuilder.toString();
    }

    private String generateWhereStatement(StopPlaceSearch stopPlaceSearch) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("  WHERE  s.parent_stop_place = false  AND  s.parent_site_ref IS NULL  AND s.stop_place_type = 'ONSTREET_BUS'  \n" +
                "    AND (            \n" +
                "                    (\n" +
                "                        s.from_date IS NULL \n" +
                "                        AND s.to_date IS NULL\n" +
                "                    ) \n" +
                "                    OR (\n" +
                "                        s.from_date <= :pointInTime\n" +
                "                        AND (\n" +
                "                            s.to_date IS NULL \n" +
                "                            OR s.to_date > :pointInTime\n" +
                "                        )\n" +
                "                    )   \n" +
                "                )  ");

        if (!stopPlaceSearch.getOrganisationName().isEmpty() || !stopPlaceSearch.getQuery().isEmpty()) {

            queryBuilder.append(" AND spkv.key_values_key = 'imported-id' ");

            if (!stopPlaceSearch.getQuery().isEmpty()) {
                queryBuilder.append(" AND ( lower(s.name_value) like :namePattern  or lower(vi.items) like :namePattern )");
            }

            if (!stopPlaceSearch.getOrganisationName().isEmpty()) {
                queryBuilder.append("  AND lower(vi.items) like :importedIdPattern  ");
            }


        }
        return queryBuilder.toString();
    }

    private String generateSelectStatement(StopPlaceSearch stopPlaceSearch) {
        StringBuilder selectBuilder = new StringBuilder();
        selectBuilder.append(" select * FROM      \n" +
                " (     select distinct\n" +
                "        s.*\n" +
                "                from\n" +
                "        stop_place s");

        if (!stopPlaceSearch.getOrganisationName().isEmpty() || !stopPlaceSearch.getQuery().isEmpty()) {
            selectBuilder.append("    LEFT JOIN    stop_place_key_values spkv  on  s.id = spkv.stop_place_id   \n" +
                    "    LEFT JOIN    value_items vi on vi.value_id = spkv.key_values_id       ");


        }

        return selectBuilder.toString();
    }
}
