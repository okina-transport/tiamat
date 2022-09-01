package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.EnumUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.model.AccessibilityAssessment;
import org.rutebanken.tiamat.model.AccessibilityLimitation;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.LimitationStatusEnumeration;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingArea;
import org.rutebanken.tiamat.model.ParkingCapacity;
import org.rutebanken.tiamat.model.ParkingPaymentProcessEnumeration;
import org.rutebanken.tiamat.model.ParkingProperties;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.ParkingUserEnumeration;
import org.rutebanken.tiamat.model.PublicUseEnumeration;
import org.rutebanken.tiamat.model.SpecificParkingAreaUsageEnumeration;
import org.rutebanken.tiamat.rest.dto.DtoParking;
import org.rutebanken.tiamat.service.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParkingsCSVHelper {

    public final static String DELIMETER_PARKING_ID_NAME = " : ";

    private final static Pattern patternXlongYlat = Pattern.compile("^-?([0-9]*)\\.{1}\\d{1,20}");

    private static GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();


    public static List<DtoParking> parseDocument(InputStream csvFile) throws IllegalArgumentException, IOException {

        List<DtoParking> dtoParkingList = new ArrayList<>();

        Iterable<CSVRecord> records = CSVHelper.getRecords(csvFile);


        for (CSVRecord csvRecord : records) {
            DtoParking dtoParking = new DtoParking(
                    csvRecord.get(0),
                    csvRecord.get(1),
                    csvRecord.get(2),
                    csvRecord.get(3),
                    csvRecord.get(4),
                    csvRecord.get(5),
                    csvRecord.get(6),
                    csvRecord.get(7),
                    csvRecord.get(8),
                    csvRecord.get(9),
                    csvRecord.get(10),
                    csvRecord.get(11),
                    csvRecord.get(12),
                    csvRecord.get(13),
                    csvRecord.get(14),
                    csvRecord.get(15),
                    csvRecord.get(16),
                    csvRecord.get(17),
                    csvRecord.get(18),
                    csvRecord.get(19),
                    csvRecord.get(20),
                    csvRecord.get(21),
                    csvRecord.get(22),
                    csvRecord.get(23),
                    csvRecord.get(24),
                    csvRecord.get(25),
                    csvRecord.get(26),
                    csvRecord.get(27),
                    csvRecord.get(28),
                    csvRecord.get(29)
            );

            validateParking(dtoParking);

            dtoParkingList.add(dtoParking);
        }

        return dtoParkingList;
    }

    private static void validateParking(DtoParking parking) throws IllegalArgumentException {
        Preconditions.checkArgument(!parking.getId().isEmpty(), "ID is required in all your parkings");
        Preconditions.checkArgument(!parking.getName().isEmpty(), "NAME is required to parking with Id " + parking.getId());
        Preconditions.checkArgument(patternXlongYlat.matcher(parking.getXlong()).matches(), "X Longitud is not correct in the parking with " + parking.getId());
        Preconditions.checkArgument(patternXlongYlat.matcher(parking.getYlat()).matches(), "Y Latitud is not correct in the parking with " + parking.getId());
    }

    public static void checkDuplicatedParkings(List<DtoParking> parkings) throws IllegalArgumentException {
        List<String> compositeKey = parkings.stream().map(parking -> parking.getId() + parking.getName()).collect(Collectors.toList());
        List<String> duplicates = foundDuplicates(compositeKey);
        
        if (duplicates.size() > 0){
            String duplicatesMsg = duplicates.stream()
                    .collect(Collectors.joining(","));

            throw new IllegalArgumentException("There are duplicated parkings in your CSV File 'With the same ID & Name'. Duplicates:" + duplicatesMsg);
        }

    }

    private static List<String> foundDuplicates(List<String> fullList){
        List<String> alreadyReadList = new ArrayList<>();
        List<String> duplicateList = new ArrayList<>();

        fullList.stream()
                .forEach(id -> {
                    if (alreadyReadList.contains(id)){
                        duplicateList.add(id);
                    }else{
                        alreadyReadList.add(id);
                    }
                });

        return duplicateList;
    }


    public static List<Parking> mapFromDtoToEntity(List<DtoParking> dtoParkingsCSV) {
        return  dtoParkingsCSV.stream().map(parkingDto -> {

            Parking parking = new Parking();
            parking.setParkingAreas(new ArrayList<>());
            parking.setParkingProperties(new ArrayList<>());

            ParkingArea parkingArea = new ParkingArea();


            ParkingProperties parkingProperties = new ParkingProperties();
            parkingProperties.setSpaces(new ArrayList<>());

            parking.setOriginalId(parkingDto.getId());
            parking.setName(new EmbeddableMultilingualString(parkingDto.getName()));

            //Type d'usagers du parking
            if (EnumUtils.isValidEnum(ParkingUserEnumeration.class, parkingDto.getUserType())) {
                parkingProperties.getParkingUserTypes().add(ParkingUserEnumeration.fromValue(parkingDto.getUserType()));
            } else if (parkingDto.getUserType().equals("abonnés")) {
                parkingProperties.getParkingUserTypes().add(ParkingUserEnumeration.REGISTERED);
            } else if (parkingDto.getUserType().equals("tous")) {
                parkingProperties.getParkingUserTypes().add(ParkingUserEnumeration.ALL);
            }

            BigInteger parkAndRideCapacity = parkingDto.getNbOfPr().isEmpty() ? BigInteger.ZERO : new BigInteger(parkingDto.getNbOfPr());

            if (parkAndRideCapacity.equals(BigInteger.ZERO)){
                parking.setParkingType(ParkingTypeEnumeration.PARKING_ZONE);
            }else{
                parking.setParkingType(ParkingTypeEnumeration.PARK_AND_RIDE);

                ParkingArea parkAndRideArea = new ParkingArea();
                parkAndRideArea.setVersion(1L);
                parkAndRideArea.setName(new EmbeddableMultilingualString("Zone P+R", "FR"));
                parkAndRideArea.setTotalCapacity(parkAndRideCapacity);
                parkAndRideArea.setSpecificParkingAreaUsage(SpecificParkingAreaUsageEnumeration.PARD_AND_RIDE);
                parking.getParkingAreas().add(parkAndRideArea);
            }


            parking.setBookingUrl(parkingDto.getUrl());
            parking.setVersion(1L);
            parkingArea.setVersion(1L);
            parking.setInsee(parkingDto.getInsee());
            parking.setSiret(parkingDto.getSiretNumber());

            //Gratuité du parking ou non
            if (Boolean.parseBoolean(parkingDto.getFree())) {
                parking.setFreeParkingOutOfHours(true);
                parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.FREE);
            } else {
                parking.setFreeParkingOutOfHours(false);
            }

            //Hauteur maximum
            if (parkingDto.getMaxHeight() != null && !parkingDto.getMaxHeight().equalsIgnoreCase("N/A")){
                parkingArea.setMaximumHeight(new BigDecimal(parkingDto.getMaxHeight()));
            }


            //Gestion de la capacité max du parking
            BigInteger totalCapacityInt = parkingDto.getNbOfPlaces().isEmpty() ? BigInteger.ZERO : new BigInteger(parkingDto.getNbOfPlaces());
            ParkingCapacity pmrCapacity = new ParkingCapacity();
            pmrCapacity.setParkingUserType(ParkingUserEnumeration.REGISTERED_DISABLED);

            //Capacité totale du parking
            ParkingCapacity totalCapacity = new ParkingCapacity();
            totalCapacity.setParkingUserType(ParkingUserEnumeration.ALL_USERS);
            totalCapacity.setNumberOfSpaces(totalCapacityInt);
            parking.setTotalCapacity(totalCapacityInt);
            parkingArea.setTotalCapacity(totalCapacityInt);

            //Nombre de places destinées aux personnes handicapées (à soustraire du nombre total de places)
            if(!totalCapacityInt.equals(BigInteger.ZERO) && !parkingDto.getDisabledParkingNb().isEmpty()) {
                if (parkingDto.getDisabledParkingNb().equals(parkingDto.getNbOfPlaces())) {
                    parking.setAllAreasWheelchairAccessible(true);
                } else {
                    parking.setAllAreasWheelchairAccessible(false);
                }
                pmrCapacity.setNumberOfSpaces(new BigInteger(parkingDto.getDisabledParkingNb()));

                ParkingArea pmrParkingArea = new ParkingArea();
                pmrParkingArea.setSpecificParkingAreaUsage(SpecificParkingAreaUsageEnumeration.DISABLED);
                pmrParkingArea.setPublicUse(PublicUseEnumeration.DISABLED_PUBLIC_ONLY);
                pmrParkingArea.setVersion(1L);
                if (parkingDto.getMaxHeight() != null && !parkingDto.getMaxHeight().equalsIgnoreCase("N/A")){
                    pmrParkingArea.setMaximumHeight(new BigDecimal(parkingDto.getMaxHeight()));
                }
                pmrParkingArea.setName(new EmbeddableMultilingualString("Zone PMR", "FR"));
                pmrParkingArea.setTotalCapacity(new BigInteger(parkingDto.getDisabledParkingNb()));
                parking.getParkingAreas().add(pmrParkingArea);
            }else{
                parking.setAllAreasWheelchairAccessible(false);
            }
            parkingProperties.getSpaces().add(pmrCapacity);


            //Nombre de places pour véhicules électriques
            if(!parkingDto.getElectricVehicleNb().isEmpty() && Integer.parseInt(parkingDto.getElectricVehicleNb())>=1){
                parking.setRechargingAvailable(true);
                totalCapacity.setNumberOfSpacesWithRechargePoint(BigInteger.valueOf(Long.parseLong(parkingDto.getElectricVehicleNb())));
            }else{
                parking.setRechargingAvailable(false);
            }

            //Nombre de places pour le covoiturage
            if (!parkingDto.getCarPoolingNb().isEmpty() && Integer.parseInt(parkingDto.getCarPoolingNb()) >= 1) {
                parking.setCarpoolingAvailable(true);

                ParkingArea carPoolParkingArea = new ParkingArea();
                carPoolParkingArea.setSpecificParkingAreaUsage(SpecificParkingAreaUsageEnumeration.CARPOOL);
                carPoolParkingArea.setPublicUse(PublicUseEnumeration.ALL);
                carPoolParkingArea.setVersion(1L);
                if (parkingDto.getMaxHeight() != null && !parkingDto.getMaxHeight().equalsIgnoreCase("N/A")){
                    carPoolParkingArea.setMaximumHeight(new BigDecimal(parkingDto.getMaxHeight()));
                }
                carPoolParkingArea.setName(new EmbeddableMultilingualString("Zone réservée aux covoitureurs", "FR"));
                carPoolParkingArea.setTotalCapacity(BigInteger.valueOf(Long.parseLong(parkingDto.getCarPoolingNb())));
                parking.getParkingAreas().add(carPoolParkingArea);
            } else {
                parking.setCarpoolingAvailable(false);
            }

            //Nombre de places pour l'autopartage
            if (!parkingDto.getCarSharingNb().isEmpty() && Integer.parseInt(parkingDto.getCarSharingNb()) >= 1) {
                parking.setCarsharingAvailable(true);
                totalCapacity.setNumberOfCarsharingSpaces(BigInteger.valueOf(Long.parseLong(parkingDto.getCarSharingNb())));

                ParkingArea carSharingArea = new ParkingArea();
                carSharingArea.setSpecificParkingAreaUsage(SpecificParkingAreaUsageEnumeration.CARSHARE);
                carSharingArea.setPublicUse(PublicUseEnumeration.ALL);
                carSharingArea.setVersion(1L);
                if (parkingDto.getMaxHeight() != null && !parkingDto.getMaxHeight().equalsIgnoreCase("N/A")){
                    carSharingArea.setMaximumHeight(new BigDecimal(parkingDto.getMaxHeight()));
                }
                carSharingArea.setName(new EmbeddableMultilingualString("Zone Autopartage", "FR"));
                carSharingArea.setTotalCapacity(BigInteger.valueOf(Long.parseLong(parkingDto.getCarSharingNb())));
                parking.getParkingAreas().add(carSharingArea);
            } else {
                parking.setCarsharingAvailable(false);
            }
            parkingProperties.getSpaces().add(totalCapacity);

            //Emplacement du parking
            parking.setCentroid(geometryFactory.createPoint(new Coordinate(Double.parseDouble(parkingDto.getXlong()), Double.parseDouble(parkingDto.getYlat()))));

            parking.getParkingAreas().add(parkingArea);
            parking.getParkingProperties().add(parkingProperties);

            return parking;
        }).collect(Collectors.toList());
    }


    private static void addAccessibilityAssessment(Parking parking) {

        AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
        accessibilityAssessment.setVersion(1);
        accessibilityAssessment.setCreated(Instant.now());
        accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.PARTIAL);
        accessibilityAssessment.setLimitations(new ArrayList<>());

        AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
        accessibilityLimitation.setCreated(Instant.now());
        accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.TRUE);
        accessibilityLimitation.setAudibleSignalsAvailable(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setEscalatorFreeAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setLiftFreeAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setStepFreeAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setVisualSignsAvailable(LimitationStatusEnumeration.UNKNOWN);

        accessibilityAssessment.getLimitations().add(accessibilityLimitation);
        parking.setAccessibilityAssessment(accessibilityAssessment);

    }

}
