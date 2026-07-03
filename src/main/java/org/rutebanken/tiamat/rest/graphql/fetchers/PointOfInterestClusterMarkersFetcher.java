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
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.rest.dto.DTOClusterMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.IGNORE_STOPS;

@Service("poiClusterMarkersFetcher")
@Transactional
class PointOfInterestClusterMarkersFetcher implements DataFetcher {


    private static final Logger logger = LoggerFactory.getLogger(PointOfInterestClusterMarkersFetcher.class);



    private final PointOfInterestRepository pointOfInterestRepository;

    private final RoleAssignmentExtractor roleAssignmentExtractor;



    PointOfInterestClusterMarkersFetcher(PointOfInterestRepository pointOfInterestRepository, RoleAssignmentExtractor roleAssignmentExtractor) {
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.roleAssignmentExtractor = roleAssignmentExtractor;

    }

    @Override
    @Transactional
    public Object get(DataFetchingEnvironment environment) {

        long startTime = System.currentTimeMillis();
        Boolean ignoreStops = environment.getArgument(IGNORE_STOPS);
        if (ignoreStops != null && ignoreStops) { return new PageImpl<>(new ArrayList<>()); }

        logger.info("Searching for Poi clusters with arguments {}", environment.getArguments());


        List<DTOClusterMarker> clusters = pointOfInterestRepository.findClusterMarkers();
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Found {} poi clusters. duration : {} ms", clusters.size(), duration);





        return clusters;
    }
}
