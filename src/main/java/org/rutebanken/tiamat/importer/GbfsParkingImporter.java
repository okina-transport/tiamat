package org.rutebanken.tiamat.importer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.collections.CollectionUtils;
import org.entur.gbfs.http.GBFSHttpClient;
import org.entur.gbfs.mapper.GBFSMapper;
import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.model.FileValidationError;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.model.ValidationResult;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFS;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSFeed;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSGbfs;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStation;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStationInformation;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSSystemInformation;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSVehicleTypes;
import org.rutebanken.tiamat.externalapis.gbfs.mapper.StationInformationMapper;
import org.rutebanken.tiamat.externalapis.gbfs.mapper.SystemInformationMapper;
import org.rutebanken.tiamat.externalapis.gbfs.GbfsHttpStatusException;
import org.rutebanken.tiamat.externalapis.gbfs.GbfsFieldError;
import org.rutebanken.tiamat.externalapis.gbfs.GbfsFieldErrorExtractor;
import org.rutebanken.tiamat.externalapis.gbfs.GbfsFieldLengthChecker;
import org.rutebanken.tiamat.general.GbfsFieldTooLongException;
import org.rutebanken.tiamat.validator.FieldLengthValidator;
import org.rutebanken.tiamat.general.AnalyzeImportException;
import org.rutebanken.tiamat.general.GbfsHttpErrorException;
import org.rutebanken.tiamat.general.GbfsInvalidEncodingException;
import org.rutebanken.tiamat.general.GbfsInvalidJsonException;
import org.rutebanken.tiamat.general.DuplicatedIdentifierException;
import org.rutebanken.tiamat.general.GbfsNoStationException;
import org.rutebanken.tiamat.general.GbfsFieldErrorException;
import org.rutebanken.tiamat.general.MissingGbfsFeedException;
import org.rutebanken.tiamat.general.UnsupportedGbfsVersionException;
import org.rutebanken.tiamat.model.job.ImportWarnings;
import org.rutebanken.tiamat.model.job.JobError;
import org.rutebanken.tiamat.model.job.JobErrorSeverity;
import org.rutebanken.tiamat.general.Utf8Helper;
import org.rutebanken.tiamat.model.Organisation;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportData;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportParams;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.service.parking.OrganisationsImportedService;
import org.rutebanken.tiamat.service.parking.ParkingsImportedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GbfsParkingImporter {

    private static final Logger logger = LoggerFactory.getLogger(GbfsParkingImporter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectReader STRICT_JSON_READER = MAPPER.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final String GBFS_V2_3 = "2.3";
    private static final String GBFS_V3_0 = "3.0";
    private static final List<String> SUPPORTED_GBFS_VERSIONS = List.of(GBFS_V2_3, GBFS_V3_0);
    private static final List<GBFSFeed.Name> REQUIRED_FEEDS = List.of(
            GBFSFeed.Name.STATION_INFORMATION,
            GBFSFeed.Name.SYSTEM_INFORMATION,
            GBFSFeed.Name.VEHICLE_TYPES
    );
    private static final List<GBFSFeed.Name> OPTIONAL_FEEDS = List.of(GBFSFeed.Name.SYSTEM_PRICING_PLANS);

    private final GBFSHttpClient gbfsHttpClient;
    private final GBFSMapper gbfsMapper; // maps gbfs v2 to v3 and vice versa
    private final GbfsValidator gbfsValidator;
    private final OrganisationsImportedService organisationsImportedService;
    private final ParkingsImportedService parkingsImportedService;
    private final FieldLengthValidator fieldLengthValidator;

    public GbfsParkingImporter(GBFSHttpClient gbfsHttpClient, GBFSMapper gbfsMapper, GbfsValidator gbfsValidator, OrganisationsImportedService organisationsImportedService, ParkingsImportedService parkingsImportedService, FieldLengthValidator fieldLengthValidator) {
        this.gbfsHttpClient = gbfsHttpClient;
        this.gbfsMapper = gbfsMapper;
        this.gbfsValidator = gbfsValidator;
        this.organisationsImportedService = organisationsImportedService;
        this.parkingsImportedService = parkingsImportedService;
        this.fieldLengthValidator = fieldLengthValidator;
    }

    /**
     * Retrieve GBFS data required to perform a GBFS parking import.
     * First it will request gbfs.json feed to get URL of required GBFS feed to perform GBFS import i.e.
     * station_information.json, system_information.json and vehicle_types.json. It will then request each required
     * feed one by one, validate them then return those feeds. If GBFS feeds are in version 2.3, it will convert them to
     * v3 feeds.
     *
     * @param gbfsJsonUri URI to gbfs.json file to retrieve GBFS data from (GBFS API must be version 2.3 or 3.0 GBFS)
     * @return GBFS data retrieved and parsed and validated from gbfsJsonUri
     * @throws TiamatBusinessException when requesting GBFS through HTTP fails (network/availability issue)
     * @throws AnalyzeImportException  when the retrieved GBFS data is invalid, classified as:
     *                                 <ul>
     *                                 <li>{@link AnalyzeImportErrorType#ENCODING} when a GBFS file is not valid UTF-8</li>
     *                                 <li>{@link AnalyzeImportErrorType#HTTP_ERROR} when a GBFS file is answered with an
     *                                 HTTP error status (ex: 503)</li>
     *                                 <li>{@link AnalyzeImportErrorType#NOT_GBFS_FILE} when the target url does not
     *                                 return a valid gbfs.json file</li>
     *                                 <li>{@link AnalyzeImportErrorType#UNSUPPORTED_GBFS_VERSION} when the GBFS version
     *                                 is neither 2.3 nor 3.0</li>
     *                                 <li>{@link AnalyzeImportErrorType#MISSING_REQUIRED_GBFS_FEED} when
     *                                 station_information, system_information or vehicle_types is missing (a missing
     *                                 system_pricing_plans is only reported as a
     *                                 {@link AnalyzeImportErrorType#MISSING_OPTIONAL_GBFS_FEED} warning)</li>
     *                                 <li>{@link AnalyzeImportErrorType#INVALID_JSON} when a downloaded feed is not a
     *                                 readable JSON object</li>
     *                                 <li>{@link AnalyzeImportErrorType#NO_DATA_ROWS} when station_information.json does
     *                                 not contain any station</li>
     *                                 <li>{@link AnalyzeImportErrorType#UNEXPECTED} when a GBFS file cannot be read
     *                                 although it passed the validation</li>
     *                                 <li>{@link AnalyzeImportErrorType#MISSING_REQUIRED_FIELD} when required fields are
     *                                 missing in the feeds (ex: lat of a station)</li>
     *                                 <li>{@link AnalyzeImportErrorType#INVALID_FORMAT_NUMBER} when a numeric field is not
     *                                 a number (ex: lon of a station is "5,7")</li>
     *                                 <li>{@link AnalyzeImportErrorType#INVALID_FORMAT_COORDINATE} when the coordinates of
     *                                 a station are (0, 0) or out of range</li>
     *                                 <li>{@link AnalyzeImportErrorType#MISSING_DATA} when a GBFS feed's content fails
     *                                 GBFS schema validation for another reason</li>
     *                                 </ul>
     * @throws DuplicatedIdentifierException when a station_id or a vehicle_type_id is duplicated
     * @throws GbfsFieldTooLongException     when text values exceed the size of their database column
     */
    public GbfsParkingImportData getGBFSParkingImportData(URI gbfsJsonUri) throws TiamatBusinessException {
        byte[] rawGbfsJsonBytes = download(gbfsJsonUri, "gbfs.json");
        checkIsUtf8(rawGbfsJsonBytes, "gbfs.json");

        FileValidationResult gbfsFileValidation = checkIsGbfsJson(rawGbfsJsonBytes, gbfsJsonUri);

        String version = gbfsFileValidation.version();
        checkGbfsVersionIsSupported(version);

        GBFSGbfs gbfsV3;
        try {
            if (GBFS_V2_3.equals(version)) {
                GBFS gbfsV2 = MAPPER.readValue(gbfsFileValidation.fileContents(), GBFS.class);
                String locale = gbfsV2.getFeedsData().containsKey("fr") ? "fr" : gbfsV2.getFeedsData().keySet().iterator().next();
                gbfsV3 = gbfsMapper.map(gbfsV2, locale);
            } else {
                gbfsV3 = MAPPER.readValue(gbfsFileValidation.fileContents(), GBFSGbfs.class);
            }
        } catch (IOException ex) {
            String msg = "Could not parse gbfs.json file (version: " + version + ")";
            logger.error(msg, ex);
            throw new AnalyzeImportException(AnalyzeImportErrorType.UNEXPECTED, msg);
        }

        List<GBFSFeed.Name> requiredFeeds = REQUIRED_FEEDS;
        List<GBFSFeed.Name> optionalFeedsIfPresentForValidation = checkFeedsArePresent(gbfsV3);

        Map<String, byte[]> feedBytes = new HashMap<>();

        for (GBFSFeed.Name feedName : Stream.concat(requiredFeeds.stream(), optionalFeedsIfPresentForValidation.stream()).toList()) {
            GBFSFeed feed = findFeed(gbfsV3, feedName);
            String file = feedName(feedName) + ".json";
            byte[] rawFeedBytes = download(URI.create(feed.getUrl()), file);
            checkIsUtf8(rawFeedBytes, file);
            checkIsJsonObject(rawFeedBytes, file);
            feedBytes.put(feedName(feedName), rawFeedBytes);
        }

        ValidationResult compositeValidation;
        try {
            Map<String, InputStream> asStreams = feedBytes.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new ByteArrayInputStream(e.getValue())));
            compositeValidation = gbfsValidator.validate(asStreams);
        } catch (Exception ex) {
            String msg = "Error validating GBFS feeds (composite validation)";
            logger.error(msg, ex);
            throw new AnalyzeImportException(AnalyzeImportErrorType.UNEXPECTED, msg);
        }

        checkSchemaErrors(compositeValidation, Stream.concat(requiredFeeds.stream(), optionalFeedsIfPresentForValidation.stream()).toList(), feedBytes);
        for (GBFSFeed.Name feedName : requiredFeeds) {
            String key = feedName.toString().toLowerCase();
            FileValidationResult res = compositeValidation.files().get(key);
            if (res == null || !res.exists()) {
                String msg = "Url for " + key + ".json is not valid";
                logger.error(msg);
                throw new AnalyzeImportException(AnalyzeImportErrorType.UNEXPECTED, msg);
            }
        }

        GBFSStationInformation stationInformation = null;
        GBFSSystemInformation systemInformation = null;
        GBFSVehicleTypes vehicleTypes = null;

        try {
            if (GBFS_V2_3.equals(version)) {
                var stationInfoV2 = MAPPER.readValue(
                        feedBytes.get(GBFSFeed.Name.STATION_INFORMATION.toString().toLowerCase()),
                        org.mobilitydata.gbfs.v2_3.station_information.GBFSStationInformation.class);
                stationInformation = gbfsMapper.map(stationInfoV2, "fr");

                var systemInfoV2 = MAPPER.readValue(
                        feedBytes.get(GBFSFeed.Name.SYSTEM_INFORMATION.toString().toLowerCase()),
                        org.mobilitydata.gbfs.v2_3.system_information.GBFSSystemInformation.class);
                systemInformation = gbfsMapper.map(systemInfoV2, "fr");

                var vehicleTypesV2 = MAPPER.readValue(
                        feedBytes.get(GBFSFeed.Name.VEHICLE_TYPES.toString().toLowerCase()),
                        org.mobilitydata.gbfs.v2_3.vehicle_types.GBFSVehicleTypes.class);
                vehicleTypes = gbfsMapper.map(vehicleTypesV2, "fr");
            } else {
                stationInformation = MAPPER.readValue(
                        feedBytes.get(GBFSFeed.Name.STATION_INFORMATION.toString().toLowerCase()),
                        GBFSStationInformation.class);
                systemInformation = MAPPER.readValue(
                        feedBytes.get(GBFSFeed.Name.SYSTEM_INFORMATION.toString().toLowerCase()),
                        GBFSSystemInformation.class);
                vehicleTypes = MAPPER.readValue(
                        feedBytes.get(GBFSFeed.Name.VEHICLE_TYPES.toString().toLowerCase()),
                        GBFSVehicleTypes.class);
            }
        } catch (IOException ex) {
            String msg = "Could not parse one of the GBFS feed files (version: " + version + ")";
            logger.error(msg, ex);
            throw new AnalyzeImportException(AnalyzeImportErrorType.UNEXPECTED, msg);
        }

        checkHasStations(stationInformation);
        checkNoDuplicatedIds(feedName(GBFSFeed.Name.STATION_INFORMATION) + ".json", "station_id",
                stationInformation.getData().getStations().stream().map(station -> station.getStationId()).toList());
        if (vehicleTypes != null && vehicleTypes.getData() != null && vehicleTypes.getData().getVehicleTypes() != null) {
            checkNoDuplicatedIds(feedName(GBFSFeed.Name.VEHICLE_TYPES) + ".json", "vehicle_type_id",
                    vehicleTypes.getData().getVehicleTypes().stream().map(vehicleType -> vehicleType.getVehicleTypeId()).toList());
        }
        checkFieldLengths(systemInformation, stationInformation);
        warnOnMissingCapacity(stationInformation);
        warnOnTruncatedAddress(stationInformation);

        return new GbfsParkingImportData(stationInformation, systemInformation, vehicleTypes);
    }

    /**
     * Blocking control: every file of the GBFS feed must be encoded in valid UTF-8.
     *
     * @param file name of the file, reported to the user (ex: station_information.json)
     * @throws GbfsInvalidEncodingException when the file is not valid UTF-8
     */
    private static void checkIsUtf8(byte[] bytes, String file) {
        try {
            Utf8Helper.decodeStrictUtf8(bytes, file);
        } catch (AnalyzeImportException ex) {
            GbfsInvalidEncodingException error = new GbfsInvalidEncodingException(file);
            logger.error(error.getMessage());
            throw error;
        }
    }

    /**
     * Blocking control: the downloaded feeds must respect the GBFS schema of their version. All the fields in error of
     * all the feeds are reported at once, one error per field:
     * <ul>
     * <li>missing required field (ex: lat missing in station 42 of station_information.json);</li>
     * <li>field which is not a number (ex: lon of station 42 is "5,7");</li>
     * <li>invalid coordinates of a station: (0, 0), or out of range.</li>
     * </ul>
     * The other schema errors (wrong value, wrong format…) are reported as a single MISSING_DATA error, their detail
     * is logged. The errors of the optional feeds are only warnings (see {@link #reportOptionalFeedErrorsAsWarnings}).
     *
     * @throws GbfsFieldErrorException when at least one field is in error
     * @throws AnalyzeImportException  {@link AnalyzeImportErrorType#MISSING_DATA} when there are only other schema
     *                                 errors
     */
    private static void checkSchemaErrors(ValidationResult validation, List<GBFSFeed.Name> feeds, Map<String, byte[]> feedBytes) {
        List<GbfsFieldError> fieldErrors = new ArrayList<>();
        boolean otherErrors = false;
        StringBuilder detail = new StringBuilder();
        for (GBFSFeed.Name feed : feeds) {
            String key = feedName(feed);
            if (feed == GBFSFeed.Name.STATION_INFORMATION) {
                fieldErrors.addAll(GbfsFieldErrorExtractor.findInvalidStationCoordinates(key + ".json", feedBytes.get(key)));
            }
            FileValidationResult res = validation.files().get(key);
            if (res == null || !res.exists() || CollectionUtils.isEmpty(res.errors())) {
                continue;
            }
            detail.append(key).append(".json contains error(s):\n")
                    .append(res.errors().stream().map(FileValidationError::toString).collect(Collectors.joining("\n")))
                    .append("\n");
            GbfsFieldErrorExtractor.Result result = GbfsFieldErrorExtractor.extract(key + ".json", res.errors(), feedBytes.get(key));
            if (OPTIONAL_FEEDS.contains(feed)) {
                reportOptionalFeedErrorsAsWarnings(key, result);
                continue;
            }
            fieldErrors.addAll(result.fieldErrors());
            otherErrors |= !result.otherErrors().isEmpty();
        }
        if (!fieldErrors.isEmpty()) {
            GbfsFieldErrorException error = new GbfsFieldErrorException(fieldErrors, otherErrors, detail.toString());
            logger.error(error.getMessage());
            throw error;
        }
        if (otherErrors) {
            logger.error(detail.toString());
            throw new AnalyzeImportException(AnalyzeImportErrorType.MISSING_DATA, detail.toString());
        }
    }

    /**
     * Non-blocking control: the errors of an optional feed (system_pricing_plans) are only reported as warnings, the
     * rest of the GBFS feed is imported. The pricing plans in error are ignored: note that the pricing plans are
     * currently only validated, they are not imported.
     */
    private static void reportOptionalFeedErrorsAsWarnings(String feed, GbfsFieldErrorExtractor.Result result) {
        String file = feed + ".json";
        for (GbfsFieldError fieldError : result.fieldErrors()) {
            ImportWarnings.add(GbfsFieldErrorException.toJobError(fieldError, JobErrorSeverity.WARNING));
        }
        if (!result.otherErrors().isEmpty()) {
            ImportWarnings.add(JobError.warning(AnalyzeImportErrorType.MISSING_DATA).withParam("file", file));
        }
        logger.warn("{} contains error(s), ignored because this feed is optional:\n{}", file,
                Stream.concat(result.fieldErrors().stream().map(Object::toString), result.otherErrors().stream().map(Object::toString))
                        .collect(Collectors.joining("\n")));
    }

    /**
     * Blocking control: the ids of the stations (station_id) and of the vehicle types (vehicle_type_id) must be unique
     * in their file. Each duplicated id is reported once, whatever its number of occurrences.
     *
     * @param file    name of the file (ex: station_information.json)
     * @param idField name of the id field (ex: station_id)
     * @param ids     the ids, in the order of the file
     * @throws DuplicatedIdentifierException when at least one id is duplicated
     */
    private static void checkNoDuplicatedIds(String file, String idField, List<String> ids) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicated = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !seen.add(id)) {
                duplicated.add(id);
            }
        }
        if (duplicated.isEmpty()) {
            return;
        }
        List<DuplicatedIdentifierException.Duplicate> duplicates = duplicated.stream()
                .map(id -> new DuplicatedIdentifierException.Duplicate(id, null))
                .toList();
        DuplicatedIdentifierException error = new DuplicatedIdentifierException(
                "Duplicated " + idField + " in " + file + ": " + String.join(", ", duplicated), file, idField, duplicates);
        logger.error(error.getMessage());
        throw error;
    }

    /**
     * Blocking control: the text values imported (names, urls, phone number…) must fit in their database column, whose
     * size is read from the database schema. All the values too long are reported at once, before anything is written
     * in database. The address of a station is not concerned: it is truncated (see {@link #warnOnTruncatedAddress}).
     *
     * @throws GbfsFieldTooLongException when at least one value is too long
     */
    private void checkFieldLengths(GBFSSystemInformation systemInformation, GBFSStationInformation stationInformation) {
        List<GbfsFieldLengthChecker.Violation> violations =
                GbfsFieldLengthChecker.check(systemInformation, stationInformation, fieldLengthValidator::maxLength);
        if (!violations.isEmpty()) {
            GbfsFieldTooLongException error = new GbfsFieldTooLongException(violations);
            logger.error(error.getMessage());
            throw error;
        }
    }

    /**
     * Non-blocking control: the capacity of each station should be given. A warning is reported for each station
     * without capacity field, the station is imported anyway.
     */
    private static void warnOnMissingCapacity(GBFSStationInformation stationInformation) {
        String file = feedName(GBFSFeed.Name.STATION_INFORMATION) + ".json";
        List<GBFSStation> stations = stationInformation.getData().getStations();
        for (int i = 0; i < stations.size(); i++) {
            GBFSStation station = stations.get(i);
            if (station.getCapacity() != null) {
                continue;
            }
            JobError warning = JobError.warning(AnalyzeImportErrorType.MISSING_OPTIONAL_FIELD)
                    .withParam("file", file)
                    .withParam("entity", "station");
            if (station.getStationId() != null) {
                warning.withParam("entityId", station.getStationId());
            } else {
                warning.withParam("position", String.valueOf(i + 1));
            }
            warning.withParam("field", "capacity");
            logger.warn("Station {} of {}: capacity missing", station.getStationId() != null ? station.getStationId() : "#" + (i + 1), file);
            ImportWarnings.add(warning);
        }
    }

    /**
     * Non-blocking control: the address of a station longer than {@link Parking#ADDRESS_MAX_LENGTH} characters (size of
     * the database column) is truncated when the station is mapped to a parking. A warning is reported for each
     * truncated address, the station is imported anyway.
     */
    private static void warnOnTruncatedAddress(GBFSStationInformation stationInformation) {
        String file = feedName(GBFSFeed.Name.STATION_INFORMATION) + ".json";
        List<GBFSStation> stations = stationInformation.getData().getStations();
        for (int i = 0; i < stations.size(); i++) {
            GBFSStation station = stations.get(i);
            String address = station.getAddress();
            if (address == null) {
                continue;
            }
            int length = address.codePointCount(0, address.length());
            if (length <= Parking.ADDRESS_MAX_LENGTH) {
                continue;
            }
            JobError warning = JobError.warning(AnalyzeImportErrorType.FIELD_TRUNCATED)
                    .withParam("file", file)
                    .withParam("entity", "station");
            if (station.getStationId() != null) {
                warning.withParam("entityId", station.getStationId());
            } else {
                warning.withParam("position", String.valueOf(i + 1));
            }
            warning.withParam("field", "address")
                    .withParam("originalLength", String.valueOf(length))
                    .withParam("maxLength", String.valueOf(Parking.ADDRESS_MAX_LENGTH));
            logger.warn("Station {} of {}: address truncated from {} to {} characters",
                    station.getStationId() != null ? station.getStationId() : "#" + (i + 1), file, length, Parking.ADDRESS_MAX_LENGTH);
            ImportWarnings.add(warning);
        }
    }

    /**
     * Blocking control: station_information.json must contain at least one station. The GBFS schema requires the
     * "stations" field, but accepts an empty list.
     *
     * @throws GbfsNoStationException when there is no station
     */
    private static void checkHasStations(GBFSStationInformation stationInformation) {
        if (stationInformation == null || stationInformation.getData() == null
                || CollectionUtils.isEmpty(stationInformation.getData().getStations())) {
            GbfsNoStationException error = new GbfsNoStationException(feedName(GBFSFeed.Name.STATION_INFORMATION) + ".json");
            logger.error(error.getMessage());
            throw error;
        }
    }

    /**
     * Blocking control: every downloaded feed must be a readable JSON object (a GBFS feed always is). Truncated JSON,
     * HTML page, empty file, JSON array or content followed by garbage are rejected.
     *
     * @param file name of the file, reported to the user (ex: system_information.json)
     * @throws GbfsInvalidJsonException when the file is not a readable JSON object
     */
    private static void checkIsJsonObject(byte[] bytes, String file) {
        String detail;
        try {
            JsonNode json = STRICT_JSON_READER.readTree(bytes);
            if (json != null && json.isObject()) {
                return;
            }
            detail = json == null || json.isMissingNode() ? "empty content" : "not a JSON object";
        } catch (JsonProcessingException ex) {
            detail = ex.getOriginalMessage();
        } catch (IOException ex) {
            detail = ex.getMessage();
        }
        GbfsInvalidJsonException error = new GbfsInvalidJsonException(file, detail);
        logger.error(error.getMessage());
        throw error;
    }

    /**
     * Controls the feeds declared in the gbfs.json file, before downloading any of them:
     * <ul>
     * <li>blocking control: station_information, system_information and vehicle_types are required. All the missing
     * required feeds are reported at once;</li>
     * <li>non-blocking control: system_pricing_plans is optional, a warning is reported when it is missing.</li>
     * </ul>
     *
     * @return the optional feeds present in the gbfs.json file, to be downloaded and validated
     * @throws MissingGbfsFeedException when at least one required feed is missing
     */
    private static List<GBFSFeed.Name> checkFeedsArePresent(GBFSGbfs gbfs) {
        List<GBFSFeed.Name> presentOptionalFeeds = new ArrayList<>();
        for (GBFSFeed.Name optionalFeed : OPTIONAL_FEEDS) {
            if (findFeed(gbfs, optionalFeed) != null) {
                presentOptionalFeeds.add(optionalFeed);
            } else {
                logger.warn("Optional feed {} missing in gbfs.json", feedName(optionalFeed));
                ImportWarnings.add(JobError.warning(AnalyzeImportErrorType.MISSING_OPTIONAL_GBFS_FEED)
                        .withParam("feed", feedName(optionalFeed)));
            }
        }

        List<String> missingRequiredFeeds = REQUIRED_FEEDS.stream()
                .filter(requiredFeed -> findFeed(gbfs, requiredFeed) == null)
                .map(GbfsParkingImporter::feedName)
                .toList();
        if (!missingRequiredFeeds.isEmpty()) {
            MissingGbfsFeedException error = new MissingGbfsFeedException(missingRequiredFeeds);
            logger.error(error.getMessage());
            throw error;
        }
        return presentOptionalFeeds;
    }

    private static GBFSFeed findFeed(GBFSGbfs gbfs, GBFSFeed.Name feedName) {
        if (gbfs.getData() == null || gbfs.getData().getFeeds() == null) {
            return null;
        }
        return gbfs.getData().getFeeds().stream()
                .filter(feed -> feed.getName() == feedName)
                .findFirst()
                .orElse(null);
    }

    /**
     * @return the name of the feed as written in gbfs.json (ex: vehicle_types)
     */
    private static String feedName(GBFSFeed.Name feedName) {
        return feedName.toString().toLowerCase();
    }

    /**
     * Blocking control: only the GBFS versions 2.3 and 3.0 are imported.
     * Note: the GBFS validator considers a gbfs.json file without "version" as a GBFS 1.0 file.
     *
     * @throws UnsupportedGbfsVersionException when the version of the gbfs.json file is not supported
     */
    private static void checkGbfsVersionIsSupported(String version) {
        if (!SUPPORTED_GBFS_VERSIONS.contains(version)) {
            UnsupportedGbfsVersionException error = new UnsupportedGbfsVersionException(version, SUPPORTED_GBFS_VERSIONS);
            logger.error(error.getMessage());
            throw error;
        }
    }

    /**
     * Blocking control: the target url must return a gbfs.json discovery file respecting the GBFS schema.
     * A file which is not a JSON object (HTML page, JSON array…) or which is another kind of JSON (ex: GeoJSON) is
     * rejected. Note: the GBFS validator considers a file without "version" as a GBFS 1.0 file.
     *
     * @return the validation result of the gbfs.json file
     * @throws AnalyzeImportException {@link AnalyzeImportErrorType#NOT_GBFS_FILE} when the file is not a valid gbfs.json
     */
    private FileValidationResult checkIsGbfsJson(byte[] rawGbfsJsonBytes, URI gbfsJsonUri) {
        ValidationResult gbfsValidation;
        try {
            gbfsValidation = gbfsValidator.validate(Map.of("gbfs", new ByteArrayInputStream(rawGbfsJsonBytes)));
        } catch (Exception ex) {
            String msg = "Target url " + gbfsJsonUri + " is not a gbfs.json file: its content is not a JSON object";
            logger.error(msg, ex);
            throw new AnalyzeImportException(AnalyzeImportErrorType.NOT_GBFS_FILE, msg);
        }
        FileValidationResult gbfsFileValidation = gbfsValidation.files().get("gbfs");
        if (gbfsFileValidation == null || !gbfsFileValidation.exists()) {
            String msg = "Target url " + gbfsJsonUri + " is not a gbfs.json file";
            logger.error(msg);
            throw new AnalyzeImportException(AnalyzeImportErrorType.NOT_GBFS_FILE, msg);
        }
        if (CollectionUtils.isNotEmpty(gbfsFileValidation.errors())) {
            String msg = "Target url " + gbfsJsonUri + " is not a gbfs.json file, it contains error(s):\n"
                    + gbfsFileValidation.errors().stream().map(FileValidationError::toString).collect(Collectors.joining("\n"));
            logger.error(msg);
            throw new AnalyzeImportException(AnalyzeImportErrorType.NOT_GBFS_FILE, msg);
        }
        return gbfsFileValidation;
    }

    /**
     * Downloads one file of the GBFS feed. Blocking control: any failure cancels the whole import, before anything
     * is written in database.
     *
     * @param uri  url of the file
     * @param file name of the file, used in error messages (ex: gbfs.json)
     * @return the raw content of the file
     * @throws GbfsHttpErrorException  when the server answers with an HTTP error status (ex: 503)
     * @throws TiamatBusinessException when the server cannot be reached (network/availability issue)
     */
    private byte[] download(URI uri, String file) throws TiamatBusinessException {
        try (InputStream is = gbfsHttpClient.getData(uri)) {
            if (is == null) {
                String msg = "No content received when requesting GBFS " + file + " from " + uri;
                logger.error(msg);
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_HTTP_RETRIEVAL_FAILED, msg);
            }
            return is.readAllBytes();
        } catch (GbfsHttpStatusException ex) {
            GbfsHttpErrorException error = new GbfsHttpErrorException(file, uri.toString(), ex.getStatusCode());
            logger.error(error.getMessage());
            throw error;
        } catch (IOException ex) {
            String msg = "Error requesting GBFS " + file + " from " + uri;
            logger.error(msg, ex);
            throw new TiamatBusinessException(TiamatBusinessException.GBFS_HTTP_RETRIEVAL_FAILED, msg);
        }
    }

    /**
     * @param params GBFS parking import params
     * @throws TiamatBusinessException see {@link #getGBFSParkingImportData(URI)}
     */
    public void importProcess(GbfsParkingImportParams params) throws TiamatBusinessException {
        GbfsParkingImportData data = this.getGBFSParkingImportData(params.getGlobalUrl());
        SystemInformationMapper systemInformationMapper = new SystemInformationMapper();
        Organisation incomingOrganisation = systemInformationMapper.toOrganisation(data.systemInformation());
        Organisation organisation = organisationsImportedService.createOrUpdateOrganisation(incomingOrganisation);
        // originalId is transient : it is lost when the organisation is reloaded from the database
        organisation.setOriginalId(incomingOrganisation.getOriginalId());
        StationInformationMapper stationInformationMapper = new StationInformationMapper();
        List<Parking> parkings = data.stationInformation().getData().getStations().stream()
                .map(gbfsStation -> stationInformationMapper.toParking(organisation, gbfsStation, data.vehicleTypes(), params.getParkingType(), params.getParkingAreaType()))
                .toList();
        parkingsImportedService.createOrUpdateParkings(parkings);
    }
}
