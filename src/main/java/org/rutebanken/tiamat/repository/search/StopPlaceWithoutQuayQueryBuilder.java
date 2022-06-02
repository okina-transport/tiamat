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
public class StopPlaceWithoutQuayQueryBuilder {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceWithoutQuayQueryBuilder.class);


    @Autowired
    private SearchHelper searchHelper;


    public Pair<String, Map<String, Object>> buildQuery(StopPlaceSearch stopPlaceSearch) {
        Map<String, Object> parameters = generateParametersMap(stopPlaceSearch);
        String selectStatement = generateSelectStatement(stopPlaceSearch);
        String whereStatement = generateWhereStatement(stopPlaceSearch);
        String orderByStatement = generateOrderbyStatement();

        String generatedSql = selectStatement + whereStatement + orderByStatement;

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
        StringBuilder importedIdPattern = new StringBuilder();

        importedIdPattern.append(stopPlaceSearch.getOrganisationName());
        importedIdPattern.append(":stopplace:%");
        return importedIdPattern.toString();
    }


    private String generateSelectStatement(StopPlaceSearch stopPlaceSearch) {
        StringBuilder selectBuilder = new StringBuilder();
        selectBuilder.append(" select * FROM      \n" +
                "      \n" +
                "(     select distinct\n" +
                "        s.*\n" +
                "                from\n" +
                "        stop_place s  ");

        if (!stopPlaceSearch.getOrganisationName().isEmpty() || !stopPlaceSearch.getQuery().isEmpty()) {
            selectBuilder.append("    LEFT JOIN    stop_place_key_values spkv  on  s.id = spkv.stop_place_id   \n" +
                    "    LEFT JOIN    value_items vi on vi.value_id = spkv.key_values_id       ");


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
                queryBuilder.append(" AND ( lower(s.name_value) like :namePattern  or lower(vi.items) like :namePattern )");
            }

            if (!stopPlaceSearch.getOrganisationName().isEmpty()) {
                queryBuilder.append("  AND lower(vi.items) like lower(:importedIdPattern)  ");
            }
        }

        queryBuilder.append("  )TMP_STOPS  WHERE  not exists (           " +
                "               SELECT 1 FROM stop_place_quays spq WHERE spq.stop_place_id =  TMP_STOPS.id ) ");


        return queryBuilder.toString();
    }

    private String generateOrderbyStatement() {
        return " order by\n" +
                "            TMP_STOPS.centroid ,\n" +
                "            TMP_STOPS.netex_id,\n" +
                "            TMP_STOPS.version asc ";
    }
}
