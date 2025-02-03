package org.rutebanken.tiamat.externalapis.gbfs.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportData;
import org.rutebanken.tiamat.model.gbfs.StationInformation;
import org.rutebanken.tiamat.model.gbfs.VehicleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

public class StationInformationMapper {

    private static final Logger logger = LoggerFactory.getLogger(StationInformationMapper.class);

    private final GeometryFactory geometryFactory = new GeometryFactory();

    public List<Parking> toParkingList(Organisation organisation, GbfsParkingImportData gbfsParkingImportData) {
        List<Parking> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(gbfsParkingImportData.stations())) {
            result = gbfsParkingImportData.stations()
                    .stream()
                    .map(station -> toParking(organisation, station, gbfsParkingImportData.parkingTypeEnumeration(), gbfsParkingImportData.vehicleTypes()))
                    .toList();
        }
        return result;
    }

    public Parking toParking(Organisation organisation, StationInformation stationInformation, ParkingTypeEnumeration parkingType, List<VehicleType> vehicleTypes) {
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
            Set<PaymentMethodEnumeration> parkingPaymentMethodEnumeration = toParkingPaymentMethodEnumeration(stationInformation);
            if (CollectionUtils.isNotEmpty(parkingPaymentMethodEnumeration)) {
                parking.getParkingPaymentMethods().addAll(parkingPaymentMethodEnumeration);
                parking.setParkingPaymentProcess(List.of(ParkingPaymentProcessEnumeration.PAY_BY_MOBILE_DEVICE,
                        ParkingPaymentProcessEnumeration.PAY_AND_DISPLAY,
                        ParkingPaymentProcessEnumeration.PAY_BY_PREPAID_TOKEN));
            }
            ParkingLayoutEnumeration parkingLayoutEnumeration = toParkingLayoutEnumeration(stationInformation);
            if (parkingLayoutEnumeration != null) {
                parking.setParkingLayout(parkingLayoutEnumeration);
            }
            parking.setOrganisation(organisation);
            parking.setParkingType(parkingType);
            mapVehicleCapacity(stationInformation, vehicleTypes, parking);

        }
        return parking;
    }

    public Set<PaymentMethodEnumeration> toParkingPaymentMethodEnumeration(StationInformation stationInformation) {
        Set<PaymentMethodEnumeration> paymentMethodEnumerations = new HashSet<>();
        if (stationInformation != null && CollectionUtils.isNotEmpty(stationInformation.getRentalMethods())) {
            for (String rentalMethod : stationInformation.getRentalMethods()) {
                switch (rentalMethod) {
                    case "creditcard":
                        paymentMethodEnumerations.add(PaymentMethodEnumeration.CREDIT_CARD);
                        break;
                    case "paypass":
                        paymentMethodEnumerations.add(PaymentMethodEnumeration.CONTACTLESS_PAYMENT_CARD);
                        break;
                    case "applepay", "androidpay", "phone":
                        paymentMethodEnumerations.add(PaymentMethodEnumeration.MOBILE_PHONE);
                        break;
                    case "transitcard":
                        paymentMethodEnumerations.add(PaymentMethodEnumeration.TRAVEL_CARD);
                        break;
                    default:
                        break;
                }
            }
        }
        return paymentMethodEnumerations;
    }

    public ParkingLayoutEnumeration toParkingLayoutEnumeration(StationInformation stationInformation) {
        ParkingLayoutEnumeration parkingLayoutEnumeration = null;
        if (stationInformation != null && StringUtils.isNotBlank(stationInformation.getParkingType())) {
            switch (stationInformation.getParkingType()) {
                case "parking_lot":
                    parkingLayoutEnumeration = ParkingLayoutEnumeration.OPEN_SPACE;
                    break;
                case "street_parking", "sidewalk_parking":
                    parkingLayoutEnumeration = ParkingLayoutEnumeration.ROADSIDE;
                    break;
                case "underground_parking":
                    parkingLayoutEnumeration = ParkingLayoutEnumeration.UNDERGROUND;
                    break;
                default:
                    break;
            }
        }
        return parkingLayoutEnumeration;
    }

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

                ParkingVehicleEnumeration parkingVehicleEnumeration = ParkingVehicleEnumeration.OTHER;
                try {
                    parkingVehicleEnumeration = ParkingVehicleEnumeration.fromValue(vehicleType.getFormFactor());
                } catch (IllegalArgumentException e) {
                    logger.error("Error converting vehicle type to parking vehicle enumeration - {}", vehicleType.getFormFactor());
                }
                parkingCapacity.setParkingVehicleType(parkingVehicleEnumeration);
                parkingCapacityList.add(parkingCapacity);
            }
        }
    }
}
