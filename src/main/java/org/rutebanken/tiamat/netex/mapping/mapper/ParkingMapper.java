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

package org.rutebanken.tiamat.netex.mapping.mapper;

import jakarta.xml.bind.JAXBElement;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.ParkingEntrance;
import org.rutebanken.tiamat.model.SpecificParkingAreaUsageEnumeration;
import org.rutebanken.tiamat.model.TimeBand;
import org.rutebanken.tiamat.netex.NetexUtils;

import java.math.BigInteger;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.rutebanken.tiamat.netex.id.NetexIdHelper.*;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

public class ParkingMapper extends CustomMapper<Parking, org.rutebanken.tiamat.model.Parking> {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    private static final Pattern patternStreetNumber = Pattern.compile("^(\\d+)\\s*(.*)$");

    private final String superId;

    public ParkingMapper(String superId) {
        this.superId = superId;
    }

    private static void mapAppDownloadUrl(org.rutebanken.tiamat.model.Parking tiamatParking, Parking netexParking) {
        if (StringUtils.isNotBlank(tiamatParking.getRentalUriAndroid())) {
            PaymentByMobileStructure paymentByMobileStructure = new PaymentByMobileStructure();
            paymentByMobileStructure.setPaymentAppDownloadUrl(tiamatParking.getRentalUriAndroid());
            netexParking.setPaymentByMobile(paymentByMobileStructure);
        } else if (StringUtils.isNotBlank(tiamatParking.getRentalUriIos())) {
            PaymentByMobileStructure paymentByMobileStructure = new PaymentByMobileStructure();
            paymentByMobileStructure.setPaymentAppDownloadUrl(tiamatParking.getRentalUriIos());
            netexParking.setPaymentByMobile(paymentByMobileStructure);
        }
    }

    private void mapAddress(org.rutebanken.tiamat.model.Parking tiamatParking, Parking netexParking) {
        if (tiamatParking.getInsee() != null || tiamatParking.getAddress() != null) {
            PostalAddress netexPostalAddress = new PostalAddress();
            netexPostalAddress.setId(superId + ":PostalAddress:" + UUID.randomUUID());
            netexPostalAddress.setVersion("any");
            if (tiamatParking.getAddress() != null) {
                // Expression régulière pour capturer les premiers chiffres au début de l'adresse
                Matcher matcher = patternStreetNumber.matcher(tiamatParking.getAddress());

                if (matcher.matches()) {
                    netexPostalAddress.setHouseNumber(matcher.group(1));
                    netexPostalAddress.setStreet(new MultilingualString().withValue(matcher.group(2)));
                } else {
                    netexPostalAddress.setStreet(new MultilingualString().withValue(tiamatParking.getAddress()));
                }
            }
            if (tiamatParking.getInsee() != null) {
                netexPostalAddress.setPostalRegion(tiamatParking.getInsee());
            }

            if (tiamatParking.getPostalAddress() != null){
                org.rutebanken.tiamat.model.PostalAddress tiamatPostalAddress = tiamatParking.getPostalAddress();
                if (StringUtils.isNotBlank(tiamatPostalAddress.getTown())){
                    MultilingualString townMultiling = new MultilingualString();
                    townMultiling.setValue(tiamatPostalAddress.getTown());
                    netexPostalAddress.setTown(townMultiling);
                }

                if (StringUtils.isNotEmpty(tiamatPostalAddress.getStreet())){
                    MultilingualString streetMultiLing = new MultilingualString();
                    streetMultiLing.setValue(tiamatPostalAddress.getStreet());
                    netexPostalAddress.setStreet(streetMultiLing);
                }

                if (StringUtils.isNotEmpty(tiamatPostalAddress.getPostCode())) {
                    netexPostalAddress.setPostCode(tiamatPostalAddress.getPostCode());
                }
            }
            netexParking.setPostalAddress(netexPostalAddress);
        }
    }

    private static String getParkingSuperId(org.rutebanken.tiamat.model.Parking parking) {
        if (StringUtils.isNotBlank(parking.getNetexId()) && parking.getNetexId().split(":").length == 3){
            return parking.getNetexId().split(":")[0];
        }
        return "MOBIITI";
    }

    private static void mapPaymentMethodBtoA(org.rutebanken.tiamat.model.Parking tiamatParking, Parking netexParking) {
        netexParking.getPaymentMethods().clear();
        if (CollectionUtils.isNotEmpty(tiamatParking.getParkingPaymentMethods())) {
            for (org.rutebanken.tiamat.model.PaymentMethodEnumeration enumeration : tiamatParking.getParkingPaymentMethods()) {
                netexParking.getPaymentMethods().add(PaymentMethodEnumeration.fromValue(enumeration.value()));
            }
        }
    }

    private static void mapPaymentMethodAtoB(Parking netexParking, org.rutebanken.tiamat.model.Parking tiamatParking) {
        tiamatParking.getParkingPaymentMethods().clear();
        if (CollectionUtils.isNotEmpty(netexParking.getPaymentMethods())) {
            for (var enumeration : netexParking.getPaymentMethods()) {
                tiamatParking.getParkingPaymentMethods().add(org.rutebanken.tiamat.model.PaymentMethodEnumeration.fromValue(enumeration.value()));
            }
        }
    }

    @Override
    public void mapAtoB(Parking netexParking, org.rutebanken.tiamat.model.Parking tiamatParking, MappingContext context) {
        super.mapAtoB(netexParking, tiamatParking, context);

        mapPaymentMethodAtoB(netexParking, tiamatParking);
        mapVehicleEntrancesMethodAtoB(netexParking, tiamatParking);

        if (netexParking.getTypeOfParkingRef() != null) {
            tiamatParking.setTypeOfParkingRef(netexParking.getTypeOfParkingRef().getRef());
        }

        if (netexParking.getParkingAreas() != null &&
                netexParking.getParkingAreas().getParkingAreaRefOrParkingArea_() != null &&
                !netexParking.getParkingAreas().getParkingAreaRefOrParkingArea_().isEmpty()) {
            List<org.rutebanken.tiamat.model.ParkingArea> parkingAreas = mapperFacade.mapAsList(netexParking.getParkingAreas().getParkingAreaRefOrParkingArea_(), org.rutebanken.tiamat.model.ParkingArea.class, context);
            if (!parkingAreas.isEmpty()) {
                tiamatParking.setParkingAreas(parkingAreas);
            }
        }

        if (netexParking.getPostalAddress() != null) {
            tiamatParking.setInsee(netexParking.getPostalAddress().getPostalRegion());
            if (StringUtils.isNotEmpty(netexParking.getPostalAddress().getPostCode())) {
                tiamatParking.getPostalAddress().setPostCode(netexParking.getPostalAddress().getPostCode());
            }
        }

        Set<org.rutebanken.tiamat.model.AvailabilityCondition> tiamatAvailabilityConditions = new HashSet<>();
        if (netexParking.getValidityConditions() != null && CollectionUtils.isNotEmpty(netexParking.getValidityConditions().getValidityConditionRefOrValidBetweenOrValidityCondition_())) {

            for (Object validityCondEltObj : netexParking.getValidityConditions().getValidityConditionRefOrValidBetweenOrValidityCondition_()) {

                if (validityCondEltObj instanceof JAXBElement<?> jaxb && jaxb.getValue() instanceof AvailabilityCondition netexAvailCond) {
                        org.rutebanken.tiamat.model.AvailabilityCondition tiamatAvailCondition = new org.rutebanken.tiamat.model.AvailabilityCondition();
                        tiamatAvailCondition.setAvailable(netexAvailCond.isIsAvailable());
                        mapDayTypes(tiamatAvailCondition, netexAvailCond);
                        tiamatAvailabilityConditions.add(tiamatAvailCondition);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(tiamatAvailabilityConditions)) {
            tiamatParking.setAvailabilityConditions(tiamatAvailabilityConditions);
        }

        if (tiamatParking.getCentroid() == null) {
            mapCentroidFromFirstVehicleEntranceMethodAtoB(netexParking, tiamatParking);
        }
    }

    private void mapDayTypes(org.rutebanken.tiamat.model.AvailabilityCondition tiamatAvailCondition, AvailabilityCondition netexAvailCond) {

        if (netexAvailCond.getDayTypes() == null || CollectionUtils.isEmpty(netexAvailCond.getDayTypes().getDayTypeRefOrDayType_())) {
            return;
        }
        Set<org.rutebanken.tiamat.model.DayType> tiamatDayTypes = new HashSet<>();

        for (JAXBElement<?> jaxbElement : netexAvailCond.getDayTypes().getDayTypeRefOrDayType_()) {
            if (jaxbElement.getValue() instanceof DayType netexDayType) {
                org.rutebanken.tiamat.model.DayType tiamatDayType = new org.rutebanken.tiamat.model.DayType();
                //tiamatDayType.setNetexId(netexDayType.getId());

                if (netexDayType.getProperties() != null && CollectionUtils.isNotEmpty(netexDayType.getProperties().getPropertyOfDay())) {
                    PropertyOfDay firstDay = netexDayType.getProperties().getPropertyOfDay().getFirst();
                    if (firstDay.getDaysOfWeek() != null && CollectionUtils.isNotEmpty(firstDay.getDaysOfWeek())) {
                        String dayOfWeekValue = firstDay.getDaysOfWeek().getFirst().value();
                        org.rutebanken.tiamat.model.DayOfWeekEnumeration tiamatDayOfWeek = org.rutebanken.tiamat.model.DayOfWeekEnumeration.fromValue(dayOfWeekValue);
                        tiamatDayType.setDays(tiamatDayOfWeek);
                    }
                }

                if (netexDayType.getTimebands() != null && CollectionUtils.isNotEmpty(netexDayType.getTimebands().getTimebandRefOrTimeband())) {
                    Set<TimeBand> tiamatTimeBands = new HashSet<>();

                    for (Object timeBandJaxb : netexDayType.getTimebands().getTimebandRefOrTimeband()) {
                        if (timeBandJaxb instanceof Timeband_VersionedChildStructure netexTimeband) {
                            TimeBand tiamatTimeBand = new TimeBand();

                            if (netexTimeband.getStartTime() != null) {
                                tiamatTimeBand.setStartTime(netexTimeband.getStartTime());
                            }

                            if (netexTimeband.getEndTime() != null) {
                                tiamatTimeBand.setEndTime(netexTimeband.getEndTime());
                            }

                            if (netexTimeband.getDayOffset() != null) {
                                tiamatTimeBand.setDayOffset(netexTimeband.getDayOffset().intValue());
                            } else if (netexTimeband.getEndTime().equals(LocalTime.of(0, 0, 0)) && netexTimeband.getDayOffset() == null) {
                                tiamatTimeBand.setDayOffset(1);
                            }


                            tiamatTimeBands.add(tiamatTimeBand);
                        }
                    }

                    if (CollectionUtils.isNotEmpty(tiamatTimeBands)) {
                        tiamatDayType.setTimeBand(tiamatTimeBands);
                    }
                }

                tiamatDayTypes.add(tiamatDayType);
            }
        }

        if (CollectionUtils.isNotEmpty(tiamatDayTypes)) {
            tiamatAvailCondition.setDayTypes(tiamatDayTypes);
        }
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.Parking tiamatParking, Parking netexParking, MappingContext context) {
        super.mapBtoA(tiamatParking, netexParking, context);
        NetexUtils.fillTypeOfKey(netexParking);

        String originalId =
                tiamatParking.getKeyValues().get(ORIGINAL_ID_KEY).getItems().stream().findFirst().orElse(null);

        if (PARKING_ID_PATTERN.matcher(originalId).matches()) {
            netexParking.setId(originalId);
        } else if (PARKING_PAN_ID_PATTERN.matcher(originalId).matches()) {
            netexParking.setId(panParkingIdToNetexParkingId(originalId));
        } else {
            netexParking.setId(otherParkingIdToNetexParkingId(originalId, tiamatParking.getInsee()));
        }

        mapPaymentMethodBtoA(tiamatParking, netexParking);
        mapVehicleEntrancesMethodBtoA(tiamatParking, netexParking);

        if (StringUtils.isNotEmpty(tiamatParking.getParkingTypeRef())) {
            TypeOfParkingRefStructure typeOfParkingRefStructure = new TypeOfParkingRefStructure();
            typeOfParkingRefStructure.withRef(tiamatParking.getParkingTypeRef());
            typeOfParkingRefStructure.withVersion("any");
            netexParking.setTypeOfParkingRef(typeOfParkingRefStructure);
        }

        mapAddress(tiamatParking, netexParking);

        mapAppDownloadUrl(tiamatParking, netexParking);
        mapValidityConditions(tiamatParking, netexParking);

        if (tiamatParking.getUrl() != null) {
            InfoLinkStructure infoLink = new InfoLinkStructure();
            GroupOfEntities_VersionStructure.InfoLinks infoLinks = new GroupOfEntities_VersionStructure.InfoLinks();
            infoLink.setValue(tiamatParking.getUrl());
            infoLinks.getInfoLink().add(infoLink);

            netexParking.setInfoLinks(infoLinks);
        }

        if (tiamatParking.getParkingAreas() != null &&
                !tiamatParking.getParkingAreas().isEmpty()) {

            List<ParkingArea_VersionStructure> parkingAreas = new ArrayList<>();
            for (org.rutebanken.tiamat.model.ParkingArea pa : tiamatParking.getParkingAreas()) {
                if (pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.CARPOOL)) {
                    parkingAreas.add(mapperFacade.map(pa, VehiclePoolingParkingArea.class));
                } else if (pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.CARSHARE)) {
                    parkingAreas.add(mapperFacade.map(pa, VehicleSharingParkingArea.class));
                } else {
                    parkingAreas.add(mapperFacade.map(pa, ParkingArea.class));
                }
            }

            if (!parkingAreas.isEmpty()) {
                parkingAreas.forEach(pa -> {
                    if (pa.getSiteRef() == null) {
                        SiteRefStructure siteRefStructure = new SiteRefStructure();
                        siteRefStructure.setRef(netexParking.getId());
                        pa.setSiteRef(siteRefStructure);
                    }
                });
                ParkingAreas_RelStructure parkingAreasRelStructure = new ParkingAreas_RelStructure();
                parkingAreasRelStructure.getParkingAreaRefOrParkingArea_().addAll(parkingAreas.stream()
                        .map(pa -> {
                            if (pa instanceof VehiclePoolingParkingArea vppa) {
                                return netexObjectFactory.createVehiclePoolingParkingArea(vppa);
                            } else if (pa instanceof VehicleSharingParkingArea vspa) {
                                return netexObjectFactory.createVehicleSharingParkingArea(vspa);
                            } else {
                                return netexObjectFactory.createParkingArea((ParkingArea) pa);
                            }
                        })
                        .toList());

                netexParking.setParkingAreas(parkingAreasRelStructure);
            }
        }
    }

    private void mapValidityConditions(org.rutebanken.tiamat.model.Parking tiamatParking, Parking netexParking) {

        if ( CollectionUtils.isEmpty(tiamatParking.getAvailabilityConditions())) {
            return;
        }


        List<AvailabilityCondition> netexAvailabilityConditions = new ArrayList<>();
        for (org.rutebanken.tiamat.model.AvailabilityCondition availabilityCondition : tiamatParking.getAvailabilityConditions()) {
            AvailabilityCondition netexAvailabilityCondition = new AvailabilityCondition();
            netexAvailabilityCondition.setId(availabilityCondition.getNetexId());
            netexAvailabilityCondition.setVersion("any");
            netexAvailabilityCondition.setIsAvailable(availabilityCondition.isAvailable());

            if (CollectionUtils.isNotEmpty(availabilityCondition.getDayTypes())) {
                List<DayType> netexDayTypes = new ArrayList<>();
                for (org.rutebanken.tiamat.model.DayType dayType : availabilityCondition.getDayTypes()) {
                    DayType netexDayType = new DayType();
                    netexDayType.setId(dayType.getNetexId());
                    netexDayType.setVersion("any");

                    if (dayType.getDays() != null) {
                        PropertiesOfDay_RelStructure propOfDayStruct = new PropertiesOfDay_RelStructure();
                        PropertyOfDay propOfDay = new PropertyOfDay();
                        String dayOfWeekStr = dayType.getDays().value();
                        propOfDay.getDaysOfWeek().add(DayOfWeekEnumeration.fromValue(dayOfWeekStr));
                        propOfDayStruct.getPropertyOfDay().add(propOfDay);
                        netexDayType.setProperties(propOfDayStruct);
                    }

                    if (CollectionUtils.isNotEmpty(dayType.getTimeBand())) {
                        List<Timeband_VersionedChildStructure> netexTimeBands = new ArrayList<>();
                        for (TimeBand timeBand : dayType.getTimeBand()) {
                            Timeband_VersionedChildStructure netexTimeBand = new Timeband_VersionedChildStructure();
                            netexTimeBand.setId(timeBand.getNetexId());
                            netexTimeBand.setVersion("any");
                            netexTimeBand.setStartTime(timeBand.getStartTime());
                            netexTimeBand.setEndTime(timeBand.getEndTime());
                            netexTimeBand.setDayOffset(BigInteger.valueOf(timeBand.getDayOffset()));
                            netexTimeBands.add(netexTimeBand);
                        }

                        if (CollectionUtils.isNotEmpty(netexTimeBands)) {
                            Timebands_RelStructure timebandStruct = new Timebands_RelStructure();
                            for (Timeband_VersionedChildStructure netexTimeBand : netexTimeBands) {
                                timebandStruct.getTimebandRefOrTimeband().add(netexTimeBand);
                                netexDayType.setTimebands(timebandStruct);
                            }
                        }
                    }
                    netexDayTypes.add(netexDayType);
                }

                if (CollectionUtils.isNotEmpty(netexDayTypes)) {
                    DayTypes_RelStructure dayTypeStruct = new DayTypes_RelStructure();
                    for (DayType netexDayType : netexDayTypes) {
                        dayTypeStruct.getDayTypeRefOrDayType_().add(netexObjectFactory.createDayType(netexDayType));
                    }
                    netexAvailabilityCondition.setDayTypes(dayTypeStruct);
                }


            }

            netexAvailabilityConditions.add(netexAvailabilityCondition);
        }


        if (CollectionUtils.isNotEmpty(netexAvailabilityConditions)) {
            ValidityConditions_RelStructure validityConditionStruct = new ValidityConditions_RelStructure();
            for (AvailabilityCondition netexAvailabilityCondition : netexAvailabilityConditions) {
                JAXBElement<AvailabilityCondition> jaxbAvailabilityCond = netexObjectFactory.createAvailabilityCondition(netexAvailabilityCondition);
                validityConditionStruct.getValidityConditionRefOrValidBetweenOrValidityCondition_().add(jaxbAvailabilityCond);
            }
            netexParking.setValidityConditions(validityConditionStruct);
        }


    }

    private static void mapVehicleEntrancesMethodAtoB(Parking netexParking, org.rutebanken.tiamat.model.Parking tiamatParking) {
        if (netexParking.getVehicleEntrances() != null &&
                CollectionUtils.isNotEmpty(netexParking.getVehicleEntrances().getParkingEntranceForVehiclesRefOrParkingEntranceForVehicles())) {

            List<ParkingEntrance> tiamatEntrances = new ArrayList<>();

            for (Object entranceObj : netexParking.getVehicleEntrances().getParkingEntranceForVehiclesRefOrParkingEntranceForVehicles()) {
                // Le contenu peut être un JAXBElement ou l'objet directement selon le unmarshaller
                ParkingEntranceForVehicles netexEntrance = null;
                if (entranceObj instanceof JAXBElement<?> jaxb) {
                    netexEntrance = (ParkingEntranceForVehicles) jaxb.getValue();
                } else if (entranceObj instanceof ParkingEntranceForVehicles p) {
                    netexEntrance = p;
                }

                if (netexEntrance != null) {
                    ParkingEntrance tiamatEnt = new ParkingEntrance();
                    tiamatEnt.setNetexId(netexEntrance.getId());

                    if (netexEntrance.getName() != null) {
                        tiamatEnt.setName(new EmbeddableMultilingualString(netexEntrance.getName().getValue()));
                    }

                    if (netexEntrance.getCentroid() != null && netexEntrance.getCentroid().getLocation() != null) {
                        LocationStructure structure = netexEntrance.getCentroid().getLocation();
                        tiamatEnt.setLongitude(structure.getLongitude());
                        tiamatEnt.setLatitude(structure.getLatitude());
                    }
                    tiamatEntrances.add(tiamatEnt);
                }
            }
            tiamatParking.setVehicleEntrances(tiamatEntrances);
        }
    }

    private void mapVehicleEntrancesMethodBtoA(org.rutebanken.tiamat.model.Parking tiamatParking, Parking netexParking) {
        if (CollectionUtils.isEmpty(tiamatParking.getVehicleEntrances())) {
            return;
        }

        ParkingEntrancesForVehicles_RelStructure entrancesRel = new ParkingEntrancesForVehicles_RelStructure();

        for (ParkingEntrance tiamatEnt : tiamatParking.getVehicleEntrances()) {
            ParkingEntranceForVehicles netexEnt = new ParkingEntranceForVehicles();
            netexEnt.setId(tiamatEnt.getNetexId());
            netexEnt.setVersion(String.valueOf(tiamatEnt.getVersion()));

            SiteRefStructure siteRef = new SiteRefStructure();
            siteRef.setRef(netexParking.getId());
            netexEnt.setSiteRef(siteRef);

            if (tiamatEnt.getName() != null && StringUtils.isNotBlank(tiamatEnt.getName().getValue())) {
                netexEnt.setName(new MultilingualString().withValue(tiamatEnt.getName().getValue()));
            }

            if (tiamatEnt.getLongitude() != null && tiamatEnt.getLatitude() != null) {
                LocationStructure location = new LocationStructure();
                location.setLongitude(tiamatEnt.getLongitude());
                location.setLatitude(tiamatEnt.getLatitude());

                SimplePoint_VersionStructure centroid = new SimplePoint_VersionStructure();
                centroid.setLocation(location);
                netexEnt.setCentroid(centroid);
            }

            entrancesRel.getParkingEntranceForVehiclesRefOrParkingEntranceForVehicles().add(netexEnt);
        }

        netexParking.setVehicleEntrances(entrancesRel);
    }

    private static void mapCentroidFromFirstVehicleEntranceMethodAtoB(Parking netexParking,
                                                            org.rutebanken.tiamat.model.Parking tiamatParking) {
        if (netexParking == null || netexParking.getVehicleEntrances() == null) {
            return;
        }

        List<Object> entrances = netexParking.getVehicleEntrances()
                .getParkingEntranceForVehiclesRefOrParkingEntranceForVehicles();
        if (entrances == null || entrances.isEmpty()) {
            return;
        }

        Object firstObj = entrances.getFirst();
        ParkingEntranceForVehicles firstEntrance = null;

        if (firstObj instanceof JAXBElement<?>) {
            firstEntrance = (ParkingEntranceForVehicles) ((JAXBElement<?>) firstObj).getValue();
        } else if (firstObj instanceof ParkingEntranceForVehicles) {
            firstEntrance = (ParkingEntranceForVehicles) firstObj;
        }

        if (firstEntrance != null
                && firstEntrance.getCentroid() != null
                && firstEntrance.getCentroid().getLocation() != null) {

            LocationStructure loc = firstEntrance.getCentroid().getLocation();
            double lon = 0.0;
            double lat = 0.0;

            if (loc.getLongitude() != null) {
                lon = Double.parseDouble(String.valueOf(loc.getLongitude()));
            }
            if (loc.getLatitude() != null) {
                lat = Double.parseDouble(String.valueOf(loc.getLatitude()));
            }


            tiamatParking.setCentroid(ImporterUtils.createPoint(lon, lat));
        }
    }
}
