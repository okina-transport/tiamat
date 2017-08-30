package org.rutebanken.tiamat.repository.search;

import org.apache.commons.lang.StringEscapeUtils;
import org.rutebanken.tiamat.exporter.params.ParkingSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ParkingQueryFromSearchBuilder extends SearchBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ParkingQueryFromSearchBuilder.class);


    public Pair<String, Map<String, Object>> buildQueryFromSearch(ParkingSearch parkingSearch) {

        StringBuilder queryString = new StringBuilder("select p.* from parking p ");
        List<String> wheres = new ArrayList<>();
        List<String> operators = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();

        if(parkingSearch != null) {

            if (parkingSearch.getParentSiteRefs() != null && !parkingSearch.getParentSiteRefs().isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder("(");
                int counter = 0;
                for(String parentSiteRef : parkingSearch.getParentSiteRefs()) {
                    stringBuilder.append("'")
                            .append(StringEscapeUtils.escapeSql(parentSiteRef))
                            .append("'");
                    if(++counter < parkingSearch.getParentSiteRefs().size()) {
                        stringBuilder.append(",");
                    }
                }
                stringBuilder.append(")");

                wheres.add("p.parent_site_ref in " + stringBuilder.toString());
            }

            if (!parkingSearch.isAllVersions()) {
                wheres.add("p.version = (select max(pv.version) from parking pv where pv.netex_id = p.netex_id)");
                operators.add("and");
            }
        }
        addWheres(queryString, wheres, operators);

        final String generatedSql = basicFormatter.format(queryString.toString());

        if (logger.isDebugEnabled()) {
            logger.debug("{}", generatedSql);
            logger.debug("params: {}", parameters.toString());
        }
        return Pair.of(generatedSql, parameters);


    }
}
