package org.rutebanken.tiamat.rest.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.rutebanken.tiamat.service.stopplace.StopPlaceRenamer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.NAME;

@Service("stopPlaceNameRecommendationsFetcher")
@Transactional
public class StopPlaceNameRecommendationsFetcher implements DataFetcher {
    @Autowired
    private StopPlaceRenamer stopPlaceRenamer;

    @Override
    @Transactional
    public Object get(DataFetchingEnvironment environment) {
        return stopPlaceRenamer.renameIfNeeded(environment.getArgument(NAME));
    }
}
