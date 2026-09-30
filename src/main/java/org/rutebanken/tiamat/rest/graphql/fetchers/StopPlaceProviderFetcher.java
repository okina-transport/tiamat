package org.rutebanken.tiamat.rest.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.rutebanken.tiamat.model.StopPlace;
import org.springframework.stereotype.Component;

@Component
public class StopPlaceProviderFetcher implements DataFetcher<String> {

    @Override
    public String get(DataFetchingEnvironment dataFetchingEnvironment) {
        Object source = dataFetchingEnvironment.getSource();
        if (!(source instanceof StopPlace stopPlace)) {
            return null;
        }
        return stopPlace.getProvider();
    }
}
