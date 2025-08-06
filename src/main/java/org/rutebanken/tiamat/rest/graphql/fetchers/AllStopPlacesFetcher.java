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
import org.jetbrains.annotations.NotNull;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.rutebanken.tiamat.auth.StopPlaceAuthorizationService;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.stopplace.ParentStopPlacesFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.IGNORE_STOPS;

@Service("allStopPlacesFetcher")
@Transactional
class AllStopPlacesFetcher implements DataFetcher {


    private static final Logger logger = LoggerFactory.getLogger(AllStopPlacesFetcher.class);

    private static final Page<StopPlace> EMPTY_STOPS_RESULT = new PageImpl<>(new ArrayList<>());

    /**
     * Whether to keep children when resolving parent stop places. False, because with graphql it's possible to fetch children from parent.
     */
    private static final boolean KEEP_CHILDREN = false;

    /**
     * User with role starting with this prefix will be considered as admin => do not filter stop places based on org.
     * TODO : probably already implemented somewhere else, have to find.
     */

    private final StopPlaceRepository stopPlaceRepository;
    private final ParentStopPlacesFetcher parentStopPlacesFetcher;
    private final RoleAssignmentExtractor roleAssignmentExtractor;
    private final StopPlaceAuthorizationService stopPlaceAuthorizationService;


    AllStopPlacesFetcher(StopPlaceRepository stopPlaceRepository, ParentStopPlacesFetcher parentStopPlacesFetcher, RoleAssignmentExtractor roleAssignmentExtractor, StopPlaceAuthorizationService stopPlaceAuthorizationService) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.parentStopPlacesFetcher = parentStopPlacesFetcher;
        this.roleAssignmentExtractor = roleAssignmentExtractor;
        this.stopPlaceAuthorizationService = stopPlaceAuthorizationService;
    }

    @Override
    @Transactional
    public Object get(DataFetchingEnvironment environment) {
        List<String> userOrgs = roleAssignmentExtractor.getRoleAssignmentsForUser().stream().map(RoleAssignment::getOrganisation).collect(Collectors.toList());
        List<String> clientList = stopPlaceAuthorizationService.getFilteredProviders();
        

        Boolean ignoreStops = environment.getArgument(IGNORE_STOPS);
        if (ignoreStops != null && ignoreStops) { return new PageImpl<>(new ArrayList<>()); }

        logger.info("Searching for StopPlaces with arguments {}", environment.getArguments());
        logger.info("User organisations : {}", userOrgs);

        List<StopPlace> stopPlaces = stopPlaceRepository.findAllStopplacesLastVersionAndValid(clientList);

        Map<String, Long> nbStopPlaces = stopPlaces.stream()
                .collect(Collectors.groupingBy(StopPlace::getNetexId, Collectors.counting()));

        nbStopPlaces.entrySet().stream().filter(e -> e.getValue() > 1).forEach(e -> logger.info("Nombre de doublons : ", e.getKey() + " " + e.getValue()   ));



        return stopPlaces;
    }
}
