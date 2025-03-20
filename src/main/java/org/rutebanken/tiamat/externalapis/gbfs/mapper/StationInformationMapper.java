package org.rutebanken.tiamat.externalapis.gbfs.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportData;
import org.rutebanken.tiamat.model.gbfs.StationInformation;
import org.rutebanken.tiamat.model.gbfs.VehicleType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class StationInformationMapper {

    private static final BigDecimal DEFAULT_PARKING_AREA_MAXIMUM_HEIGHT = new BigDecimal(300); // 3 meters

    private final GeometryFactory geometryFactory = new GeometryFactory();

    protected static void mapVehicleCapacity(StationInformation stationInformation, List<VehicleType> vehicleTypes, Parking parking) {
        if (stationInformation.getVehicleCapacity() != null && !stationInformation.getVehicleCapacity().isEmpty()) {
            ParkingProperties parkingProperties = new ParkingProperties();
            List<ParkingCapacity> parkingCapacityList = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : stationInformation.getVehicleCapacity().entrySet()) {
                convertVehicleType(vehicleTypes, entry, parkingCapacityList);
            }
            if (CollectionUtils.isNotEmpty(parkingCapacityList)) {
                parkingProperties.setSpaces(parkingCapacityList);
                parking.setParkingProperties(List.of(parkingProperties));
            }

        }
    }

    private static void convertVehicleType(List<VehicleType> vehicleTypes, Map.Entry<String, Integer> entry, List<ParkingCapacity> parkingCapacityList) {
        for (VehicleType vehicleType : vehicleTypes) {
            if (entry.getKey().equals(vehicleType.getVehicleTypeId())) {
                ParkingCapacity parkingCapacity = new ParkingCapacity();
                if (entry.getValue() != null) {
                    parkingCapacity.setNumberOfSpaces(BigInteger.valueOf(entry.getValue()));
                } else {
                    parkingCapacity.setNumberOfSpaces(BigInteger.ZERO);
                }
                parkingCapacity.setParkingVehicleType(toParkingVehicle(vehicleType));
                parkingCapacityList.add(parkingCapacity);
            }
        }
    }

    private static ParkingArea toParkingArea(StationInformation stationInformation, SpecificParkingAreaUsageEnumeration parkingAreaType, Parking parking) {
        ParkingArea parkingArea = new ParkingArea();
        parkingArea.setName(new EmbeddableMultilingualString(stationInformation.getName()));
        parkingArea.setTotalCapacity(BigInteger.valueOf(stationInformation.getCapacity()));
        parkingArea.setSpecificParkingAreaUsage(parkingAreaType);
        // maximumHeight is required by Netex Parking FRANCE profile v1.2
        // We put 3 meters as default value to be compliant
        parkingArea.setMaximumHeight(DEFAULT_PARKING_AREA_MAXIMUM_HEIGHT);
        // siteRef.ref is also required by Netex Parking FRANCE profile v1.2
        SiteRefStructure siteRefStructure = new SiteRefStructure();
        siteRefStructure.setRef(parking.getNetexId());
        parkingArea.setSiteRef(new SiteRefStructure());
        return parkingArea;
    }

    public static ParkingLayoutEnumeration toParkingLayout(StationInformation stationInformation) {
        if (stationInformation == null || StringUtils.isBlank(stationInformation.getParkingType())) {
            return ParkingLayoutEnumeration.UNDEFINED;
        }
        return switch (stationInformation.getParkingType()) {
            case "parking_lot" -> ParkingLayoutEnumeration.OPEN_SPACE;
            case "street_parking", "sidewalk_parking" -> ParkingLayoutEnumeration.ROADSIDE;
            case "underground_parking" -> ParkingLayoutEnumeration.UNDERGROUND;
            default -> ParkingLayoutEnumeration.UNDEFINED;
        };
    }

    private static ParkingVehicleEnumeration toParkingVehicle(VehicleType vehicleType) {
        return switch (vehicleType.getFormFactor()) {
            case "bicycle", "cargo_bicycle" -> ParkingVehicleEnumeration.PEDAL_CYCLE;
            case "car" -> ParkingVehicleEnumeration.CAR;
            case "moped" -> ParkingVehicleEnumeration.MOPED;
            case "scooter" -> ParkingVehicleEnumeration.MOTOR_SCOOTER;
            default -> ParkingVehicleEnumeration.UNDEFINED;
        };
    }

    public static Set<PaymentMethodEnumeration> toParkingPaymentMethods(StationInformation stationInformation) {
        if (stationInformation == null || CollectionUtils.isEmpty(stationInformation.getRentalMethods())) {
            return new HashSet<>();
        }
        return stationInformation.getRentalMethods()
                .stream()
                .map(StationInformationMapper::toParkingPaymentMethod)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static PaymentMethodEnumeration toParkingPaymentMethod(String rentalMethod) {
        return switch (rentalMethod) {
            case "creditcard" -> PaymentMethodEnumeration.CREDIT_CARD;
            case "paypass" -> PaymentMethodEnumeration.CONTACTLESS_PAYMENT_CARD;
            case "applepay", "androidpay", "phone" -> PaymentMethodEnumeration.MOBILE_PHONE;
            case "transitcard" -> PaymentMethodEnumeration.TRAVEL_CARD;
            default -> null;
        };
    }

    public List<Parking> toParkingList(Organisation organisation, GbfsParkingImportData gbfsParkingImportData) {
        List<Parking> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(gbfsParkingImportData.stations())) {
            result = gbfsParkingImportData.stations()
                    .stream()
                    .map(station -> toParking(organisation, station, gbfsParkingImportData.parkingType(), gbfsParkingImportData.vehicleTypes(), gbfsParkingImportData.parkingAreaType()))
                    .toList();
        }
        return result;
    }

    public Parking toParking(Organisation organisation, StationInformation stationInformation, ParkingTypeEnumeration parkingType, List<VehicleType> vehicleTypes, SpecificParkingAreaUsageEnumeration parkingAreaType) {
        Parking parking = new Parking();
        if (stationInformation != null) {
            parking.setNetexId("MOBIITI:PARKING:" + stationInformation.getStationId());
            parking.setOriginalId(stationInformation.getStationId());
            parking.setName(new EmbeddableMultilingualString(stationInformation.getName()));
            parking.setShortName(new EmbeddableMultilingualString(stationInformation.getShortName()));
            parking.setCentroid(geometryFactory.createPoint(new Coordinate(stationInformation.getLongitude(), stationInformation.getLatitude())));
            parking.setAddress(stationInformation.getAddress());
            parking.setCrossRoad(new MultilingualStringEntity(stationInformation.getCrossStreet()));
            parking.setTotalCapacity(new BigInteger(String.valueOf(stationInformation.getCapacity())));
            parking.setRechargingAvailable(stationInformation.getChargingStation());
            if (stationInformation.getRentalUri() != null) {
                parking.setBookingUrl(stationInformation.getRentalUri().getBookingUri());
                parking.setRentalUriIos(stationInformation.getRentalUri().getRentalUriIos());
                parking.setRentalUriAndroid(stationInformation.getRentalUri().getRentalUriAndroid());
            }
            Set<PaymentMethodEnumeration> parkingPaymentMethodEnumeration = toParkingPaymentMethods(stationInformation);
            if (CollectionUtils.isNotEmpty(parkingPaymentMethodEnumeration)) {
                parking.getParkingPaymentMethods().addAll(parkingPaymentMethodEnumeration);
                parking.setParkingPaymentProcess(List.of(ParkingPaymentProcessEnumeration.PAY_BY_MOBILE_DEVICE,
                        ParkingPaymentProcessEnumeration.PAY_AND_DISPLAY,
                        ParkingPaymentProcessEnumeration.PAY_BY_PREPAID_TOKEN));
            } else {
                // required by Netex Parking FRANCE profile v1.2
                parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.UNDEFINED);
            }
            // required by Netex Parking FRANCE profile v1.2
            parking.getParkingVehicleTypes().addAll(vehicleTypes.stream().map(StationInformationMapper::toParkingVehicle).collect(Collectors.toSet()));
            // required by Netex Parking FRANCE profile v1.2
            parking.setParkingLayout(toParkingLayout(stationInformation)); // required by Netex Parking FRANCE profile v1.2
            parking.setOrganisation(organisation);
            parking.setParkingType(parkingType);
            mapVehicleCapacity(stationInformation, vehicleTypes, parking);
            parking.setParkingAreas(List.of(toParkingArea(stationInformation, parkingAreaType, parking)));
        }
        return parking;
    }
}
