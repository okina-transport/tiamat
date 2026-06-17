package org.rutebanken.tiamat.externalapis.gbfs.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mobilitydata.gbfs.v3_0.station_information.*;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSVehicleType;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSVehicleTypes;
import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StationInformationMapper {

    private static final Logger logger = LoggerFactory.getLogger(StationInformationMapper.class);
    private static final BigDecimal DEFAULT_PARKING_AREA_MAXIMUM_HEIGHT = new BigDecimal(300); // 3 meters
    private final String superIdPrefix;

    public StationInformationMapper(String superIdPrefix) {
        this.superIdPrefix = superIdPrefix;
    }

    private static List<ParkingProperties> toParkingProperties(GBFSStation gbfsStation, GBFSVehicleTypes gbfsVehicleTypes) {
        if (CollectionUtils.isEmpty(gbfsStation.getVehicleTypesCapacity())) {
            return Collections.emptyList();
        }
        Map<String, GBFSVehicleType> gbfsVTIdToVT = gbfsVehicleTypes.getData().getVehicleTypes().stream()
                .collect(Collectors.toMap(GBFSVehicleType::getVehicleTypeId, gbfsVehicleType -> gbfsVehicleType));
        ParkingProperties parkingProperties = new ParkingProperties();
        parkingProperties.setSpaces(toSpaces(gbfsStation.getVehicleTypesCapacity(), gbfsVTIdToVT));
        return List.of(parkingProperties);
    }

    private static List<ParkingCapacity> toSpaces(List<GBFSVehicleTypesCapacity> gbfsVTCs, Map<String, GBFSVehicleType> gbfsVTIdToVT) {
        List<ParkingCapacity> spaces = new ArrayList<>();
        for (GBFSVehicleTypesCapacity gbfsVTC : gbfsVTCs) {
            BigInteger numberOfSpaces = BigInteger.valueOf(gbfsVTC.getCount());
            for (String vtid : gbfsVTC.getVehicleTypeIds()) {
                spaces.add(toParkingCapacity(gbfsVTIdToVT.get(vtid), numberOfSpaces));
            }
        }
        return spaces;
    }

    private static ParkingCapacity toParkingCapacity(GBFSVehicleType gbfsVehicleType, BigInteger numberOfSpaces) {
        ParkingCapacity parkingCapacity = new ParkingCapacity();
        parkingCapacity.setNumberOfSpaces(numberOfSpaces);
        parkingCapacity.setParkingVehicleType(toParkingVehicle(gbfsVehicleType));
        return parkingCapacity;
    }

    private static List<ParkingArea> toParkingAreas(GBFSStation gbfsStation,
                                                    SpecificParkingAreaUsageEnumeration parkingAreaType) {
        ParkingArea parkingArea = new ParkingArea();
        parkingArea.setName(toParkingName(gbfsStation));
        parkingArea.setTotalCapacity(toTotalCapacity(gbfsStation));
        parkingArea.setSpecificParkingAreaUsage(parkingAreaType);
        // maximumHeight is required by Netex Parking FRANCE profile v1.2
        // We put 3 meters as default value to be compliant
        parkingArea.setMaximumHeight(DEFAULT_PARKING_AREA_MAXIMUM_HEIGHT);
        return List.of(parkingArea);
    }

    private static ParkingLayoutEnumeration toParkingLayout(GBFSStation gbfsStation) {
        if (gbfsStation.getParkingType() == null) {
            return ParkingLayoutEnumeration.UNDEFINED;
        }
        return switch (gbfsStation.getParkingType()) {
            case PARKING_LOT -> ParkingLayoutEnumeration.OPEN_SPACE;
            case STREET_PARKING, SIDEWALK_PARKING -> ParkingLayoutEnumeration.ROADSIDE;
            case UNDERGROUND_PARKING -> ParkingLayoutEnumeration.UNDERGROUND;
            default -> ParkingLayoutEnumeration.UNDEFINED;
        };
    }

    private static ParkingVehicleEnumeration toParkingVehicle(GBFSVehicleType gbfsVehicleType) {
        return switch (gbfsVehicleType.getFormFactor()) {
            case BICYCLE, CARGO_BICYCLE -> ParkingVehicleEnumeration.PEDAL_CYCLE;
            case CAR -> ParkingVehicleEnumeration.CAR;
            case MOPED -> ParkingVehicleEnumeration.MOPED;
            case SCOOTER -> ParkingVehicleEnumeration.MOTOR_SCOOTER;
            default -> ParkingVehicleEnumeration.UNDEFINED;
        };
    }

    private @NotNull String toNetexId(GBFSStation gbfsStation) {
        return superIdPrefix + ":PARKING:" + gbfsStation.getStationId().replace(":", "##3A##");
    }

    private static @NotNull EmbeddableMultilingualString toParkingName(GBFSStation gbfsStation) {
        GBFSName gbfsName = gbfsStation.getName().stream()
                .filter(s -> "fr".equals(s.getLanguage()))
                .findFirst()
                .orElse(gbfsStation.getName().getFirst());
        return new EmbeddableMultilingualString(gbfsName.getText(),
                gbfsName.getLanguage());
    }

    private static PaymentMethodEnumeration toParkingPaymentMethod(RentalMethod rentalMethod) {
        return switch (rentalMethod) {
            case CREDITCARD -> PaymentMethodEnumeration.CREDIT_CARD;
            case PAYPASS -> PaymentMethodEnumeration.CONTACTLESS_PAYMENT_CARD;
            case APPLEPAY, ANDROIDPAY, PHONE -> PaymentMethodEnumeration.MOBILE_PHONE;
            case TRANSITCARD -> PaymentMethodEnumeration.TRAVEL_CARD;
            default -> null;
        };
    }

    private static Set<PaymentMethodEnumeration> toParkingPaymentMethods(GBFSStation gbfsStation) {
        if (CollectionUtils.isEmpty(gbfsStation.getRentalMethods())) {
            return new HashSet<>();
        }
        return gbfsStation.getRentalMethods()
                .stream()
                .map(StationInformationMapper::toParkingPaymentMethod)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static BigInteger toTotalCapacity(GBFSStation gbfsStation) {
        if (gbfsStation.getCapacity() != null) {
            return BigInteger.valueOf(gbfsStation.getCapacity());
        } else if (CollectionUtils.isNotEmpty(gbfsStation.getVehicleTypesCapacity())) {
            return BigInteger.valueOf(gbfsStation.getVehicleTypesCapacity().stream().mapToInt(GBFSVehicleTypesCapacity::getCount).sum());
        } else if (CollectionUtils.isNotEmpty(gbfsStation.getVehicleDocksCapacity())) {
            return BigInteger.valueOf(gbfsStation.getVehicleDocksCapacity().stream().mapToInt(GBFSVehicleDocksCapacity::getCount).sum());
        }
        return null;
    }

    private static @Nullable EmbeddableMultilingualString toShortName(GBFSStation gbfsStation) {
        EmbeddableMultilingualString shortName = null;
        if (CollectionUtils.isNotEmpty(gbfsStation.getShortName())) {
            GBFSShortName gbfsShortName = gbfsStation.getShortName().stream()
                    .filter(sn -> "fr".equals(sn.getLanguage()))
                    .findFirst()
                    .orElse(gbfsStation.getShortName().getFirst());
            shortName = new EmbeddableMultilingualString(gbfsShortName.getText(), gbfsShortName.getLanguage());
        }
        return shortName;
    }

    private static @NotNull Set<ParkingVehicleEnumeration> toParkingVehicleTypes(GBFSStation gbfsStation, GBFSVehicleTypes gbfsVehicleTypes) {
        Set<ParkingVehicleEnumeration> parkingVehicleTypes = new HashSet<>();
        Map<String, GBFSVehicleType> gbfsVTIdToVT =
                gbfsVehicleTypes.getData().getVehicleTypes()
                        .stream()
                        .collect(Collectors.toMap(GBFSVehicleType::getVehicleTypeId, Function.identity()));
        for (GBFSVehicleTypesCapacity vtc : gbfsStation.getVehicleTypesCapacity()) {
            for (String vtid : vtc.getVehicleTypeIds()) {
                parkingVehicleTypes.add(toParkingVehicle(gbfsVTIdToVT.get(vtid)));
            }
        }
        return parkingVehicleTypes;
    }

    private String toInsee(GBFSStation station) {
        var centroid = toCentroid(station);
        try {
            DtoGeocode dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(centroid.getCoordinate().x,
                    centroid.getCoordinate().y);
            return dtoGeocode.getCityCode();
        } catch (Exception e) {
            logger.error("Error retrieving INSEE code for parking: {}", toNetexId(station), e);
            return StringUtils.EMPTY;
        }
    }

    public Parking toParking(Organisation organisation, GBFSStation gbfsStation,
                             GBFSVehicleTypes gbfsVehicleTypes, ParkingTypeEnumeration parkingType,
                             SpecificParkingAreaUsageEnumeration parkingAreaType) {
        Parking parking = new Parking();
        parking.setNetexId(toNetexId(gbfsStation));
        parking.setOriginalId(gbfsStation.getStationId());
        parking.setName(toParkingName(gbfsStation));
        parking.setShortName(toShortName(gbfsStation));
        parking.setCentroid(toCentroid(gbfsStation));
        parking.setAddress(gbfsStation.getAddress());
        parking.setInsee(toInsee(gbfsStation));
        parking.setCrossRoad(new MultilingualStringEntity(gbfsStation.getCrossStreet()));
        parking.setTotalCapacity(toTotalCapacity(gbfsStation));
        parking.setRechargingAvailable(gbfsStation.getIsChargingStation());
        if (gbfsStation.getRentalUris() != null) {
            parking.setBookingUrl(gbfsStation.getRentalUris().getWeb());
            parking.setRentalUriIos(gbfsStation.getRentalUris().getIos());
            parking.setRentalUriAndroid(gbfsStation.getRentalUris().getAndroid());
        }
        Set<PaymentMethodEnumeration> parkingPaymentMethodEnumeration = toParkingPaymentMethods(gbfsStation);
        if (CollectionUtils.isNotEmpty(parkingPaymentMethodEnumeration)) {
            parking.getParkingPaymentMethods().addAll(parkingPaymentMethodEnumeration);
            parking.setParkingPaymentProcess(
                    List.of(ParkingPaymentProcessEnumeration.PAY_BY_MOBILE_DEVICE,
                            ParkingPaymentProcessEnumeration.PAY_AND_DISPLAY,
                            ParkingPaymentProcessEnumeration.PAY_BY_PREPAID_TOKEN));
        } else {
            // required by Netex Parking FRANCE profile v1.2
            parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.UNDEFINED);
        }
        if (CollectionUtils.isEmpty(gbfsStation.getVehicleTypesCapacity())) {
            // required by Netex Parking FRANCE profile v1.2
            // cf. https://gbfs.org/fr/documentation/reference/#vehicle_typesjson:
            // REQUIRED of systems that include information about vehicle types in the vehicle_status.json file. If
            // this file is not included, then all vehicles in the feed are assumed to be non-motorized bicycles.
            parking.getParkingVehicleTypes().add(ParkingVehicleEnumeration.PEDAL_CYCLE);
        } else {
            // required by Netex Parking FRANCE profile v1.2
            parking.getParkingVehicleTypes().addAll(toParkingVehicleTypes(gbfsStation, gbfsVehicleTypes));
        }
        // required by Netex Parking FRANCE profile v1.2
        parking.setParkingLayout(toParkingLayout(gbfsStation));
        parking.setOrganisation(organisation);
        if (StringUtils.isNotBlank(organisation.getOperator())) {
            parking.setOperator(organisation.getOperator());
        } else {
            parking.setOperator(StringUtils.defaultIfBlank(organisation.getOriginalId(), "technique"));
        }

        parking.setParkingType(parkingType);
        parking.setParkingProperties(toParkingProperties(gbfsStation, gbfsVehicleTypes));
        parking.setParkingAreas(toParkingAreas(gbfsStation, parkingAreaType));
        return parking;
    }

    private org.locationtech.jts.geom.Point toCentroid(GBFSStation gbfsStation) {
        return ImporterUtils.createPoint(gbfsStation.getLon(), gbfsStation.getLat());
    }
}
