package org.rutebanken.tiamat.repository.search;

import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class StandardStopPlaceSearchQueryBuilder {

    private static final Logger logger = LoggerFactory.getLogger(StandardStopPlaceSearchQueryBuilder.class);

    @Autowired
    private SearchHelper searchHelper;

    public Pair<String, Map<String, Object>> buildQuery(StopPlaceSearch stopPlaceSearch){


        if (stopPlaceSearch.isAllVersions()){
            return buildQueryForAllVersion(stopPlaceSearch);
        }else{
            return buildQueryForSpecificVersion(stopPlaceSearch);
        }


    }

    private Pair<String, Map<String, Object>> buildQueryForSpecificVersion(StopPlaceSearch stopPlaceSearch) {
        Map<String, Object> parameters = generateParametersMap(stopPlaceSearch);
        String queryForSpecificVersion = """                
                WITH latest_versions AS (
                  SELECT netex_id, MAX(version) AS latest_version
                  FROM stop_place
                  WHERE version IS NOT NULL
                  GROUP BY netex_id
                ),
                filtered_stop_places AS (
                  SELECT s.*
                  FROM stop_place s
                  JOIN latest_versions lv ON s.netex_id = lv.netex_id AND s.version = lv.latest_version
                  WHERE NOT s.parent_stop_place
                )
                SELECT s.*
                FROM filtered_stop_places s
                LEFT JOIN stop_place p ON p.netex_id = s.parent_site_ref AND CAST(p.version as text) = CAST(s.parent_site_ref_version as text)
                WHERE s.netex_id IN (:netexIdList)
                   OR p.netex_id IN (:netexIdList)
                ORDER BY s.netex_id, s.version;                
                """;

        searchHelper.logIfLoggable(queryForSpecificVersion, parameters, stopPlaceSearch, logger);
        return Pair.of(queryForSpecificVersion, parameters);
    }

    private Pair<String, Map<String, Object>> buildQueryForAllVersion(StopPlaceSearch stopPlaceSearch) {
        Map<String, Object> parameters = generateParametersMap(stopPlaceSearch);
        String queryForSpecificVersion = """                
                SELECT s.*
                   FROM stop_place s 
                   WHERE s.netex_id IN :netexIdList AND s.parent_stop_place = false
                   UNION
                   SELECT s.*
                   FROM stop_place s
                   LEFT JOIN stop_place p ON s.parent_site_ref = p.netex_id AND s.parent_site_ref_version = CAST(p.version AS text)
                   WHERE p.netex_id IN :netexIdList
                  order by
                        netex_id,
                        version asc
                """;

        searchHelper.logIfLoggable(queryForSpecificVersion, parameters, stopPlaceSearch, logger);
        return Pair.of(queryForSpecificVersion, parameters);
    }

    private Map<String, Object> generateParametersMap(StopPlaceSearch stopPlaceSearch) {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("netexIdList", stopPlaceSearch.getNetexIdList());

        return parameters;

    }
}
