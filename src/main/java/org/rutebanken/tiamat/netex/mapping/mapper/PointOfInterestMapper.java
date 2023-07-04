package org.rutebanken.tiamat.netex.mapping.mapper;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.model.PointOfInterestFacilitySet;
import org.rutebanken.tiamat.model.PointOfInterestOpeningHours;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.rest.graphql.helpers.AvailabilityConditionWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PointOfInterestMapper extends CustomMapper<PointOfInterest, org.rutebanken.tiamat.model.PointOfInterest> {

    final ObjectFactory objectFactory = new ObjectFactory();

    @Override
    public void mapAtoB(PointOfInterest pointOfInterest, org.rutebanken.tiamat.model.PointOfInterest pointOfInterest2, MappingContext context) {
        super.mapAtoB(pointOfInterest, pointOfInterest2, context);
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.PointOfInterest pointOfInterest, PointOfInterest pointOfInterest2, MappingContext context) {
        super.mapBtoA(pointOfInterest, pointOfInterest2, context);

        PostalAddress pa = new PostalAddress();
        Boolean mustAddPostalAddress = false;
        if (pointOfInterest.getZipCode() != null) {
            pa.setPostCode(pointOfInterest.getZipCode());
            mustAddPostalAddress = true;
        }

        if (pointOfInterest.getAddress() != null) {
            pa.setAddressLine1(new MultilingualString().withValue(pointOfInterest.getAddress()));
            mustAddPostalAddress = true;
        }

        if (pointOfInterest.getCity() != null) {
            pa.setTown(new MultilingualString().withValue(pointOfInterest.getCity()));
            mustAddPostalAddress = true;
        }

        if (mustAddPostalAddress) {
            pa.setId("MOBIITI:PostalAddress:" + pointOfInterest.getId());
            pa.setVersion("0");
            pointOfInterest2.setPostalAddress(pa);
        }

        if (pointOfInterest.getPointOfInterestFacilitySet() != null) {
            PointOfInterestFacilitySet pointOfInterestFacilitySet = pointOfInterest.getPointOfInterestFacilitySet();
            if (pointOfInterestFacilitySet != null) {
                SiteFacilitySets_RelStructure siteFacilitySets_relStructure = new SiteFacilitySets_RelStructure();

                SiteFacilitySet siteFacilitySet = new SiteFacilitySet();
                siteFacilitySet.setId(pointOfInterestFacilitySet.getNetexId());
                siteFacilitySet.setVersion(String.valueOf(pointOfInterestFacilitySet.getVersion()));
                siteFacilitySet.getTicketingFacilityList().add(TicketingFacilityEnumeration.fromValue(pointOfInterestFacilitySet.getTicketingFacility().value()));
                siteFacilitySet.getTicketingServiceFacilityList().add(TicketingServiceFacilityEnumeration.fromValue(pointOfInterestFacilitySet.getTicketingServiceFacility().value()));

                siteFacilitySets_relStructure.getSiteFacilitySetRefOrSiteFacilitySet().add(siteFacilitySet);
                pointOfInterest2.setFacilities(siteFacilitySets_relStructure);
            }
        }

        if (pointOfInterest.getPointOfInterestOpeningHours() != null) {
            PointOfInterestOpeningHours pointOfInterestOpeningHours = pointOfInterest.getPointOfInterestOpeningHours();

            ValidityConditions_RelStructure validityConditions_relStructure = new ValidityConditions_RelStructure();

            AvailabilityCondition availabilityCondition = new AvailabilityCondition();
            availabilityCondition.setId("FR:AvailabilityCondition:" + pointOfInterest.getNetexId().split(":")[2]);
            availabilityCondition.setVersion("any");
            availabilityCondition.setIsAvailable(true);

            DayTypes_RelStructure dayTypes_relStructure = objectFactory.createDayTypes_RelStructure();

            dayTypes_relStructure.setId(createDayTypesId(pointOfInterest.getNetexId()));

            ArrayList<org.rutebanken.tiamat.model.DayType> dayTypeListTiamat = new ArrayList<>(pointOfInterestOpeningHours.getDaysType());
            dayTypes_relStructure.withDayTypeRefOrDayType_(dayTypeListTiamat.stream().map(dayType -> {
                DayTypeRefStructure dayType1 = new DayTypeRefStructure();
                dayType1.setRef(dayType.getNetexId());
                return objectFactory.createDayTypeRef(dayType1);
            }).collect(Collectors.toList()));

            availabilityCondition.withDayTypes(dayTypes_relStructure);

            validityConditions_relStructure.withValidityConditionRefOrValidBetweenOrValidityCondition_(objectFactory.createAvailabilityCondition(availabilityCondition));

            pointOfInterest2.setValidityConditions(validityConditions_relStructure);
        }
    }

    private String createDayTypesId(String netexId) {
        String poiId = netexId.split(":")[1];
        return "FR:DayTypes:"+poiId;
    }
}
