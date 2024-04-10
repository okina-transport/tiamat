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

import com.google.common.base.Preconditions;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.micrometer.core.instrument.util.StringUtils;
import org.locationtech.jts.geom.Point;
import org.rutebanken.helper.organisation.ReflectionAuthorizationService;
import org.rutebanken.tiamat.externalapis.ApiProxyService;
import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.*;
import org.rutebanken.tiamat.rest.graphql.mappers.GeometryMapper;
import org.rutebanken.tiamat.rest.graphql.mappers.GroupOfEntitiesMapper;
import org.rutebanken.tiamat.rest.graphql.mappers.ValidBetweenMapper;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.PointOfInterestVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

@Service("pointOfInterestUpdater")
@Transactional
class PointOfInterestUpdater implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(PointOfInterestUpdater.class);

    @Autowired
    private PointOfInterestRepository pointOfInterestRepository;

    @Autowired
    private PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService;

    @Autowired
    private PointOfInterestClassificationRepository pointOfInterestClassificationRepository;

    @Autowired
    private PointOfInterestFacilitySetRepository pointOfInterestFacilitySetRepository;

    @Autowired
    private GeometryMapper geometryMapper;

    @Autowired
    private ReflectionAuthorizationService authorizationService;

    @Autowired
    private ValidBetweenMapper validBetweenMapper;

    @Autowired
    private VersionCreator versionCreator;

    @Autowired
    private GroupOfEntitiesMapper groupOfEntitiesMapper;

    private ApiProxyService apiProxyService = new ApiProxyService();


    @Override
    public Object get(DataFetchingEnvironment environment) {

        List<Map> input = environment.getArgument(OUTPUT_TYPE_POINT_OF_INTEREST);
        List<PointOfInterest> pointsOfInterest = null;
        if (input != null) {
            pointsOfInterest = input.stream()
             .map(this::createOrUpdatePointOfInterest)
            .collect(Collectors.toList());
        }
        return pointsOfInterest;
    }

    private PointOfInterest createOrUpdatePointOfInterest(Map input){
        PointOfInterest updatedPointOfInterest;
        PointOfInterest existingVersion = null;
        String netexId = (String) input.get(ID);
        if (netexId != null) {
            logger.info("Updating Point Of Interest {}", netexId);
            existingVersion = pointOfInterestRepository.findFirstByNetexIdOrderByVersionDesc(netexId);
            Preconditions.checkArgument(existingVersion != null, "Attempting to update Point of Interest [id = %s], but Point of Interest does not exist.", netexId);
            updatedPointOfInterest = versionCreator.createCopy(existingVersion, PointOfInterest.class);

        } else {
            logger.info("Creating new PointOfInterest");
            updatedPointOfInterest = new PointOfInterest();
        }

        boolean isUpdated = populatePointOfInterest(input, updatedPointOfInterest);
        if (isUpdated) {
            authorizationService.assertAuthorized(ROLE_EDIT_STOPS, Arrays.asList(existingVersion, updatedPointOfInterest));

            logger.info("Saving new version of point of interest {}", updatedPointOfInterest);
            updatedPointOfInterest = pointOfInterestVersionedSaverService.saveNewVersion(updatedPointOfInterest);

            return updatedPointOfInterest;
        } else {
            logger.info("No changes - Point Of Interest {} NOT updated", netexId);
        }
        return existingVersion;
    }

    private boolean populatePointOfInterest(Map input, PointOfInterest updatedPointOfInterest) {
        boolean isUpdated = false;
        if (input.get(VALID_BETWEEN) != null) {
            updatedPointOfInterest.setValidBetween(validBetweenMapper.map((Map) input.get(VALID_BETWEEN)));
            isUpdated = true;
        }

        if (input.get(GEOMETRY) != null) {
            Point geoJsonPoint = geometryMapper.createGeoJsonPoint((Map) input.get(GEOMETRY));
            isUpdated = isUpdated || (!geoJsonPoint.equals(updatedPointOfInterest.getCentroid()));
            updatedPointOfInterest.setCentroid(geoJsonPoint);

            try {
                DtoGeocode dtoGeocode = apiProxyService.getGeocodeDataByReverseGeocoding(BigDecimal.valueOf(geoJsonPoint.getCoordinate().y), BigDecimal.valueOf(geoJsonPoint.getCoordinate().x));
                if (StringUtils.isNotEmpty(dtoGeocode.getAddress())) {
                    updatedPointOfInterest.setAddress(dtoGeocode.getAddress());
                }

                if (StringUtils.isNotEmpty(dtoGeocode.getCity()))  {
                    updatedPointOfInterest.setCity(dtoGeocode.getCity());
                }

                if (StringUtils.isNotEmpty(dtoGeocode.getCity())) {
                    updatedPointOfInterest.setPostalCode(dtoGeocode.getPostCode());
                }

                if (StringUtils.isNotEmpty(dtoGeocode.getCityCode())){
                    updatedPointOfInterest.setZipCode(dtoGeocode.getCityCode());
                }
            } catch (Exception e) {
                logger.error("Unable to get zip code for poi:" + updatedPointOfInterest.getNetexId());
            }
        }

        if (input.get(PARENT_SITE_REF) != null) {
            SiteRefStructure parentSiteRef = new SiteRefStructure();
            parentSiteRef.setRef((String) input.get(PARENT_SITE_REF));

            isUpdated = isUpdated || (!parentSiteRef.equals(updatedPointOfInterest.getParentSiteRef()));

            updatedPointOfInterest.setParentSiteRef(parentSiteRef);
        }

        if (input.get(POI_FACILITY_SET) != null) {
            Map facilitySet = (Map) input.get(POI_FACILITY_SET);

            if (facilitySet.containsKey("ticketingFacility")){
                TicketingFacilityEnumeration ticketingFacility = (TicketingFacilityEnumeration) facilitySet.get("ticketingFacility");
                TicketingServiceFacilityEnumeration ticketingServiceFacility = (TicketingServiceFacilityEnumeration) facilitySet.get("ticketingServiceFacility");
                PointOfInterestFacilitySet existingFacilitySet = pointOfInterestFacilitySetRepository.findFirstByNetexIdOrderByVersionDesc(updatedPointOfInterest.getPointOfInterestFacilitySet().getNetexId());

                if (existingFacilitySet != null && !existingFacilitySet.getTicketingFacility().equals(ticketingFacility)) {
                    existingFacilitySet.setTicketingFacility(ticketingFacility);
                    isUpdated = true;
                }

                if (existingFacilitySet != null && !existingFacilitySet.getTicketingServiceFacility().equals(ticketingServiceFacility)) {
                    existingFacilitySet.setTicketingServiceFacility(ticketingServiceFacility);
                    isUpdated = true;
                }
                updatedPointOfInterest.setPointOfInterestFacilitySet(existingFacilitySet);
            }

        }

        if (input.get(POI_CLASSIFICATIONS) != null) {
            //Pour le moment la maj des classifications n'est pas possible => on garde la liste existante

            Set<PointOfInterestClassification> existingPointOfInterestClassifications = new HashSet<>();
            for (PointOfInterestClassification updatedClassification : updatedPointOfInterest.getClassifications()) {
                PointOfInterestClassification existingClassification = pointOfInterestClassificationRepository.findFirstByNetexIdOrderByVersionDesc(updatedClassification.getNetexId());
                if (existingClassification != null) {
                    existingPointOfInterestClassifications.add(existingClassification);
                }
            }
            updatedPointOfInterest.setClassifications(existingPointOfInterestClassifications);
        }

        if (input.get(POI_OPENING_HOURS) != null) {
            PointOfInterestOpeningHours pointOfInterestOpeningHours = mapPointOfInterestExistingHours((Map) input.get("pointOfInterestOpeningHours"),(String) input.get("id"));
            updatedPointOfInterest.setPointOfInterestOpeningHours(pointOfInterestOpeningHours);
            isUpdated = true;
        }

        isUpdated = isUpdated | groupOfEntitiesMapper.populate(input, updatedPointOfInterest);

        return isUpdated;
    }

    private PointOfInterestOpeningHours mapPointOfInterestExistingHours(Map<String, Object> pointOfInterestValidityConditionSet, String poiId) {

        String poiNetexId = poiId.trim().split(":")[2];

        PointOfInterestOpeningHours pointOfInterestOpeningHours = new PointOfInterestOpeningHours();
        Set<DayType> dayTypes = pointOfInterestValidityConditionSet.entrySet().stream()
                .map(entry -> {
                    DayType dayType = new DayType();
                    Set<TimeBand> timeBands = new HashSet<>();
                    dayType.setDays(DayOfWeekEnumeration.fromValue(entry.getKey()));
                    String day = dayType.getDays().value();
                    dayType.setNetexId("FR:DayType:" + day + "_" + poiNetexId);
                    Map<?,?> value = (Map<?, ?>) entry.getValue();
                    if(value.get("facility").equals("Journée")){
                        TimeBand timeBand = new TimeBand();
                        timeBand.setEndTime(Instant.parse((CharSequence) value.get("endTime")));
                        timeBand.setStartTime(Instant.parse((CharSequence) value.get("startTime")));
                        timeBand.setNetexId(createTimeBandId(day, "", poiNetexId));
                        timeBands.add(timeBand);
                    } else if(value.get("facility").equals("Demi journée")){
                        TimeBand timeBand = new TimeBand();
                        timeBand.setEndTime(Instant.parse((CharSequence) value.get("endTimeAm")));
                        timeBand.setStartTime(Instant.parse((CharSequence) value.get("startTimeAm")));
                        timeBand.setNetexId(createTimeBandId(day, "_am", poiNetexId));
                        timeBands.add(timeBand);

                        TimeBand timeBand2 = new TimeBand();
                        timeBand2.setEndTime(Instant.parse((CharSequence) value.get("endTimePm")));
                        timeBand2.setStartTime(Instant.parse((CharSequence) value.get("startTimePm")));
                        timeBand2.setNetexId(createTimeBandId(day, "_pm", poiNetexId));
                        timeBands.add(timeBand2);
                    }

                    dayType.setTimeBand(timeBands);
                    return dayType;
                }).collect(Collectors.toSet());

        pointOfInterestOpeningHours.setDaysType(dayTypes);
        return pointOfInterestOpeningHours;
    }

    private String createTimeBandId(String day, String suffix, String poiNetexId){
        return "FR:TimeBand:" + day + suffix + "_" + poiNetexId;
    }
}
