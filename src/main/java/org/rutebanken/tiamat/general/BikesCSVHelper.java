package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.model.CoveredEnumeration;
import org.rutebanken.tiamat.model.CycleStorageEnumeration;
import org.rutebanken.tiamat.model.CycleStorageEquipment;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingCapacity;
import org.rutebanken.tiamat.model.ParkingPaymentProcessEnumeration;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.ParkingUserEnumeration;
import org.rutebanken.tiamat.model.ParkingVehicleEnumeration;
import org.rutebanken.tiamat.model.PlaceEquipment;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.rest.dto.DtoBikeParking;
import org.rutebanken.tiamat.service.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.general.ParkingsCSVHelper.DELIMETER_PARKING_ID_NAME;

public class BikesCSVHelper {
    private static final Pattern patternXlongYlat = Pattern.compile("^-?([1-8]?[1-9]|[1-9]0)\\.{1}\\d{1,20}");
    private static final String HOOK_TYPE_KEY = "hook-type";
    private static final String ID_LOCAL = "id-local";

    private static GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();


    public static List<DtoBikeParking> parseDocument(InputStream csvFile) throws IllegalArgumentException, IOException {

        Iterable<CSVRecord> records = CSVHelper.getRecords(csvFile);
        List<DtoBikeParking> bikeParkingList = new ArrayList<>();


        for (CSVRecord csvRecord : records) {
            DtoBikeParking dtoBikeParking = createBikeParkingFromCSVRecord(csvRecord);
            validateBikeParking(dtoBikeParking);
            bikeParkingList.add(dtoBikeParking);
        }

        return bikeParkingList;
    }

    private static DtoBikeParking createBikeParkingFromCSVRecord(CSVRecord csvRecord) {
        DtoBikeParking dtoBikeParking = new DtoBikeParking();
        dtoBikeParking.setIdLocal(csvRecord.get(0));
        dtoBikeParking.setIdOsm(csvRecord.get(1));
        dtoBikeParking.setCodeCom(csvRecord.get(2));

        String coordinates = csvRecord.get(3);
        coordinates = coordinates.replace("[", "").replace("]", "");
        String[] coordinatesTab = coordinates.split(",");
        dtoBikeParking.setXlong(coordinatesTab[0]);
        dtoBikeParking.setYlat(coordinatesTab[1]);

        dtoBikeParking.setCapacite(csvRecord.get(4));
        dtoBikeParking.setCapaciteCargo(csvRecord.get(5));
        dtoBikeParking.setTypeAccroche(csvRecord.get(6));
        dtoBikeParking.setMobilier(csvRecord.get(7));
        dtoBikeParking.setAcces(csvRecord.get(8));
        dtoBikeParking.setGratuit(csvRecord.get(9));
        dtoBikeParking.setProtection(csvRecord.get(10));
        dtoBikeParking.setCouverture(csvRecord.get(11));
        dtoBikeParking.setSurveillance(csvRecord.get(12));
        dtoBikeParking.setLumiere(csvRecord.get(13));
        dtoBikeParking.setUrlInfo(csvRecord.get(14));
        dtoBikeParking.setdService(csvRecord.get(15));
        dtoBikeParking.setSource(csvRecord.get(16));
        dtoBikeParking.setProprietaire(csvRecord.get(17));
        dtoBikeParking.setGestionnaire(csvRecord.get(18));
        dtoBikeParking.setDateMaj(csvRecord.get(19));
        dtoBikeParking.setCommentaires(csvRecord.get(20));

        return dtoBikeParking;
    }

    private static void validateBikeParking(DtoBikeParking bikeParking) throws IllegalArgumentException {
        Preconditions.checkArgument(!bikeParking.getIdLocal().isEmpty(), "ID is required in all your parkings");
        Preconditions.checkArgument(patternXlongYlat.matcher(bikeParking.getXlong()).matches(), "X Longitud is not correct in the parking with " + bikeParking.getIdLocal());
        Preconditions.checkArgument(patternXlongYlat.matcher(bikeParking.getYlat()).matches(), "Y Latitud is not correct in the parking with " + bikeParking.getIdLocal());
        Preconditions.checkArgument(!bikeParking.getCapacite().isEmpty(), "Capacity is required in all your bike parkings");
        Preconditions.checkArgument(!bikeParking.getTypeAccroche().isEmpty(), "Hook type is required in all your bike parkings");
    }

    public static void checkDuplicatedBikeParkings(List<DtoBikeParking> bikeParkings) throws IllegalArgumentException {
        List<String> key = bikeParkings.stream().map(DtoBikeParking::getIdLocal).collect(Collectors.toList());
        List<String> duplicates = foundDuplicates(key);

        if (duplicates.size() > 0) {
            String duplicatesMsg = String.join(",", duplicates);
            throw new IllegalArgumentException("There are duplicated bike parkings in your CSV File 'With the same ID'. Duplicates:" + duplicatesMsg);
        }
    }

    private static List<String> foundDuplicates(List<String> fullList) {
        List<String> alreadyReadList = new ArrayList<>();
        List<String> duplicateList = new ArrayList<>();

        fullList.forEach(id -> {
            if (alreadyReadList.contains(id)) {
                duplicateList.add(id);
            } else {
                alreadyReadList.add(id);
            }
        });

        return duplicateList;
    }


    public static List<Parking> mapFromDtoToEntityParking(List<DtoBikeParking> dtoParkingsCSV, boolean isRentalBike) {
        return dtoParkingsCSV.stream().map(bikeParkingDto -> {

            Parking parking = new Parking();

            parking.setVersion(1L);

            parking.setDescription(new EmbeddableMultilingualString(bikeParkingDto.getCommentaires()));
            parking.setName(new EmbeddableMultilingualString(bikeParkingDto.getIdLocal()));

            if(StringUtils.isNotEmpty(bikeParkingDto.getIdOsm())){
                parking.setName(new EmbeddableMultilingualString(parking.getName().getValue() +  DELIMETER_PARKING_ID_NAME + bikeParkingDto.getIdOsm()));
            }


            //Emplacement du parking
            parking.setCentroid(geometryFactory.createPoint(new Coordinate(Double.parseDouble(bikeParkingDto.getXlong()), Double.parseDouble(bikeParkingDto.getYlat()))));


            if (Boolean.parseBoolean(bikeParkingDto.getCouverture())) {
                parking.setCovered(CoveredEnumeration.COVERED);
            }

            // Parking type
            if(isRentalBike){
                parking.setParkingType(ParkingTypeEnumeration.CYCLE_RENTAL);
            }
            else{
                if ("CONSIGNE COLLECTIVE FERMEE".equals(bikeParkingDto.getProtection()) || "BOX INDIVIDUEL FERME".equals(bikeParkingDto.getProtection())) {
                    parking.setParkingType(ParkingTypeEnumeration.OTHER);
                } else {
                    parking.setParkingType(ParkingTypeEnumeration.PARKING_ZONE);
                }
            }


            // Parking type ref
            if ("CONSIGNE COLLECTIVE FERMEE".equals(bikeParkingDto.getProtection())) {
                parking.setTypeOfParkingRef("SecureBikeParking");
            } else if ("BOX INDIVIDUEL FERME".equals(bikeParkingDto.getProtection())) {
                parking.setTypeOfParkingRef("IndividualBox");
            } else {
                parking.setTypeOfParkingRef(null);
            }

            if (Boolean.parseBoolean(bikeParkingDto.getSurveillance()) || "CONSIGNE COLLECTIVE FERMEE".equals(bikeParkingDto.getProtection())) {
                parking.setSecure(true);
            }


            //Capacité totale du parking
            ParkingCapacity totalCapacity = new ParkingCapacity();
            totalCapacity.setParkingUserType(ParkingUserEnumeration.ALL_USERS);
            parking.setTotalCapacity(new BigInteger(bikeParkingDto.getCapacite()));

            // Place equipments
            PlaceEquipment placeEquipment = new PlaceEquipment();
            CycleStorageEquipment cycleStorageEquipment = new CycleStorageEquipment();
            cycleStorageEquipment.setNumberOfSpaces(BigInteger.valueOf(Long.parseLong(bikeParkingDto.getCapacite())));

            switch (bikeParkingDto.getMobilier()){
                case "RACK DOUBLE ETAGE":
                case "RATELIER":
                    cycleStorageEquipment.setCycleStorageType(CycleStorageEnumeration.RACKS);
                case "CROCHET":
                case "SUPPORT GUIDON":
                case "POTELET":
                case "ARCEAU":
                case "ARCEAU VELO GRANDE TAILLE":
                    cycleStorageEquipment.setCycleStorageType(CycleStorageEnumeration.RAILINGS);
                    break;
                case "AUCUN EQUIPEMENT":
                case "AUTRE":
                    cycleStorageEquipment.setCycleStorageType(CycleStorageEnumeration.OTHER);
                    break;
            }


            if ("BOX INDIVIDUEL FERME".equals(bikeParkingDto.getProtection())) {
                cycleStorageEquipment.setCage(true);
            }
            if (Boolean.parseBoolean(bikeParkingDto.getCouverture())) {
                cycleStorageEquipment.setCovered(true);
            }
            placeEquipment.getInstalledEquipment().add(cycleStorageEquipment);

            parking.setPlaceEquipments(placeEquipment);


            // INSEE
            parking.setInsee(bikeParkingDto.getCodeCom());


            // Parking vehicle types
            parking.getParkingVehicleTypes().add(ParkingVehicleEnumeration.PEDAL_CYCLE);


            //Gratuité du parking ou non
            if (Boolean.parseBoolean(bikeParkingDto.getGratuit())) {
                parking.setFreeParkingOutOfHours(true);
                parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.FREE);
            } else {
                parking.setFreeParkingOutOfHours(false);
            }


            // Parking key values
            setIdLocal(parking, bikeParkingDto.getIdLocal());

            Set<String> existingIdOsm = parking.getOrCreateValues("id_osm");
            existingIdOsm.add(bikeParkingDto.getIdOsm());

            Set<String> existingDService = parking.getOrCreateValues("d_service");
            existingDService.add(bikeParkingDto.getdService());

            Set<String> existingSource = parking.getOrCreateValues("source");
            existingSource.add(bikeParkingDto.getSource());

            Set<String> existingProprietaire = parking.getOrCreateValues("proprietaire");
            existingProprietaire.add(bikeParkingDto.getProprietaire());

            Set<String> existingGestionnaire = parking.getOrCreateValues("gestionnaire");
            existingGestionnaire.add(bikeParkingDto.getGestionnaire());

            Set<String> existingDateMaj = parking.getOrCreateValues("date_maj");
            existingDateMaj.add(bikeParkingDto.getDateMaj());

            setHookType(parking, bikeParkingDto.getTypeAccroche());

            return parking;
        }).collect(Collectors.toList());
    }

    private static void setIdLocal(Parking parking, String idLocal){
        Value value = new Value(idLocal);
        parking.getKeyValues().put(ID_LOCAL, value);
    }

    private static void setHookType(Parking parking, String hookType){
        Value value = new Value(hookType);
        parking.getKeyValues().put(HOOK_TYPE_KEY, value);
    }
}
