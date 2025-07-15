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

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.model.SpecificParkingAreaUsageEnumeration;
import org.rutebanken.tiamat.netex.NetexUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.rutebanken.tiamat.netex.id.NetexIdHelper.*;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

public class ParkingMapper extends CustomMapper<Parking, org.rutebanken.tiamat.model.Parking> {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    private static final Pattern patternStreetNumber = Pattern.compile("^(\\d+)\\s*(.*)$");

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

    private static void mapAddress(org.rutebanken.tiamat.model.Parking tiamatParking, Parking netexParking) {
        if (tiamatParking.getInsee() != null || tiamatParking.getAddress() != null) {
            PostalAddress postalAddress = new PostalAddress();
            postalAddress.setId("MOBIITI:PostalAddress:" + UUID.randomUUID());
            postalAddress.setVersion("any");
            if (tiamatParking.getAddress() != null) {
                // Expression régulière pour capturer les premiers chiffres au début de l'adresse
                Matcher matcher = patternStreetNumber.matcher(tiamatParking.getAddress());

                if (matcher.matches()) {
                    postalAddress.setHouseNumber(matcher.group(1));
                    postalAddress.setStreet(new MultilingualString().withValue(matcher.group(2)));
                } else {
                    postalAddress.setStreet(new MultilingualString().withValue(tiamatParking.getAddress()));
                }
            }
            if (tiamatParking.getInsee() != null) {
                postalAddress.setPostalRegion(tiamatParking.getInsee());
            }
            netexParking.setPostalAddress(postalAddress);
        }
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
        }

    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.Parking tiamatParking, Parking netexParking, MappingContext context) {
        super.mapBtoA(tiamatParking, netexParking, context);
        NetexUtils.fillTypeOfKey(netexParking);

        String originalId =
                tiamatParking.getKeyValues().get(ORIGINAL_ID_KEY).getItems().stream().findFirst().orElseThrow();

        if (PARKING_ID_PATTERN.matcher(originalId).matches()) {
            netexParking.setId(originalId);
        } else if (PARKING_PAN_ID_PATTERN.matcher(originalId).matches()) {
            netexParking.setId(panParkingIdToNetexParkingId(originalId));
        } else {
            netexParking.setId(otherParkingIdToNetexParkingId(originalId, tiamatParking.getInsee()));
        }

        mapPaymentMethodBtoA(tiamatParking, netexParking);

        if (StringUtils.isNotEmpty(tiamatParking.getParkingTypeRef())) {
            TypeOfParkingRefStructure typeOfParkingRefStructure = new TypeOfParkingRefStructure();
            typeOfParkingRefStructure.withRef(tiamatParking.getParkingTypeRef());
            typeOfParkingRefStructure.withVersion("any");
            netexParking.setTypeOfParkingRef(typeOfParkingRefStructure);
        }

        mapAddress(tiamatParking, netexParking);

        mapAppDownloadUrl(tiamatParking, netexParking);

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
}
