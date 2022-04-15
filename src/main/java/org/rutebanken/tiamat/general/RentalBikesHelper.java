package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingPaymentProcessEnumeration;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.rest.dto.DtoParking;
import org.rutebanken.tiamat.service.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RentalBikesHelper {

    private static final Pattern patternXlongYlat = Pattern.compile("^-?([1-8]?[1-9]|[1-9]0)\\.{1}\\d{1,20}");
    private static final String HOOK_TYPE_KEY = "hook-type";

    private static GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();


    public static List<DtoParking> parseDocument(InputStream csvFile) throws IllegalArgumentException, IOException {

        Iterable<CSVRecord> records = CSVHelper.getRecords(csvFile);
        List<DtoParking> parkingList = new ArrayList<>();


        for (CSVRecord csvRecord : records) {
            DtoParking dtoParking = createParkingFromCSVRecord(csvRecord);
            validateParking(dtoParking);
            parkingList.add(dtoParking);
        }

        return parkingList;
    }

    private static DtoParking createParkingFromCSVRecord(CSVRecord csvRecord){
        DtoParking dtoParking = new DtoParking();
        dtoParking.setId(csvRecord.get(0));
        dtoParking.setHookType(csvRecord.get(6));

        String coordinates = csvRecord.get(3);
        coordinates = coordinates.replace("[", "").replace("]", "");
        String[] coordinatesTab = coordinates.split(",");
        dtoParking.setXlong(coordinatesTab[0]);
        dtoParking.setYlat(coordinatesTab[1]);

        dtoParking.setBikeNb(csvRecord.get(4));
        dtoParking.setFree(csvRecord.get(9));
        dtoParking.setInfo(csvRecord.get(20));

        return dtoParking;
    }

    private static void validateParking(DtoParking parking) throws IllegalArgumentException {
        Preconditions.checkArgument(!parking.getId().isEmpty(), "ID is required in all your parkings");
        Preconditions.checkArgument(patternXlongYlat.matcher(parking.getXlong()).matches(), "X Longitud is not correct in the parking with " + parking.getId());
        Preconditions.checkArgument(patternXlongYlat.matcher(parking.getYlat()).matches(), "Y Latitud is not correct in the parking with " + parking.getId());
    }

    public static void checkDuplicatedParkings(List<DtoParking> parkings) throws IllegalArgumentException {
        List<String> compositeKey = parkings.stream().map(parking -> parking.getId() + parking.getName()).collect(Collectors.toList());
        Set listWithoutDuplicatedValues = new HashSet(compositeKey);
        if (compositeKey.size() > listWithoutDuplicatedValues.size())
            throw new IllegalArgumentException("There are duplicated parkings in your CSV File 'With the same ID & Name'");
    }

    public static List<Parking> mapFromDtoToEntity(List<DtoParking> dtoParkingsCSV) {
        return dtoParkingsCSV.stream()
                            .map(RentalBikesHelper::convertDTOToParking)
                            .collect(Collectors.toList());
    }

    private static Parking convertDTOToParking(DtoParking dto){
        Parking parking = new Parking();

        parking.setName(new EmbeddableMultilingualString(dto.getId() + ParkingsCSVHelper.DELIMETER_PARKING_ID_NAME + dto.getName()));

        parking.setParkingType(ParkingTypeEnumeration.CYCLE_RENTAL);
        setHookType(parking, dto.getHookType());
        parking.setCentroid(geometryFactory.createPoint(new Coordinate(Double.valueOf(dto.getXlong()), Double.valueOf(dto.getYlat()))));

        if (StringUtils.isNotEmpty(dto.getBikeNb())){
            parking.setTotalCapacity(new BigInteger(dto.getBikeNb()));
        }


        boolean isFree = Boolean.parseBoolean(dto.getFree());
        if (isFree){
            parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.FREE);
        }


        return parking;
    }

    private static void setHookType(Parking parking, String hookType){
        Value value = new Value(hookType);
        parking.getKeyValues().put(HOOK_TYPE_KEY,value);
    }

}
