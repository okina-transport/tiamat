package org.rutebanken.tiamat.rest.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.rutebanken.tiamat.service.Renamer;
import org.rutebanken.tiamat.service.stopplace.StopPlaceRenamer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.NAME;

@Service("nameRecommendationsFetcher")
@Transactional
public class NameRecommendationsFetcher implements DataFetcher {

    @Autowired
    private Renamer renamer;

    @Override
    @Transactional
    public Object get(DataFetchingEnvironment environment) {
        return renamer.renameIfNeeded(environment.getArgument(NAME));
    }
}
