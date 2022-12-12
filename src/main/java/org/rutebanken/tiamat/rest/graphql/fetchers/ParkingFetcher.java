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
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.rest.graphql.GraphQLNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

@Service("parkingFetcher")
@Transactional
class ParkingFetcher implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(ParkingFetcher.class);

    @Autowired
    private ParkingRepository parkingRepository;


    @Override
    public Object get(DataFetchingEnvironment environment) {

        PageRequest pageable = PageRequest.of(environment.getArgument(PAGE), environment.getArgument(SIZE));

        Page<Parking> parkingPage;

        String stopPlaceId = environment.getArgument(FIND_BY_STOP_PLACE_ID);

        Boolean ignoreParking = environment.getArgument(IGNORE_PARKINGS);
        if (ignoreParking != null && ignoreParking) { return new PageImpl<>(new ArrayList<>()); }

        String parkingId = environment.getArgument(GraphQLNames.ID);
        Integer version = environment.getArgument(VERSION);

        if (parkingId != null) {
            List<Parking> parkingList = new ArrayList<>();
            if(version != null && version > 0) {
                logger.info("Finding parking by netexid {} and version {}", parkingId, version);
                parkingList = Arrays.asList(parkingRepository.findFirstByNetexIdAndVersion(parkingId, version));
                parkingPage = new PageImpl<>(parkingList, pageable, 1L);
            } else {
                logger.info("Finding first parking by netexid {} and highest version", parkingId);
                parkingList.add(parkingRepository.findFirstByNetexIdOrderByVersionDesc(parkingId));
                parkingPage = new PageImpl<>(parkingList, pageable, 1L);
            }
        } else if (stopPlaceId != null) {
            logger.info("Finding parkings by stop place netexid {}", stopPlaceId);
            return parkingRepository.findByStopPlaceNetexId(stopPlaceId).stream()
                    .peek(parkingNetexId -> logger.info("Finding parking by netexid {} and highest version", parkingNetexId))
                    .map(netexId -> parkingRepository.findFirstByNetexIdOrderByVersionDesc(netexId))
                    .collect(Collectors.toList());
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

            String ignoreParkingId = null;
            if (environment.getArgument(IGNORE_PARKING_ID) != null) {
                ignoreParkingId = environment.getArgument(IGNORE_PARKING_ID);
            }

            Envelope envelope = new Envelope(boundingBox.xMin, boundingBox.xMax, boundingBox.yMin, boundingBox.yMax);


            parkingPage = parkingRepository.findNearbyParking(envelope, null, null, ignoreParkingId, pageable);
        } else if (environment.getArgument(QUERY) != null){
            String query = environment.getArgument(QUERY);
            parkingPage = parkingRepository.findByName(query, pageable);
        }
        else {
            logger.info("Finding all parkings regardless of version and validity");
            parkingPage = parkingRepository.findAll(pageable);
        }

        return parkingPage;
    }
}
