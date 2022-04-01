package org.rutebanken.tiamat.general;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.AccessibilityAssessment;
import org.rutebanken.tiamat.model.AccessibilityLimitation;
import org.rutebanken.tiamat.model.LimitationStatusEnumeration;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.rest.dto.DtoParking;
import org.rutebanken.tiamat.service.Preconditions;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParkingsCSVHelper {

    public final static String DELIMETER_PARKING_ID_NAME = " : ";

    private final static String delimeterToIgnoreCommasInQuotes1 = ";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

    private final static Pattern patternXlongYlat = Pattern.compile("^-?([1-8]?[1-9]|[1-9]0)\\.{1}\\d{1,20}");

    private static GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();


    public static List<DtoParking> parseDocument(String csvFile) throws IllegalArgumentException{
        String csvRows = csvFile.substring(csvFile.indexOf("\n")+1);

        List<String> rowsParkings = Arrays.asList(csvRows.split("\n"));

        return rowsParkings.stream().map(row ->{

            String[] values = row.split(delimeterToIgnoreCommasInQuotes1);

            DtoParking parking = new DtoParking(
                    values[0],
                    values[1],
                    values[2],
                    values[3],
                    values[4],
                    values[5],
                    values[6],
                    values[7],
                    values[8],
                    values[9],
                    values[10],
                    values[11],
                    values[12],
                    values[13],
                    values[14],
                    values[15],
                    values[16],
                    values[17],
                    values[18],
                    values[19],
                    values[20],
                    values[21],
                    values[22],
                    values[23],
                    values[24],
                    values[25],
                    values[26],
                    values[27],
                    values[28],
                    values[29]
            );

            validateParking(parking);

            return parking;

        }).collect(Collectors.toList());
    }

    private static void validateParking(DtoParking parking) throws IllegalArgumentException{
        Preconditions.checkArgument(!parking.getId().isEmpty(),"ID is required in all your parkings" );
        Preconditions.checkArgument(!parking.getNom().isEmpty() ,"NAME is required to parking with Id "+parking.getId());
        Preconditions.checkArgument(patternXlongYlat.matcher(parking.getXlong()).matches(),"X Longitud is not correct in the parking with" + parking.getId());
        Preconditions.checkArgument(patternXlongYlat.matcher(parking.getYlat()).matches(),"Y Latitud is not correct in the parking with" + parking.getId());
    }

    public static void checkDuplicatedParkings(List<DtoParking> parkings) throws IllegalArgumentException{
        List <String> compositeKey = parkings.stream().map(parking -> parking.getId()+parking.getNom()).collect(Collectors.toList());
        Set listWithoutDuplicatedValues = new HashSet(compositeKey);
        if(compositeKey.size()>listWithoutDuplicatedValues.size()) throw new IllegalArgumentException("There are duplicated parkings in your CSV File 'With the same ID & Name'");
    }


    public static List<Parking> mapFromDtoToEntity(List<DtoParking> dtoParkingsCSV){
        return  dtoParkingsCSV.stream().map(parkingDto -> {

            Parking parking = new Parking();

            parking.setName(new EmbeddableMultilingualString(parkingDto.getId() + DELIMETER_PARKING_ID_NAME + parkingDto.getNom()));

            if(!parkingDto.getNb_places().isEmpty()){
                parking.setTotalCapacity(new BigInteger(parkingDto.getNb_places()));
            }


            parking.setParkingType(ParkingTypeEnumeration.PARKING_ZONE);
            parking.setBookingUrl(parkingDto.getUrl());
            parking.setVersion(1L);

            if(!parkingDto.getNb_pmr().isEmpty() && parkingDto.getNb_pmr().equals(parkingDto.getNb_places())){
                parking.setAllAreasWheelchairAccessible(true);
            }else{
                parking.setAllAreasWheelchairAccessible(false);
            }

//            if(!parkingDto.getNb_pmr().isEmpty() && Integer.valueOf(parkingDto.getNb_pmr())>1){
//                addAccessibilityAssessment(parking);
//            }


            if(!parkingDto.getNb_voitures_electriques().isEmpty() && Integer.valueOf(parkingDto.getNb_voitures_electriques())>=1){
                parking.setRechargingAvailable(true);
            }else{
                parking.setRechargingAvailable(false);
            }

            if (!parkingDto.getNb_covoit().isEmpty() && Integer.valueOf(parkingDto.getNb_covoit()) >= 1) {
                parking.setCarpoolingAvailable(true);
            } else {
                parking.setCarpoolingAvailable(false);
            }

            if (!parkingDto.getNb_covoit().isEmpty() && Integer.valueOf(parkingDto.getNb_autopartage()) >= 1) {
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
