package org.rutebanken.tiamat.rest.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class StopPlaceInseeCodeFetcher implements DataFetcher<String> {

    @Override
    public String get(DataFetchingEnvironment dataFetchingEnvironment) {
        Object source = dataFetchingEnvironment.getSource();
        if (!(source instanceof StopPlace stopPlace)) {
            return null;
        }

        String inseeCode = firstNonBlankInseeCode(stopPlace.getQuays());
        if (inseeCode != null) {
            return inseeCode;
        }

        if (stopPlace.isParentStopPlace() && stopPlace.getChildren() != null) {
            for (StopPlace child : stopPlace.getChildren()) {
                inseeCode = firstNonBlankInseeCode(child.getQuays());
                if (inseeCode != null) {
                    return inseeCode;
                }
            }
        }

        return null;
    }

    private String firstNonBlankInseeCode(Set<Quay> quays) {
        if (quays == null) {
            return null;
        }
        return quays.stream()
                .map(Quay::getInseeCode)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);
    }
}
