package org.rutebanken.tiamat.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStationInformation;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSSystemInformation;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSVehicleTypes;
import org.rutebanken.tiamat.externalapis.gbfs.mapper.StationInformationMapper;
import org.rutebanken.tiamat.externalapis.gbfs.mapper.SystemInformationMapper;
import org.rutebanken.tiamat.model.Organisation;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportData;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportParams;
import org.rutebanken.tiamat.repository.OrganisationRepository;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
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

    private final GBFSHttpClient gbfsHttpClient;
    private final GBFSMapper gbfsMapper; // maps gbfs v2 to v3 and vice versa
    private final GbfsValidator gbfsValidator;
    private final OrganisationRepository organisationRepository;
    private final ParkingsImportedService parkingsImportedService;

    public GbfsParkingImporter(GBFSHttpClient gbfsHttpClient, GBFSMapper gbfsMapper, GbfsValidator gbfsValidator, OrganisationRepository organisationRepository, ParkingsImportedService parkingsImportedService) {
        this.gbfsHttpClient = gbfsHttpClient;
        this.gbfsMapper = gbfsMapper;
        this.gbfsValidator = gbfsValidator;
        this.organisationRepository = organisationRepository;
        this.parkingsImportedService = parkingsImportedService;
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
     * @throws TiamatBusinessException when:
     *                                 <ul>
     *                                 <li>Requesting GBFS through HTTP fails</li>
     *                                 <li>Deserializing GBFS fails</li>
     *                                 <li>Any GBFS json file contains validation errors</li>
     *                                 <li>GBFS feed is not in version 2.3 or 3.0</li>
     *                                 </ul>
     */
    public GbfsParkingImportData getGBFSParkingImportData(URI gbfsJsonUri) throws TiamatBusinessException {
        InputStream rawGbfsJsonFile;
        try {
            rawGbfsJsonFile = gbfsHttpClient.getData(gbfsJsonUri);
        } catch (IOException ex) {
            String msg = "Error requesting gbfs.json file from " + gbfsJsonUri;
            logger.error(msg, ex);
            throw new TiamatBusinessException(TiamatBusinessException.GBFS_HTTP_RETRIEVAL_FAILED, msg);
        }

        ValidationResult gbfsValidation;
        try {
            gbfsValidation = gbfsValidator.validate(Map.of("gbfs", rawGbfsJsonFile));
        } catch (Exception ex) {
            String msg = "Error validating gbfs.json file from " + gbfsJsonUri;
            logger.error(msg, ex);
            throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
        }
        FileValidationResult gbfsFileValidation = gbfsValidation.files().get("gbfs");
        if (gbfsFileValidation == null || !gbfsFileValidation.exists()) {
            String msg = "Target url " + gbfsJsonUri + " is not a gbfs.json file";
            logger.error(msg);
            throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
        }
        if (CollectionUtils.isNotEmpty(gbfsFileValidation.errors())) {
            String msg =
                    "gbfs.json contains error(s):\n" + gbfsFileValidation.errors().stream().map(FileValidationError::toString).collect(Collectors.joining("\n"));
            logger.error(msg);
            throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
        }

        GBFSGbfs gbfsV3;
        String version = gbfsFileValidation.version();
        try {
            if (version != null && version.startsWith("2.")) {
                GBFS gbfsV2 = MAPPER.readValue(gbfsFileValidation.fileContents(), GBFS.class);
                String locale = gbfsV2.getFeedsData().containsKey("fr") ? "fr" : gbfsV2.getFeedsData().keySet().iterator().next();
                gbfsV3 = gbfsMapper.map(gbfsV2, locale);
            } else if ("3.0".equals(version)) {
                gbfsV3 = MAPPER.readValue(gbfsFileValidation.fileContents(), GBFSGbfs.class);
            } else {
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, "Unsupported GBFS version: " + version);
            }
        } catch (IOException ex) {
            String msg = "Could not parse gbfs.json file (version: " + version + ")";
            logger.error(msg, ex);
            throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
        }

        List<GBFSFeed.Name> requiredFeeds = List.of(
                GBFSFeed.Name.STATION_INFORMATION,
                GBFSFeed.Name.SYSTEM_INFORMATION,
                GBFSFeed.Name.VEHICLE_TYPES
        );

        List<GBFSFeed.Name> optionalFeedsIfPresentForValidation = new ArrayList<>();

        boolean hasPricingPlans = gbfsV3.getData().getFeeds().stream()
                .anyMatch(f -> f.getName() == GBFSFeed.Name.SYSTEM_PRICING_PLANS);
        if (hasPricingPlans) {
            optionalFeedsIfPresentForValidation.add(GBFSFeed.Name.SYSTEM_PRICING_PLANS);
        }

        Map<String, byte[]> feedBytes = new HashMap<>();

        for (GBFSFeed.Name feedName : Stream.concat(requiredFeeds.stream(), optionalFeedsIfPresentForValidation.stream()).toList()) {
            GBFSFeed feed = gbfsV3.getData().getFeeds().stream()
                    .filter(f -> f.getName() == feedName)
                    .findFirst()
                    .orElse(null);

            if (feed == null) {
                if (requiredFeeds.contains(feedName)) {
                    String msg = "Could not find feed " + feedName + " in gbfs.json";
                    logger.error(msg);
                    throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
                } else {
                    continue;
                }
            }

            try (InputStream is = gbfsHttpClient.getData(URI.create(feed.getUrl()))) {
                feedBytes.put(feedName.toString().toLowerCase(), is.readAllBytes());
            } catch (IOException ex) {
                String msg = "Error requesting GBFS " + feedName + ".json from " + feed.getUrl();
                logger.error(msg, ex);
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_HTTP_RETRIEVAL_FAILED, msg);
            }
        }

        ValidationResult compositeValidation;
        try {
            Map<String, InputStream> asStreams = feedBytes.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new ByteArrayInputStream(e.getValue())));
            compositeValidation = gbfsValidator.validate(asStreams);
        } catch (Exception ex) {
            String msg = "Error validating GBFS feeds (composite validation)";
            logger.error(msg, ex);
            throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
        }

        for (GBFSFeed.Name feedName : requiredFeeds) {
            String key = feedName.toString().toLowerCase();
            FileValidationResult res = compositeValidation.files().get(key);
            if (res == null || !res.exists()) {
                String msg = "Url for " + key + ".json is not valid";
                logger.error(msg);
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
            }
            if (CollectionUtils.isNotEmpty(res.errors())) {
                String msg = key + ".json contains error(s):\n" +
                        res.errors().stream().map(FileValidationError::toString).collect(Collectors.joining("\n"));
                logger.error(msg);
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
            }
        }
        for (GBFSFeed.Name feedName : optionalFeedsIfPresentForValidation) {
            String key = feedName.toString().toLowerCase();
            FileValidationResult res = compositeValidation.files().get(key);
            if (res != null && res.exists() && CollectionUtils.isNotEmpty(res.errors())) {
                String msg = key + ".json contains error(s):\n" +
                        res.errors().stream().map(FileValidationError::toString).collect(Collectors.joining("\n"));
                logger.error(msg);
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
            }
        }

        GBFSStationInformation stationInformation = null;
        GBFSSystemInformation systemInformation = null;
        GBFSVehicleTypes vehicleTypes = null;

        try {
            if (version != null && version.startsWith("2.")) {
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
            throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
        }

        return new GbfsParkingImportData(stationInformation, systemInformation, vehicleTypes);
    }

    /**
     * @param params GBFS parking import params
     * @throws TiamatBusinessException see {@link #getGBFSParkingImportData(URI)}
     */
    public void importProcess(GbfsParkingImportParams params) throws TiamatBusinessException {
        GbfsParkingImportData data = this.getGBFSParkingImportData(params.getGlobalUrl());
        SystemInformationMapper systemInformationMapper = new SystemInformationMapper();
        Organisation organisation = systemInformationMapper.toOrganisation(data.systemInformation());
        Optional<org.rutebanken.tiamat.model.Organisation> optionalOrganisation = organisationRepository.findByName(organisation.getName());
        optionalOrganisation.ifPresent(value -> organisation.setId(value.getId()));
        organisationRepository.save(organisation);
        StationInformationMapper stationInformationMapper = new StationInformationMapper();
        List<Parking> parkings = data.stationInformation().getData().getStations().stream()
                .map(gbfsStation -> stationInformationMapper.toParking(organisation, gbfsStation, data.vehicleTypes(), params.getParkingType(), params.getParkingAreaType()))
                .toList();
        parkingsImportedService.createOrUpdateParkings(parkings);
    }
}
