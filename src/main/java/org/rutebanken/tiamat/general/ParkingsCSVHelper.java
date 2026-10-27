package org.rutebanken.tiamat.general;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.model.csv.ParkingCsvHeader;
import org.rutebanken.tiamat.model.job.AnalyzeImportError;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.rest.dto.DtoParking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParkingsCSVHelper {

    private static final Logger logger = LoggerFactory.getLogger(ParkingsCSVHelper.class);
    public static final String DEFAULT_OPERATOR = "technique";
    private static final BigDecimal DEFAULT_PARKING_AREA_MAXIMUM_HEIGHT = new BigDecimal(300); // 3 meters
    private static final Pattern patternXlongYlat = Pattern.compile("^-?(\\d*)\\.\\d{1,20}");
    private static final List<String> EXPECTED_HEADERS = ParkingCsvHeader.headerNames();
    private static final String NOT_AVAILABLE = "N/A";
    // French aliases followed by the NeTEx values of ParkingUserEnumeration
    private static final Map<String, ParkingUserEnumeration> USER_TYPE_MAPPING = buildUserTypeMapping();
    private static final List<String> USER_TYPE_ALLOWED_VALUES = List.copyOf(USER_TYPE_MAPPING.keySet());
    // values accepted by ParkingLayoutEnumeration.fromValue
    private static final List<String> WORK_TYPE_ALLOWED_VALUES = Stream.concat(
                    Stream.of("enclos_en_surface", "ouvrage"),
                    Arrays.stream(ParkingLayoutEnumeration.values()).map(ParkingLayoutEnumeration::value))
            .toList();

    private ParkingsCSVHelper() {
        throw new IllegalStateException();
    }

    private static Map<String, ParkingUserEnumeration> buildUserTypeMapping() {
        Map<String, ParkingUserEnumeration> mapping = new LinkedHashMap<>();
        mapping.put("tous", ParkingUserEnumeration.ALL);
        mapping.put("abonnés", ParkingUserEnumeration.REGISTERED);
        Arrays.stream(ParkingUserEnumeration.values()).forEach(userType -> mapping.put(userType.value(), userType));
        return Collections.unmodifiableMap(mapping);
    }

    public static List<DtoParking> parseDocument(InputStream csvFile) throws IllegalArgumentException, IOException {

        List<DtoParking> dtoParkingList = new ArrayList<>();
        List<AnalyzeImportError> rowErrors = new ArrayList<>();

        CSVParser parser = CSVHelper.getRecords(csvFile);
        List<String> actualHeaders = parser.getHeaderNames();
        if (actualHeaders.isEmpty()) {
            throw new AnalyzeImportException(AnalyzeImportErrorType.EMPTY_FILE, "The CSV file is empty");
        }
        CSVHelper.validateHeaders(EXPECTED_HEADERS, actualHeaders, "parking");

        for (CSVRecord csvRecord : parser) {
            // physical line where the record ends: blank lines and multi-line quoted fields are counted
            int fileLine = (int) csvRecord.getParser().getCurrentLineNumber();
            if (csvRecord.size() != EXPECTED_HEADERS.size()) {
                rowErrors.add(new AnalyzeImportError(AnalyzeImportErrorType.TEMPLATE,
                        "Expected " + EXPECTED_HEADERS.size() + " columns but found " + csvRecord.size(),
                        fileLine, null));
                continue;
            }

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
                    csvRecord.get(29),
                    csvRecord.get(30)
            );

            dtoParking.setLine(fileLine);

            List<AnalyzeImportError> validationErrors = validateParking(dtoParking, fileLine);
            validationErrors.addAll(validateNumericFields(dtoParking, fileLine));
            validationErrors.addAll(validateEnumFields(dtoParking, fileLine));
            if (!validationErrors.isEmpty()) {
                rowErrors.addAll(validationErrors);
            } else {
                dtoParkingList.add(dtoParking);
            }
        }

        // empty lines are skipped by the parser
        if (parser.getRecordNumber() == 0) {
            throw new AnalyzeImportException(AnalyzeImportErrorType.NO_DATA_ROWS, "The CSV file does not contain any parking");
        }

        if (!rowErrors.isEmpty()) {
            throw new AnalyzeImportException(rowErrors);
        }

        return dtoParkingList;
    }

    /**
     * Checks that the numeric fields can be parsed, as they are converted when mapping the parkings,
     * and that the coordinates are decimals.
     * Empty values are allowed (mandatory fields are checked by {@link #validateParking}).
     *
     * @param fileLine line number in the file, header included
     */
    private static List<AnalyzeImportError> validateNumericFields(DtoParking parking, int fileLine) {
        List<AnalyzeImportError> errors = new ArrayList<>();
        checkInteger(errors, ParkingCsvHeader.NB_OF_PLACES, parking.getNbOfPlaces(), parking.getId(), fileLine);
        checkInteger(errors, ParkingCsvHeader.NB_OF_PR, parking.getNbOfPr(), parking.getId(), fileLine);
        checkInteger(errors, ParkingCsvHeader.DISABLED_PARKING_NB, parking.getDisabledParkingNb(), parking.getId(), fileLine);
        checkInteger(errors, ParkingCsvHeader.ELECTRIC_VEHICLE_NB, parking.getElectricVehicleNb(), parking.getId(), fileLine);
        checkInteger(errors, ParkingCsvHeader.BIKE_NB, parking.getBikeNb(), parking.getId(), fileLine);
        checkInteger(errors, ParkingCsvHeader.ELECTRIC_BIKES_NB, parking.getElectricBikesNb(), parking.getId(), fileLine);
        checkInteger(errors, ParkingCsvHeader.CAR_SHARING_NB, parking.getCarSharingNb(), parking.getId(), fileLine);
        checkInteger(errors, ParkingCsvHeader.MOTORCYCLE_NB, parking.getMotorcycleNb(), parking.getId(), fileLine);
        checkInteger(errors, ParkingCsvHeader.CAR_POOLING_NB, parking.getCarPoolingNb(), parking.getId(), fileLine);

        checkCoordinate(errors, ParkingCsvHeader.XLONG, parking.getXlong(), parking.getId(), fileLine);
        checkCoordinate(errors, ParkingCsvHeader.YLAT, parking.getYlat(), parking.getId(), fileLine);

        String maxHeight = parking.getMaxHeight();
        if (StringUtils.isNotEmpty(maxHeight) && !NOT_AVAILABLE.equalsIgnoreCase(maxHeight)) {
            try {
                new BigDecimal(maxHeight);
            } catch (NumberFormatException e) {
                errors.add(invalidNumberError(ParkingCsvHeader.MAX_HEIGHT, maxHeight, parking.getId(), fileLine));
            }
        }
        return errors;
    }

    private static void checkInteger(List<AnalyzeImportError> errors, ParkingCsvHeader header, String value, String parkingId, int fileLine) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            errors.add(invalidNumberError(header, value, parkingId, fileLine));
        }
    }

    private static void checkCoordinate(List<AnalyzeImportError> errors, ParkingCsvHeader header, String value, String parkingId, int fileLine) {
        if (StringUtils.isBlank(value) || patternXlongYlat.matcher(value).matches()) {
            return;
        }
        errors.add(AnalyzeImportError.builder()
                .type(AnalyzeImportErrorType.INVALID_FORMAT_COORDINATE)
                .message("Invalid decimal coordinate '" + value + "' in field " + header.value() + " for parking with id " + parkingId)
                .line(fileLine)
                .field(header.value())
                .errorValue(value)
                .build());
    }

    /**
     * Checks that the enumerated fields contain one of the values handled when mapping the parkings.
     * Empty values are allowed (mandatory fields are checked by {@link #validateParking}).
     *
     * @param fileLine line number in the file, header included
     */
    private static List<AnalyzeImportError> validateEnumFields(DtoParking parking, int fileLine) {
        List<AnalyzeImportError> errors = new ArrayList<>();
        checkEnum(errors, ParkingCsvHeader.USER_TYPE, parking.getUserType(), USER_TYPE_ALLOWED_VALUES, parking.getId(), fileLine);
        checkEnum(errors, ParkingCsvHeader.WORK_TYPE, parking.getWorkType(), WORK_TYPE_ALLOWED_VALUES, parking.getId(), fileLine);
        return errors;
    }

    private static void checkEnum(List<AnalyzeImportError> errors, ParkingCsvHeader header, String value, List<String> allowedValues,
                                  String parkingId, int fileLine) {
        if (StringUtils.isBlank(value) || allowedValues.contains(value)) {
            return;
        }
        errors.add(AnalyzeImportError.builder()
                .type(AnalyzeImportErrorType.INVALID_FORMAT_ENUM)
                .message("Invalid value '" + value + "' in field " + header.value() + " for parking with id " + parkingId
                        + ". Allowed values: " + String.join(", ", allowedValues))
                .line(fileLine)
                .field(header.value())
                .errorValue(value)
                .allowedValues(allowedValues)
                .build());
    }

    private static AnalyzeImportError invalidNumberError(ParkingCsvHeader header, String value, String parkingId, int fileLine) {
        return AnalyzeImportError.builder()
                .type(AnalyzeImportErrorType.INVALID_FORMAT_NUMBER)
                .message("Invalid number '" + value + "' in field " + header.value() + " for parking with id " + parkingId)
                .line(fileLine)
                .field(header.value())
                .errorValue(value)
                .build();
    }

    private static List<AnalyzeImportError> validateParking(DtoParking parking, int line) {
        List<AnalyzeImportError> errors = new ArrayList<>();
        if (StringUtils.isBlank(parking.getId())) {
            errors.add(new AnalyzeImportError(AnalyzeImportErrorType.MISSING_DATA, "ID is required in all your parkings", line, ParkingCsvHeader.ID.value()));
        }
        if (StringUtils.isBlank(parking.getInsee())) {
            errors.add(new AnalyzeImportError(AnalyzeImportErrorType.MISSING_DATA, "INSEE is required for parking with id " + parking.getId(), line, ParkingCsvHeader.INSEE.value()));
        }
        if (StringUtils.isBlank(parking.getName())) {
            errors.add(new AnalyzeImportError(AnalyzeImportErrorType.MISSING_DATA, "NAME is required for parking with id " + parking.getId(), line, ParkingCsvHeader.NAME.value()));
        }
        if (StringUtils.isBlank(parking.getUserType())) {
            errors.add(new AnalyzeImportError(AnalyzeImportErrorType.MISSING_DATA, "TYPE_USAGERS is required for parking with id " + parking.getId(), line, ParkingCsvHeader.USER_TYPE.value()));
        }
        if (StringUtils.isBlank(parking.getFree())) {
            errors.add(new AnalyzeImportError(AnalyzeImportErrorType.MISSING_DATA, "GRATUIT is required for parking with id " + parking.getId(), line, ParkingCsvHeader.FREE.value()));
        }
        if (StringUtils.isBlank(parking.getNbOfPlaces())) {
            errors.add(new AnalyzeImportError(AnalyzeImportErrorType.MISSING_DATA, "NB_PLACES is required for parking with id " + parking.getId(), line, ParkingCsvHeader.NB_OF_PLACES.value()));
        }
        if (StringUtils.isBlank(parking.getMaxHeight())) {
            errors.add(new AnalyzeImportError(AnalyzeImportErrorType.MISSING_DATA, "HAUTEUR_MAX is required for parking with id " + parking.getId(), line, ParkingCsvHeader.MAX_HEIGHT.value()));
        }
        if (StringUtils.isBlank(parking.getSiretNumber())) {
            errors.add(new AnalyzeImportError(AnalyzeImportErrorType.MISSING_DATA, "NUM_SIRET is required for parking with id " + parking.getId(), line, ParkingCsvHeader.SIRET_NUMBER.value()));
        }
        if (StringUtils.isBlank(parking.getOperator())) {
            errors.add(new AnalyzeImportError(AnalyzeImportErrorType.MISSING_DATA, "OPERATOR is required for parking with id " + parking.getId(), line, ParkingCsvHeader.OPERATOR.value()));
        }
        if (StringUtils.isBlank(parking.getXlong())) {
            errors.add(new AnalyzeImportError(AnalyzeImportErrorType.MISSING_DATA, "X Longitude is required for parking with id " + parking.getId(), line, ParkingCsvHeader.XLONG.value()));
        }
        if (StringUtils.isBlank(parking.getYlat())) {
            errors.add(new AnalyzeImportError(AnalyzeImportErrorType.MISSING_DATA, "Y Latitude is required for parking with id " + parking.getId(), line, ParkingCsvHeader.YLAT.value()));
        }
        return errors;
    }

    /**
     * Fails if several parkings share the same ID and name.
     *
     * @throws DuplicatedIdentifierException with one message line per duplicated parking ID
     */
    public static void checkDuplicatedParkings(List<DtoParking> parkings, String fileName) throws DuplicatedIdentifierException {
        Set<String> seenKeys = new HashSet<>();
        List<DtoParking> duplicatedParkings = parkings.stream()
                .filter(parking -> !seenKeys.add(parking.getId() + parking.getName()))
                .toList();

        if (CollectionUtils.isNotEmpty(duplicatedParkings)) {
            String duplicatesMsg = duplicatedParkings.stream()
                    .map(parking -> parking.getId() + parking.getName())
                    .collect(Collectors.joining(","));
            List<DuplicatedIdentifierException.Duplicate> duplicates = duplicatedParkings.stream()
                    .map(parking -> new DuplicatedIdentifierException.Duplicate(parking.getId(), parking.getLine()))
                    .toList();
            throw new DuplicatedIdentifierException("There are duplicated parkings in your CSV File 'With the same ID & Name'. Duplicates:" + duplicatesMsg,
                    fileName, ParkingCsvHeader.ID.value(), duplicates);
        }
    }

    public static List<Parking> mapFromDtoToEntity(List<DtoParking> dtoParkingsCSV, ParkingLayoutEnumeration parkingLayoutEnumeration,
                                                   ParkingTypeEnumeration parkingTypeEnumeration, Boolean parkAndRideDetection) {
        return dtoParkingsCSV.stream().map(parkingDto -> {

            Parking parking = new Parking();
            parking.setParkingAreas(new ArrayList<>());
            parking.setParkingProperties(new ArrayList<>());

            ParkingArea parkingArea = new ParkingArea();


            ParkingProperties parkingProperties = new ParkingProperties();
            parkingProperties.setSpaces(new ArrayList<>());

            parking.setOriginalId(parkingDto.getId());
            parking.setName(new EmbeddableMultilingualString(parkingDto.getName()));

            //Type d'usagers du parking
            ParkingUserEnumeration userType = USER_TYPE_MAPPING.get(parkingDto.getUserType());
            if (userType != null) {
                parkingProperties.getParkingUserTypes().add(userType);
            }

            BigInteger parkAndRideCapacity = parkingDto.getNbOfPr().isEmpty() ? BigInteger.ZERO : new BigInteger(parkingDto.getNbOfPr());
            BigInteger nbPMR = parkingDto.getDisabledParkingNb().isEmpty() ? BigInteger.ZERO : new BigInteger(parkingDto.getDisabledParkingNb());


            if (parkAndRideDetection) {
                // detection based on field : nb_pr
                parking.setParkingType(parkAndRideCapacity.equals(BigInteger.ZERO) ? ParkingTypeEnumeration.PARKING_ZONE : ParkingTypeEnumeration.PARK_AND_RIDE);
            } else {
                parking.setParkingType(parkingTypeEnumeration);
            }

            setParkingAreaMaximumHeight(parkingDto, parkingArea);

            if (ParkingTypeEnumeration.PARK_AND_RIDE.equals(parking.getParkingType())) {
                ParkingArea parkAndRideArea = new ParkingArea();
                parkAndRideArea.setVersion(1L);
                parkAndRideArea.setSpecificParkingAreaUsage(SpecificParkingAreaUsageEnumeration.PARK_AND_RIDE);
                setParkingAreaMaximumHeight(parkingDto, parkAndRideArea);
                if (nbPMR != null) {
                    parkAndRideArea.setName(new EmbeddableMultilingualString("Zone PMR", "FR"));
                    parkAndRideArea.setTotalCapacity(nbPMR);

                    AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
                    accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.TRUE);

                    AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
                    accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.TRUE);
                    accessibilityAssessment.setLimitations(Collections.singletonList(accessibilityLimitation));

                    parking.setAccessibilityAssessment(accessibilityAssessment);
                } else {
                    parkAndRideArea.setName(new EmbeddableMultilingualString("Zone P+R", "FR"));
                    parkAndRideArea.setTotalCapacity(parkAndRideCapacity);
                }
                parking.getParkingAreas().add(parkAndRideArea);
            }

            if (StringUtils.isBlank(parkingDto.getWorkType())) {
                parking.setParkingLayout(parkingLayoutEnumeration);
            } else {
                parking.setParkingLayout(ParkingLayoutEnumeration.fromValue(parkingDto.getWorkType()));
            }

            parking.setBookingUrl(parkingDto.getUrl());
            parking.setVersion(1L);
            parkingArea.setVersion(1L);
            parking.setSiret(parkingDto.getSiretNumber());

            //Gratuité du parking ou non
            if (parkingDto.getFree() != null) {
                if (Boolean.parseBoolean(parkingDto.getFree())) {
                    parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.FREE);
                } else {
                    parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.PAY_AND_DISPLAY);
                    parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.PAY_BY_PREPAID_TOKEN);
                    parking.getParkingPaymentProcess().add(ParkingPaymentProcessEnumeration.PAY_BY_MOBILE_DEVICE);
                }
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
            if (!totalCapacityInt.equals(BigInteger.ZERO) && !parkingDto.getDisabledParkingNb().isEmpty()) {
                parking.setAllAreasWheelchairAccessible(parkingDto.getDisabledParkingNb().equals(parkingDto.getNbOfPlaces()));
                pmrCapacity.setNumberOfSpaces(new BigInteger(parkingDto.getDisabledParkingNb()));

                ParkingArea pmrParkingArea = new ParkingArea();
                pmrParkingArea.setSpecificParkingAreaUsage(SpecificParkingAreaUsageEnumeration.DISABLED);
                pmrParkingArea.setPublicUse(PublicUseEnumeration.DISABLED_PUBLIC_ONLY);
                pmrParkingArea.setVersion(1L);
                setParkingAreaMaximumHeight(parkingDto, pmrParkingArea);
                pmrParkingArea.setName(new EmbeddableMultilingualString("Zone PMR", "FR"));
                pmrParkingArea.setTotalCapacity(new BigInteger(parkingDto.getDisabledParkingNb()));
                parking.getParkingAreas().add(pmrParkingArea);
            } else {
                parking.setAllAreasWheelchairAccessible(false);
            }
            parkingProperties.getSpaces().add(pmrCapacity);


            //Nombre de places pour véhicules électriques
            if (!parkingDto.getElectricVehicleNb().isEmpty() && Integer.parseInt(parkingDto.getElectricVehicleNb()) >= 1) {
                parking.setRechargingAvailable(true);
                totalCapacity.setNumberOfSpacesWithRechargePoint(BigInteger.valueOf(Long.parseLong(parkingDto.getElectricVehicleNb())));
            }

            if (!parkingDto.getElectricVehicleNb().isEmpty() && Integer.parseInt(parkingDto.getElectricVehicleNb()) == 0) {
                parking.setRechargingAvailable(false);
                totalCapacity.setNumberOfSpacesWithRechargePoint(BigInteger.valueOf(Long.parseLong(parkingDto.getElectricVehicleNb())));
            }

            //Nombre de places pour le covoiturage
            if (!parkingDto.getCarPoolingNb().isEmpty() && Integer.parseInt(parkingDto.getCarPoolingNb()) >= 1) {
                parking.setCarpoolingAvailable(true);

                ParkingArea carPoolParkingArea = new ParkingArea();
                carPoolParkingArea.setSpecificParkingAreaUsage(SpecificParkingAreaUsageEnumeration.CARPOOL);
                carPoolParkingArea.setPublicUse(PublicUseEnumeration.ALL);
                carPoolParkingArea.setVersion(1L);
                setParkingAreaMaximumHeight(parkingDto, carPoolParkingArea);
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
                setParkingAreaMaximumHeight(parkingDto, carSharingArea);
                carSharingArea.setName(new EmbeddableMultilingualString("Zone Autopartage", "FR"));
                carSharingArea.setTotalCapacity(BigInteger.valueOf(Long.parseLong(parkingDto.getCarSharingNb())));
                parking.getParkingAreas().add(carSharingArea);
            } else {
                parking.setCarsharingAvailable(false);
            }

            //Nombre de places pour les vélos
            if (!parkingDto.getBikeNb().isEmpty() && Integer.parseInt(parkingDto.getBikeNb()) >= 1) {
                BigInteger numberValue = BigInteger.valueOf(Long.parseLong(parkingDto.getBikeNb()));

                createBikeOrTwoWheeledArea(parkingDto, parking, parkingProperties, totalCapacity,
                        ParkingVehicleEnumeration.PEDAL_CYCLE,
                        SpecificParkingAreaUsageEnumeration.PEDAL_CYCLE, numberValue,
                        "Zone réservée aux vélos");
            }

            //Nombre de places pour les vélos électriques
            if (!parkingDto.getElectricBikesNb().isEmpty() && Integer.parseInt(parkingDto.getElectricBikesNb()) >= 1) {
                BigInteger numberValue = BigInteger.valueOf(Long.parseLong(parkingDto.getElectricBikesNb()));

                createBikeOrTwoWheeledArea(parkingDto, parking, parkingProperties, totalCapacity,
                        ParkingVehicleEnumeration.TWO_WHEELED_VEHICLE,
                        SpecificParkingAreaUsageEnumeration.TWO_WHEELED_VEHICLE, numberValue,
                        "Zone réservée aux vélos électriques");
            }

            //Nombre de places pour les 2 roues
            if (!parkingDto.getMotorcycleNb().isEmpty() && Integer.parseInt(parkingDto.getMotorcycleNb()) >= 1) {
                BigInteger numberValue = BigInteger.valueOf(Long.parseLong(parkingDto.getMotorcycleNb()));

                createBikeOrTwoWheeledArea(parkingDto, parking, parkingProperties, totalCapacity,
                        ParkingVehicleEnumeration.TWO_WHEELED_VEHICLE,
                        SpecificParkingAreaUsageEnumeration.TWO_WHEELED_VEHICLE, numberValue,
                        "Zone réservée aux 2 roues");
            }

            parkingProperties.getSpaces().add(totalCapacity);

            //Emplacement du parking
            parking.setCentroid(ImporterUtils.createPoint(Double.parseDouble(parkingDto.getXlong()), Double.parseDouble(parkingDto.getYlat())));


            Optional<String> inseeOpt = ImporterUtils.getInseeFromLatLng(parking.getCentroid().getX(), parking.getCentroid().getY());
            parking.setInsee(inseeOpt.orElse(parkingDto.getInsee()));


            parking.getParkingAreas().add(parkingArea);
            parking.getParkingProperties().add(parkingProperties);

            if (StringUtils.isNotEmpty(parkingDto.getOperator())) {
                parking.setOperator(parkingDto.getOperator());
            }

            parking.setDescription(new EmbeddableMultilingualString(parkingDto.getInfo()));
            parking.setAddress(parkingDto.getAddress());

            if (CollectionUtils.isEmpty(parking.getParkingVehicleTypes())) {
                parking.getParkingVehicleTypes().add(ParkingVehicleEnumeration.UNDEFINED);
            }

            if (CollectionUtils.isNotEmpty(parking.getParkingProperties())) {
                parking.getParkingProperties().stream()
                        .filter(pp -> CollectionUtils.isEmpty(pp.getParkingVehicleTypes()))
                        .forEach(pp -> pp.getParkingVehicleTypes().add(ParkingVehicleEnumeration.UNDEFINED));
            }

            if (StringUtils.isBlank(parking.getOperator())) {
                parking.setOperator(DEFAULT_OPERATOR);
            } else {
                logger.warn("Undefind parking operator for parking {}", parking.getOriginalId());
            }

            return parking;
        }).collect(Collectors.toList());
    }

    private static void setParkingAreaMaximumHeight(DtoParking parkingDto, ParkingArea parkingArea) {
        //Hauteur maximum
        if (parkingDto.getMaxHeight() != null && !parkingDto.getMaxHeight().equalsIgnoreCase("N/A")) {
            parkingArea.setMaximumHeight(new BigDecimal(parkingDto.getMaxHeight()));
        } else {
            parkingArea.setMaximumHeight(DEFAULT_PARKING_AREA_MAXIMUM_HEIGHT);
        }
    }

    private static void createBikeOrTwoWheeledArea(DtoParking parkingDto,
                                                   Parking parking,
                                                   ParkingProperties parkingProperties,
                                                   ParkingCapacity totalCapacity,
                                                   ParkingVehicleEnumeration parkingVehicleEnumeration,
                                                   SpecificParkingAreaUsageEnumeration specificParkingAreaUsageEnumeration,
                                                   BigInteger numberValue,
                                                   String areaName) {

        ParkingArea newArea = new ParkingArea();
        newArea.setSpecificParkingAreaUsage(specificParkingAreaUsageEnumeration);
        newArea.setPublicUse(PublicUseEnumeration.ALL);
        newArea.setVersion(1L);

        setParkingAreaMaximumHeight(parkingDto, newArea);
        newArea.setName(new EmbeddableMultilingualString(areaName, "FR"));
        newArea.setTotalCapacity(numberValue);

        parkingProperties.getParkingVehicleTypes().add(parkingVehicleEnumeration);

        ParkingCapacity newCapacity = new ParkingCapacity();
        newCapacity.setParkingUserType(totalCapacity.getParkingUserType());
        newCapacity.setNumberOfSpaces(numberValue);
        newCapacity.setParkingVehicleType(parkingVehicleEnumeration);

        parkingProperties.getSpaces().add(newCapacity);
        newArea.setParkingProperties(parkingProperties);

        parking.getParkingAreas().add(newArea);
    }
}
