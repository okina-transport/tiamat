package org.rutebanken.tiamat.repository.search;

import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

@Component
public class SearchStopPlacesWithDistantQuaysBuilder{

    private static final Logger logger = LoggerFactory.getLogger(SearchStopPlacesWithDistantQuaysBuilder.class);

    //50m ~  0,000449166666667°
    private final double DEFAULT_NEARBY_THRESHOLD = 0.000898333;

    @Autowired
    private SearchHelper searchHelper;

    public Pair <String, Map<String, Object>> buildQuery(StopPlaceSearch stopPlaceSearch){

        Map <String, Object> parameters = generateParametersMap(stopPlaceSearch);
        String selectStatement = generateSelectStatement(stopPlaceSearch);
        String whereStatement =  generateWhereStatement(stopPlaceSearch);
        String organisationNameFilter = generateOrganisationNameFilter(stopPlaceSearch);
        String orderByStatement = generateOrderByStatement(stopPlaceSearch);

        String generatedSql = selectStatement + whereStatement + organisationNameFilter + orderByStatement;

        searchHelper.logIfLoggable(generatedSql, parameters, stopPlaceSearch, logger);
        return Pair.of(generatedSql, parameters);
    }

    private Map<String, Object> generateParametersMap(StopPlaceSearch stopPlaceSearch){

        Map <String, Object> parameters = new HashMap<>();

        parameters.put("pointInTime", stopPlaceSearch.getPointInTime() == null ? Date.from(Instant.now()) : Timestamp.from(stopPlaceSearch.getPointInTime()));

        double nearbyThreshold = stopPlaceSearch.getNearbyRadius() != 0 ? getThresholdInDegrees(stopPlaceSearch.getNearbyRadius()) : DEFAULT_NEARBY_THRESHOLD;
        parameters.put("nearbyThreshold", nearbyThreshold);

        if (!stopPlaceSearch.getOrganisationName().isEmpty()) {
            parameters.put("importedIdPattern", generateImportedIdPattern(stopPlaceSearch));
        }

        return parameters;
    }

    private String generateSelectStatement(StopPlaceSearch stopPlaceSearch){

        StringBuilder selectBuilder = new StringBuilder();
        selectBuilder.append("SELECT DISTINCT sp.* FROM stop_place sp  ").append(System.lineSeparator());
        selectBuilder.append("LEFT JOIN stop_place_quays spq ON spq.stop_place_id = sp.id").append(System.lineSeparator());
        selectBuilder.append("LEFT JOIN quay q ON q.id = spq.quays_id").append(System.lineSeparator());
        return selectBuilder.toString();
    }

    private String generateWhereStatement(StopPlaceSearch stopPlaceSearch){

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("WHERE sp.parent_stop_place = false").append(System.lineSeparator());
        queryBuilder.append("AND(\n"+
                "   sp.id IS NOT NULL \n" +
                "   AND (\n" +
                "       (\n" +
                "           sp.from_date IS NULL \n" +
                "           AND sp.to_date IS NULL\n" +
                "        ) \n" +
                "           OR (" +
                "               sp.from_date <= :pointInTime \n" +
                "               AND (\n" +
                "                   sp.to_date IS NULL \n" +
                "                   OR sp.to_date > :pointInTime \n" +
                "                   )\n" +
                "               )\n" +
                "           )\n" +
                "       )\n" +
                "AND EXISTS (\n" +
                "   SELECT * FROM quay quay_nearby\n" +
                "   INNER JOIN stop_place_quays spq_nearby ON spq_nearby.quays_id = quay_nearby.id \n" +
                "   WHERE spq.stop_place_id = spq_nearby.stop_place_id \n" +
                "   AND ST_Distance(q.centroid, quay_nearby.centroid) > :nearbyThreshold\n" +
                ")\n"

        );
        return queryBuilder.toString();
    }

    private String generateOrganisationNameFilter(StopPlaceSearch stopPlaceSearch){

        StringBuilder organisationNameFilter = new StringBuilder();

        if (!stopPlaceSearch.getOrganisationName().isEmpty() || !stopPlaceSearch.getQuery().isEmpty()) {

            organisationNameFilter.append("AND EXISTS(\n" +
                    "   SELECT 1 FROM stop_place_key_values spkv \n" +
                    "   INNER JOIN value_items vi ON vi.value_id = spkv.key_values_id   \n" +
                    "   WHERE spkv.key_values_key= 'imported-id'\n" +
                    "   AND sp.id = spkv.stop_place_id  \n" +
                    "   AND lower(vi.items) LIKE concat('%', lower(''), '%')\n" +
                    "   AND vi.items LIKE :importedIdPattern\n"+
                    ")\n"
            );
        }
        return organisationNameFilter.toString();
    }
    private String generateOrderByStatement(StopPlaceSearch stopPlaceSearch){
        return "ORDER BY sp.id";
    }


    private String generateImportedIdPattern(StopPlaceSearch stopPlaceSearch) {
        StringBuilder importedIdPattern = new StringBuilder();

        importedIdPattern.append(stopPlaceSearch.getOrganisationName().toUpperCase());
        importedIdPattern.append(":StopPlace:%");
        return importedIdPattern.toString();
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

}
