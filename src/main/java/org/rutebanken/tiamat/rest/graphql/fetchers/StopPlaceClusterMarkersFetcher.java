/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.rest.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.rutebanken.tiamat.auth.StopPlaceAuthorizationService;

import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.rest.dto.DTOClusterMarker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.IGNORE_STOPS;

@Service("stopPlaceClusterMarkersFetcher")
@Transactional
class StopPlaceClusterMarkersFetcher implements DataFetcher {


    private static final Logger logger = LoggerFactory.getLogger(StopPlaceClusterMarkersFetcher.class);

    /**
     * User with role starting with this prefix will be considered as admin => do not filter stop places based on org.
     * TODO : probably already implemented somewhere else, have to find.
     */

    private final StopPlaceRepository stopPlaceRepository;
    private final RoleAssignmentExtractor roleAssignmentExtractor;
    private final StopPlaceAuthorizationService stopPlaceAuthorizationService;


    StopPlaceClusterMarkersFetcher(StopPlaceRepository stopPlaceRepository, RoleAssignmentExtractor roleAssignmentExtractor, StopPlaceAuthorizationService stopPlaceAuthorizationService) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.roleAssignmentExtractor = roleAssignmentExtractor;
        this.stopPlaceAuthorizationService = stopPlaceAuthorizationService;
    }

    @Override
    @Transactional
    public Object get(DataFetchingEnvironment environment) {

        List<String> clientList = stopPlaceAuthorizationService.getFilteredProviders();

        long startTime = System.currentTimeMillis();
        Boolean ignoreStops = environment.getArgument(IGNORE_STOPS);
        if (ignoreStops != null && ignoreStops) { return new PageImpl<>(new ArrayList<>()); }

        logger.info("Searching for StopPlaces with arguments {}", environment.getArguments());

        List<DTOClusterMarker> clusters = stopPlaceRepository.findClusterMarkers(clientList);
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Found {} clusters. duration : {} ms", clusters.size(), duration);





        return clusters;
    }
}
