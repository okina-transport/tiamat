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
public class StopPlaceWithMultipleProducersQueryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(StopPlaceWithMultipleProducersQueryBuilder.class);


    @Autowired
    private SearchHelper searchHelper;


    public Pair<String, Map<String, Object>> buildQuery(StopPlaceSearch stopPlaceSearch) {
        Map<String, Object> parameters = generateParametersMap(stopPlaceSearch);
        String selectStatement = generateSelectStatement(stopPlaceSearch);
        String whereStatement = generateWhereStatement(stopPlaceSearch);
        String orderByStatement = generateOrderbyStatement();
        String withStatement = generateWithStatement();

        String generatedSql =  withStatement + selectStatement + whereStatement + orderByStatement;

        searchHelper.logIfLoggable(generatedSql, parameters, stopPlaceSearch, logger);
        return Pair.of(generatedSql, parameters);
    }


    private Map<String, Object> generateParametersMap(StopPlaceSearch stopPlaceSearch) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("pointInTime", stopPlaceSearch.getPointInTime() == null ? Date.from(Instant.now()) : Timestamp.from(stopPlaceSearch.getPointInTime()));

        if (!stopPlaceSearch.getOrganisationName().isEmpty()) {
            parameters.put("importedIdPattern", generateImportedIdPattern(stopPlaceSearch));
        }

        if (!stopPlaceSearch.getQuery().isEmpty()) {
            parameters.put("namePattern", "%" + stopPlaceSearch.getQuery().toLowerCase() + "%");
        }

        return parameters;
    }

    private String generateImportedIdPattern(StopPlaceSearch stopPlaceSearch) {
        return stopPlaceSearch.getOrganisationName() + ":stopplace:%";
    }


    private String generateSelectStatement(StopPlaceSearch stopPlaceSearch) {
        StringBuilder selectBuilder = new StringBuilder();
        selectBuilder.append(" SELECT * FROM      \n" +
                "      \n" +
                "(     SELECT DISTINCT\n" +
                "        s.*\n" +
                "                FROM\n" +
                "        stop_place s  ");

        if (!stopPlaceSearch.getOrganisationName().isEmpty() || !stopPlaceSearch.getQuery().isEmpty()) {
            selectBuilder.append("    LEFT JOIN    stop_place_key_values spkv  ON  s.id = spkv.stop_place_id   \n" +
                    "    LEFT JOIN    value_items vi ON vi.value_id = spkv.key_values_id       ");


        }


        return selectBuilder.toString();
    }

    private String generateWhereStatement(StopPlaceSearch stopPlaceSearch) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("  WHERE  s.parent_stop_place = false \n" +
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
                queryBuilder.append(" AND ( lower(s.name_value) LIKE :namePattern OR lower(vi.items) LIKE :namePattern )");
            }

            if (!stopPlaceSearch.getOrganisationName().isEmpty()) {
                queryBuilder.append("  AND lower(vi.items) LIKE lower(:importedIdPattern)  ");
            }
        }

        queryBuilder.append("  )TMP_STOPS WHERE EXISTS (           " +
                "               SELECT 1 FROM stop_place sp1 WHERE sp1.netex_id in " +
                "               (SELECT netex_id FROM sp_with_multiple_producers GROUP BY netex_id HAVING count(producer) > 1)" +
                "               AND sp1.netex_id = TMP_STOPS.netex_id ) ");


        return queryBuilder.toString();
    }

    private String generateWithStatement() {
        return "WITH sp_with_multiple_producers AS (\n" +
                "SELECT DISTINCT(SPLIT_PART(vi.items,':',1)) AS producer, sp.netex_id AS netex_id FROM stop_place sp \n" +
                "LEFT JOIN stop_place_key_values spkv ON sp.id = spkv.stop_place_id \n" +
                "LEFT JOIN value_items vi ON vi.value_id = spkv.key_values_id \n" +
                "WHERE spkv.key_values_key = 'imported-id')\n";
    }

    private String generateOrderbyStatement() {
        return " order by\n" +
                "            TMP_STOPS.centroid ,\n" +
                "            TMP_STOPS.netex_id,\n" +
                "            TMP_STOPS.version asc ";
    }
}
