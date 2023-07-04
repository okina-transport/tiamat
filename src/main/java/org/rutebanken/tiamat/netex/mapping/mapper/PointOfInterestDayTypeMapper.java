//package org.rutebanken.tiamat.netex.mapping.mapper;
//
//import ma.glasnost.orika.CustomMapper;
//import ma.glasnost.orika.MappingContext;
//import org.rutebanken.netex.model.DayType;
//import org.rutebanken.netex.model.General_VersionFrameStructure;
//import org.rutebanken.netex.model.SiteFrame;
//import org.rutebanken.netex.model.Site_VersionFrameStructure;
//import org.springframework.stereotype.Component;
//
//@Component
//public class PointOfInterestDayTypeMapper extends CustomMapper<DayType, org.rutebanken.tiamat.model.DayType> {
//
//
//    @Override
//    public void mapAtoB(DayType dayType, org.rutebanken.tiamat.model.DayType dayType2, MappingContext context) {
//        super.mapAtoB(dayType, dayType2, context);
//    }
//
//    @Override
//    public void mapBtoA(org.rutebanken.tiamat.model.DayType dayType, DayType dayType2, MappingContext context) {
//        super.mapBtoA(dayType, dayType2, context);
//
//        SiteFrame
//
//                    dayTypes_relStructure.withDayTypeRefOrDayType_(pointOfInterestOpeningHours.getDaysType().stream()
//                    .map(dayType -> {
//                        DayType dayType1 = new DayType();
//
//                        dayType1.setId(dayType.getNetexId());
//
////                                PropertiesOfDay_RelStructure propertiesOfDayRelStructure = new PropertiesOfDay_RelStructure();
////                                PropertyOfDay propertyOfDay = new PropertyOfDay();
////                                propertyOfDay.withDaysOfWeek(dayType.getDays());
////                                propertiesOfDayRelStructure.getPropertyOfDay().add(propertyOfDay);
////                                dayType1.setProperties(propertiesOfDayRelStructure);
//
//                        Timebands_RelStructure timebands_relStructure = new Timebands_RelStructure();
//                        timebands_relStructure.setId("FR:timebands:1");
//                        timebands_relStructure.withTimebandRefOrTimeband(dayType.getTimeBand().stream()
//                                .map(timeBand -> {
//                                    Timeband timeband1 = new Timeband();
//                                    timeband1.setId(timeBand.getNetexId());
//                                    timeband1.setVersion("any");
//                                    timeband1.setStartTime(LocalDateTime.ofInstant(timeBand.getStartTime(), ZoneId.of("Europe/Paris")).toLocalTime());
//                                    timeband1.setEndTime(LocalDateTime.ofInstant(timeBand.getEndTime(), ZoneId.of("Europe/Paris")).toLocalTime());
//                                    return timeband1;
//                                }).collect(Collectors.toList()));
//                        dayType1.withTimebands(timebands_relStructure);
//                        return objectFactory.createDayType_(dayType1);
//                    })
//                    .collect(Collectors.toList()));
//
//    }
//}
