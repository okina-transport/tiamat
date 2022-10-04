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
import org.locationtech.jts.geom.Envelope;
import org.rutebanken.tiamat.dtoassembling.dto.BoundingBoxDto;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.rest.graphql.GraphQLNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

@Service("pointOfInterestFetcher")
@Transactional
class PointOfInterestFetcher implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(PointOfInterestFetcher.class);

    @Autowired
    private PointOfInterestRepository pointOfInterestRepository;


    @Override
    public Object get(DataFetchingEnvironment environment) {

        PageRequest pageable = PageRequest.of(environment.getArgument(PAGE), environment.getArgument(SIZE));

        Page<PointOfInterest> pointOfInterestPage;

        String pointOfInterestId = environment.getArgument(GraphQLNames.ID);
        Integer version = environment.getArgument(VERSION);

        if (pointOfInterestId != null) {
            List<PointOfInterest> pointOfInterestList = new ArrayList<>();
            if(version != null && version > 0) {
                logger.info("Finding point of interest by netexid {} and version {}", pointOfInterestId, version);
                pointOfInterestList = Arrays.asList(pointOfInterestRepository.findFirstByNetexIdAndVersion(pointOfInterestId, version));
                pointOfInterestPage = new PageImpl<>(pointOfInterestList, pageable, 1L);
            } else {
                logger.info("Finding first poi by netexid {} and highest version", pointOfInterestId);
                pointOfInterestList.add(pointOfInterestRepository.findFirstByNetexIdOrderByVersionDesc(pointOfInterestId));
                pointOfInterestPage = filterOnlyPDV(new PageImpl<>(pointOfInterestList, pageable, 1L));
            }
        } else if (environment.getArgument(LONGITUDE_MIN) != null) {
            BoundingBoxDto boundingBox = new BoundingBoxDto();

            try {
                boundingBox.xMin = ((BigDecimal) environment.getArgument(LONGITUDE_MIN)).doubleValue();
                boundingBox.yMin = ((BigDecimal) environment.getArgument(LATITUDE_MIN)).doubleValue();
                boundingBox.xMax = ((BigDecimal) environment.getArgument(LONGITUDE_MAX)).doubleValue();
                boundingBox.yMax = ((BigDecimal) environment.getArgument(LATITUDE_MAX)).doubleValue();
            } catch (NullPointerException npe) {
                RuntimeException rte = new RuntimeException(MessageFormat.format("{}, {}, {} and {} must all be set when searching within bounding box", LONGITUDE_MIN, LATITUDE_MIN, LONGITUDE_MAX, LATITUDE_MAX));
                rte.setStackTrace(new StackTraceElement[0]);
                throw rte;
            }

            String ignorePointOfInterestId = null;
            if (environment.getArgument(IGNORE_POINT_OF_INTEREST_ID) != null) {
                ignorePointOfInterestId = environment.getArgument(IGNORE_POINT_OF_INTEREST_ID);
            }

            Envelope envelope = new Envelope(boundingBox.xMin, boundingBox.xMax, boundingBox.yMin, boundingBox.yMax);
            pointOfInterestPage = filterOnlyPDV(pointOfInterestRepository.findNearbyPOI(envelope, null, ignorePointOfInterestId, pageable));

        } else if (environment.getArgument(QUERY) != null){
            String query = environment.getArgument(QUERY);
            pointOfInterestPage = filterOnlyPDV(pointOfInterestRepository.findByName(query, pageable));
        } else if (environment.getArgument(POI_CLASSIFICATIONS) != null) {
            List<String> classifications = environment.getArgument(POI_CLASSIFICATIONS);
            pointOfInterestPage = filterOnlyPDV(pointOfInterestRepository.findByClassifications(classifications, pageable));
        }
        else {
            logger.info("Finding all poi regardless of version and validity");
            pointOfInterestPage = filterOnlyPDV(pointOfInterestRepository.findAll(pageable));
        }

        return pointOfInterestPage;
    }

    private Page<PointOfInterest> filterOnlyPDV(Page<PointOfInterest> currentPage) {
        if (currentPage.isEmpty()) {
            return currentPage;
        }

        //On enlève ici tout ce qui n'est pas un pdv
        List<PointOfInterest> pdvFiltered = currentPage.stream().filter(poi -> {
            boolean isShop  = false;
            boolean isTicketMachine = false;
            boolean isPurchase = false;
            for (PointOfInterestClassification classification : poi.getClassifications()) {
                if (classification.getName().getValue().equals("shop") || (classification.getParent() != null && classification.getParent().getName().getValue().equals("shop"))) {
                    isShop = true;
                }
            }

            if (poi.getPointOfInterestFacilitySet() != null && poi.getPointOfInterestFacilitySet().getTicketingFacility().equals(TicketingFacilityEnumeration.TICKET_MACHINES)) {
                isTicketMachine = true;
            }

            if (poi.getPointOfInterestFacilitySet() != null && poi.getPointOfInterestFacilitySet().getTicketingServiceFacility().equals(TicketingServiceFacilityEnumeration.PURCHASE)) {
                isPurchase = true;
            }

            if (isShop && isTicketMachine && isPurchase) {
                return true;
            } else {
                return false;
            }
        }).collect(Collectors.toList());

        Page<PointOfInterest> pdvPage = new PageImpl<PointOfInterest>(pdvFiltered, currentPage.getPageable(), currentPage.getTotalElements());

        return pdvPage;
    }
}
