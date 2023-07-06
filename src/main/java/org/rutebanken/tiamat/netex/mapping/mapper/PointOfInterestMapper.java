package org.rutebanken.tiamat.netex.mapping.mapper;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.model.PointOfInterestFacilitySet;
import org.rutebanken.tiamat.model.PointOfInterestOpeningHours;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

            List<JAXBElement<? extends DayType_VersionStructure>> jaxbElements = dayTypeListTiamat.stream().map(dayType -> {

                DayType dayType2 = objectFactory.createDayType();
                dayType2.setVersion("any");


                dayType2.setId(dayType.getNetexId());


                PropertiesOfDay_RelStructure propertiesOfDayRelStructure = new PropertiesOfDay_RelStructure();
                PropertyOfDay propertyOfDay = new PropertyOfDay();
                propertyOfDay.withDaysOfWeek(DayOfWeekEnumeration.fromValue(dayType.getDays().value()));
                propertiesOfDayRelStructure.getPropertyOfDay().add(propertyOfDay);
                dayType2.setProperties(propertiesOfDayRelStructure);

                Timebands_RelStructure timebands_relStructure = objectFactory.createTimebands_RelStructure();
                timebands_relStructure.setId("FR:timebands:" + pointOfInterest.getNetexId().split(":")[2]);

                dayType.getTimeBand().forEach(timeBand -> {
                            Timeband_VersionedChildStructure timebandVersionedChildStructure = objectFactory.createTimeband_VersionedChildStructure();

                            timebandVersionedChildStructure.setId(timeBand.getNetexId());
                            timebandVersionedChildStructure.setVersion("any");

                            LocalDateTime startTime = LocalDateTime.ofInstant(timeBand.getStartTime(), ZoneId.of("Europe/Paris"));
                            int hours = startTime.getHour();
                            int minutes = startTime.getMinute();
                            timebandVersionedChildStructure.setStartTime(LocalTime.of(hours, minutes));

                            LocalDateTime endTime = LocalDateTime.ofInstant(timeBand.getEndTime(), ZoneId.of("Europe/Paris"));
                            int hoursEnd = endTime.getHour();
                            int minutesEnd = endTime.getMinute();
                            timebandVersionedChildStructure.setEndTime(LocalTime.of(hoursEnd, minutesEnd));
                            timebands_relStructure.withTimebandRefOrTimeband(timebandVersionedChildStructure);
                        });
                dayType2.withTimebands(timebands_relStructure);
                return objectFactory.createDayType(dayType2);
            }).collect(Collectors.toList());

            dayTypes_relStructure.getDayTypeRefOrDayType_().addAll(jaxbElements);

            availabilityCondition.withDayTypes(dayTypes_relStructure);

            validityConditions_relStructure.withValidityConditionRefOrValidBetweenOrValidityCondition_(objectFactory.createAvailabilityCondition(availabilityCondition));

            pointOfInterest2.setValidityConditions(validityConditions_relStructure);
        }
    }

    private String createDayTypesId(String netexId) {
        String poiId = netexId.split(":")[2];
        return "FR:DayTypes:"+poiId;
    }
}