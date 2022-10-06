package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.rest.dto.DtoBikeParking;
import org.rutebanken.tiamat.rest.dto.DtoQuayResource;
import org.rutebanken.tiamat.service.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BikesCSVHelper {
    private static final Pattern patternXlongYlat = Pattern.compile("^-?([0-9]*)\\.{1}\\d{1,20}");

    private final static String DATA_GOUV_ENDPOINT = "https://api-adresse.data.gouv.fr/reverse/?lat=%s&lon=%s";
    private final static String GEO_API_GOUV_ENDPOINT = "https://geo.api.gouv.fr/communes?lat=%s&lon=%s&fields=nom,code,codesPostaux&format=json";

    private static GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();

    private static final Logger logger = LoggerFactory.getLogger(BikesCSVHelper.class);


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
        dtoBikeParking.setName(csvRecord.get(21));

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


    public static List<Parking> mapFromDtoToEntityParking(List<DtoBikeParking> dtoParkingsCSV, boolean isRentalBike) throws IllegalArgumentException{
        return dtoParkingsCSV.stream().map(bikeParkingDto -> {

            Parking parking = new Parking();

            parking.setVersion(1L);

            parking.setDescription(new EmbeddableMultilingualString(bikeParkingDto.getCommentaires()));
            if (bikeParkingDto.getName() != null && !bikeParkingDto.getName().isEmpty()) {
                parking.setName(new EmbeddableMultilingualString(bikeParkingDto.getName()));
            } else {
                parking.setName(new EmbeddableMultilingualString(buildBikeParkingName(bikeParkingDto, isRentalBike)));
            }

            //Emplacement du parking
            parking.setCentroid(geometryFactory.createPoint(new Coordinate(Double.parseDouble(bikeParkingDto.getXlong()), Double.parseDouble(bikeParkingDto.getYlat()))));


            if (Boolean.parseBoolean(bikeParkingDto.getCouverture())) {
                parking.setCovered(CoveredEnumeration.COVERED);
            }

            // Parking type
            if(isRentalBike){
                parking.setParkingType(ParkingTypeEnumeration.CYCLE_RENTAL);
            } else{
                parking.setParkingType(ParkingTypeEnumeration.OTHER);
            }


            // Parking type ref
            if ("CONSIGNE COLLECTIVE FERMEE".equals(bikeParkingDto.getProtection())) {
                parking.setTypeOfParkingRef("SecureBikeParking");
            } else if ("BOX INDIVIDUEL FERME".equals(bikeParkingDto.getProtection())) {
                parking.setTypeOfParkingRef("IndividualBox");
            } else {
                parking.setTypeOfParkingRef("BikeParking");
            }

            if (Boolean.parseBoolean(bikeParkingDto.getSurveillance()) || "CONSIGNE COLLECTIVE FERMEE".equals(bikeParkingDto.getProtection())) {
                parking.setSecure(true);
            }


            //Capacité totale du parking
            ParkingCapacity totalCapacity = new ParkingCapacity();
            totalCapacity.setParkingUserType(ParkingUserEnumeration.ALL_USERS);
            BigInteger totalCap = new BigInteger(bikeParkingDto.getCapacite());
            parking.setTotalCapacity(totalCap);
            totalCapacity.setNumberOfSpaces(totalCap);


            ParkingProperties parkingProps = new ParkingProperties();
            parkingProps.setSpaces(new ArrayList<>());
            parkingProps.getSpaces().add(totalCapacity);
            parking.setParkingProperties(new ArrayList<>());
            parking.getParkingProperties().add(parkingProps);

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
            if (bikeParkingDto.getGratuit() != null) {
                if (Boolean.parseBoolean(bikeParkingDto.getGratuit())) {
                    parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.FREE);
                } else {
                    parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.PAY_AND_DISPLAY);
                    parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.PAY_BY_PREPAID_TOKEN);
                    parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.PAY_BY_MOBILE_DEVICE);
                }
            }


            // Parking key values
            Set<String> existingIdLocal = parking.getOrCreateValues("id_local");
            existingIdLocal.add(bikeParkingDto.getIdLocal());

            if(StringUtils.isNotEmpty(bikeParkingDto.getIdOsm())){
                Set<String> existingIdOsm = parking.getOrCreateValues("id_osm");
                existingIdOsm.add(bikeParkingDto.getIdOsm());
            }

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

            Set<String> hookType = parking.getOrCreateValues("hook_type");
            hookType.add(bikeParkingDto.getTypeAccroche());

            return parking;
        }).collect(Collectors.toList());
    }

    private static String buildBikeParkingName(DtoBikeParking bikeParkingDto, Boolean isRentalBike) {
        String type = "";
        if (isRentalBike) {
            type = "VLS";
        } else if ("CONSIGNE COLLECTIVE FERMEE".equals(bikeParkingDto.getProtection())) {
            type = "CONSIGNE VELO";
        } else if ("BOX INDIVIDUEL FERME".equals(bikeParkingDto.getProtection())) {
            type = "BOX VELO";
        } else {
            type = "STATION VELO";
        }

        final String dataGouvUrl = String.format(DATA_GOUV_ENDPOINT, bikeParkingDto.getYlat(), bikeParkingDto.getXlong());
        final String geoApiGouvUrl = String.format(GEO_API_GOUV_ENDPOINT, bikeParkingDto.getXlong(), bikeParkingDto.getYlat());
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity response = null;
        String city = "";
        String street = "";

        try {
            response = restTemplate.exchange(dataGouvUrl, HttpMethod.GET, HttpEntity.EMPTY, String.class);
            JSONObject body = new JSONObject(Objects.requireNonNull(response.getBody()).toString());

            if (body.getJSONArray("features") != null && body.getJSONArray("features").length() > 0) {

                JSONObject properties = body.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
                city = properties.has("city") ? properties.getString("city") : "";
                street = properties.has("street") ? properties.getString("street") : "";

                return "[" + type + "], " + city + ", " + street;
            } else {

                response = restTemplate.exchange(geoApiGouvUrl, HttpMethod.GET, HttpEntity.EMPTY, Object.class);
                body = new JSONObject(Objects.requireNonNull(response.getBody()).toString());

                if (body.getString("nom") != null && !body.getString("nom").isEmpty()) {
                    city = body.getString("nom");
                    return "[" + type + "], " + city;
                } else {
                    throw new IllegalArgumentException("Impossible de trouver le nom du parking suivant : " + bikeParkingDto.getIdLocal());
                }
            }
        } catch (RestClientException | JSONException | IllegalArgumentException e) {
            logger.error("Error on parking name build", e);
            logger.error("dataGouvUrl : " + dataGouvUrl);
            logger.error("geoApiGouvUrl : " + geoApiGouvUrl);
            if (response != null && response.getBody() != null){
                logger.error(response.getBody().toString());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[" + type + "]");

            if (StringUtils.isNotEmpty(city)){
                sb.append(", " + city);
            }

            if (StringUtils.isNotEmpty(street)){
                sb.append(", " + street);
            }
            return sb.toString();
        }
    }
}
