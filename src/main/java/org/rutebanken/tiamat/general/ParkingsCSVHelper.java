package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.model.AccessibilityAssessment;
import org.rutebanken.tiamat.model.AccessibilityLimitation;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.LimitationStatusEnumeration;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.rest.dto.DtoParking;
import org.rutebanken.tiamat.service.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParkingsCSVHelper {

    public final static String DELIMETER_PARKING_ID_NAME = " : ";

    private final static Pattern patternXlongYlat = Pattern.compile("^-?([1-8]?[1-9]|[1-9]0)\\.{1}\\d{1,20}");

    private static GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();


    public static List<DtoParking> parseDocument(InputStream csvFile) throws IllegalArgumentException, IOException {

        Reader reader = new InputStreamReader(csvFile);
        List<DtoParking> dtoParkingList = new ArrayList<>();


        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDelimiter(';')
                .build()
                .parse(reader);

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

            parking.setName(new EmbeddableMultilingualString(parkingDto.getId() + DELIMETER_PARKING_ID_NAME + parkingDto.getName()));

            if(!parkingDto.getNbOfPlaces().isEmpty()){
                parking.setTotalCapacity(new BigInteger(parkingDto.getNbOfPlaces()));
            }


            parking.setParkingType(ParkingTypeEnumeration.PARKING_ZONE);
            parking.setBookingUrl(parkingDto.getUrl());
            parking.setVersion(1L);

            if(!parkingDto.getDisabledParkingNb().isEmpty() && parkingDto.getDisabledParkingNb().equals(parkingDto.getNbOfPlaces())){
                parking.setAllAreasWheelchairAccessible(true);
            }else{
                parking.setAllAreasWheelchairAccessible(false);
            }

//            if(!parkingDto.getNb_pmr().isEmpty() && Integer.valueOf(parkingDto.getNb_pmr())>1){
//                addAccessibilityAssessment(parking);
//            }


            if(!parkingDto.getElectricVehicleNb().isEmpty() && Integer.valueOf(parkingDto.getElectricVehicleNb())>=1){
                parking.setRechargingAvailable(true);
            }else{
                parking.setRechargingAvailable(false);
            }

            if (!parkingDto.getCarPoolingNb().isEmpty() && Integer.valueOf(parkingDto.getCarPoolingNb()) >= 1) {
                parking.setCarpoolingAvailable(true);
            } else {
                parking.setCarpoolingAvailable(false);
            }

            if (!parkingDto.getCarPoolingNb().isEmpty() && Integer.valueOf(parkingDto.getCarSharingNb()) >= 1) {
                parking.setCarsharingAvailable(true);
            } else {
                parking.setCarsharingAvailable(false);
            }

            parking.setCentroid(geometryFactory.createPoint(new Coordinate(Double.valueOf(parkingDto.getXlong()), Double.valueOf(parkingDto.getYlat()))));

            return parking;
        }).collect(Collectors.toList());
    }


    private static void addAccessibilityAssessment(Parking parking) {

        AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
        accessibilityAssessment.setVersion(1);
        accessibilityAssessment.setCreated(Instant.now());
        accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.PARTIAL);

        AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
        accessibilityLimitation.setCreated(Instant.now());
        accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.TRUE);
        accessibilityLimitation.setAudibleSignalsAvailable(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setEscalatorFreeAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setLiftFreeAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setStepFreeAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setVisualSignsAvailable(LimitationStatusEnumeration.UNKNOWN);

        accessibilityAssessment.setLimitations(Arrays.asList(accessibilityLimitation));

        parking.setAccessibilityAssessment(accessibilityAssessment);

    }

}
