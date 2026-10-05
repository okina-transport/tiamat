package org.rutebanken.tiamat.rest.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.service.stopplace.StopPlaceMergeCandidateFinder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StopPlaceMergeIdFetcher implements DataFetcher<String> {

    private static final String MERGE_GROUPS_CONTEXT_KEY = "mergeGroupsByNetexId";

    private final StopPlaceMergeCandidateFinder stopPlaceMergeCandidateFinder;

    public StopPlaceMergeIdFetcher(StopPlaceMergeCandidateFinder stopPlaceMergeCandidateFinder) {
        this.stopPlaceMergeCandidateFinder = stopPlaceMergeCandidateFinder;
    }

    @Override
    public String get(DataFetchingEnvironment dataFetchingEnvironment) {
        Object source = dataFetchingEnvironment.getSource();
        if (!(source instanceof StopPlace stopPlace)) {
            return null;
        }
        // Computed once per query (not once per stop place) since it scans the whole database.
        Map<String, String> mergeGroups = dataFetchingEnvironment.getGraphQlContext()
                .computeIfAbsent(MERGE_GROUPS_CONTEXT_KEY, key -> stopPlaceMergeCandidateFinder.computeMergeGroups());
        return stopPlaceMergeCandidateFinder.findMergeId(stopPlace, mergeGroups);
    }
}
