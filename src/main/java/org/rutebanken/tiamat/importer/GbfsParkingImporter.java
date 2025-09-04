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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        // Request GBFS feed
        InputStream rawGbfsJsonFile;
        try {
            rawGbfsJsonFile = gbfsHttpClient.getData(gbfsJsonUri);
        } catch (
                IOException ex) {
            String msg = "Error requesting gbfs.json file from " + gbfsJsonUri;
            logger.error(msg, ex);
            throw new TiamatBusinessException(TiamatBusinessException.GBFS_HTTP_RETRIEVAL_FAILED, msg);
        }

        // Validate gbfs.json file
        ValidationResult gbfsValidation;
        try {
            gbfsValidation = gbfsValidator.validate(Map.of("gbfs", rawGbfsJsonFile));
        } catch (Exception ex) {
            String msg = "Error validating gbfs.json file from " + gbfsJsonUri;
            logger.error(msg, ex);
            throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
        }
        FileValidationResult gbfsFileValidation = gbfsValidation.files().get("gbfs");
        if (!gbfsFileValidation.exists()) {
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

        // if gbfs.json file is in v2.3 deserialize it to v2.3 gbfs.json entity then map it to v3.0 entity
        // if gbfs.json file is in v3.0 deserialize it to v3.0 gbfs.json entity
        // otherwise throw exception for invalid version
        GBFSGbfs gbfsV3;
        try {
            gbfsV3 = switch (gbfsFileValidation.version()) {
                case "2.0", "2.1", "2.2", "2.3" -> {
                    GBFS gbfsV2 =
                            MAPPER.readValue(gbfsFileValidation.fileContents(), GBFS.class);
                    yield gbfsMapper.map(gbfsV2, gbfsV2.getFeedsData().containsKey("fr") ? "fr" : gbfsV2.getFeedsData().keySet().iterator().next());
                }
                case "3.0" -> MAPPER.readValue(gbfsFileValidation.fileContents(), GBFSGbfs.class);
                default ->
                        throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, "Unsupported GBFS version: " + gbfsFileValidation.version());
            };
        } catch (IOException ex) {
            String msg = "Could not parse gbfs.json file content: " + gbfsFileValidation.fileContents();
            logger.error(msg, ex);
            throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
        }

        // retrieve station_information.json, system_information.json and vehicle_types.json files
        GBFSStationInformation stationInformation = null;
        GBFSSystemInformation systemInformation = null;
        GBFSVehicleTypes vehicleTypes = null;
        for (
                GBFSFeed.Name feedName : List.of(GBFSFeed.Name.STATION_INFORMATION, GBFSFeed.Name.SYSTEM_INFORMATION,
                GBFSFeed.Name.VEHICLE_TYPES)) {
            Optional<GBFSFeed> feed =
                    gbfsV3.getData().getFeeds().stream().filter(f -> f.getName() == feedName).findFirst();
            if (feed.isEmpty()) {
                String msg = "Could not find feed " + feedName + " in gbfs.json file";
                logger.error(msg);
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
            }
            try {
                rawGbfsJsonFile = gbfsHttpClient.getData(feed.get().getUrl());
            } catch (IOException ex) {
                String msg = "Errors requesting GBFS " + feedName + ".json from " + feed.get().getUrl();
                logger.error(msg, ex);
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_HTTP_RETRIEVAL_FAILED, msg);
            }
            ValidationResult feedValidation;
            try {
                feedValidation = gbfsValidator.validate(Map.of(feedName.toString().toLowerCase(), rawGbfsJsonFile));
            } catch (Exception ex) {
                String msg = "Error validating " + feedName + ".json file from " + feed.get().getUrl();
                logger.error(msg, ex);
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
            }
            FileValidationResult feedFileValidation = feedValidation.files().get(feedName.toString().toLowerCase());
            if (!feedFileValidation.exists()) {
                String msg = "Url " + feed.get().getUrl() + " is not a " + feedName + ".json file";
                logger.error(msg);
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
            }
            if (CollectionUtils.isNotEmpty(feedFileValidation.errors())) {
                String msg =
                        feedName + ".json contains error(s):\n" + feedFileValidation.errors().stream()
                                .map(FileValidationError::toString)
                                .collect(Collectors.joining("\n"));
                logger.error(msg);
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
            }
            try {
                if (List.of("2.0", "2.1", "2.2", "2.3").contains(gbfsFileValidation.version())) {
                    switch (feedName) {
                        case STATION_INFORMATION -> {
                            var stationInformationV2 = MAPPER.readValue(feedFileValidation.fileContents()
                                    , org.mobilitydata.gbfs.v2_3.station_information.GBFSStationInformation.class);
                            stationInformation = gbfsMapper.map(stationInformationV2, "fr");

                        }
                        case SYSTEM_INFORMATION -> {
                            var systemInformationV2 = MAPPER.readValue(feedFileValidation.fileContents(),
                                    org.mobilitydata.gbfs.v2_3.system_information.GBFSSystemInformation.class);
                            systemInformation = gbfsMapper.map(systemInformationV2, "fr");
                        }
                        case VEHICLE_TYPES -> {
                            var vehicleTypesV2 = MAPPER.readValue(feedFileValidation.fileContents(), org.mobilitydata.gbfs.v2_3.vehicle_types.GBFSVehicleTypes.class);
                            vehicleTypes = gbfsMapper.map(vehicleTypesV2, "fr");
                        }
                    }
                } else {
                    switch (feedName) {
                        case STATION_INFORMATION ->
                                stationInformation = MAPPER.readValue(feedFileValidation.fileContents()
                                        , GBFSStationInformation.class);
                        case SYSTEM_INFORMATION ->
                                systemInformation = MAPPER.readValue(feedFileValidation.fileContents(),
                                        GBFSSystemInformation.class);
                        case VEHICLE_TYPES -> vehicleTypes = MAPPER.readValue(feedFileValidation.fileContents(),
                                GBFSVehicleTypes.class);
                    }
                }
            } catch (IOException ex) {
                String msg = "Could not parse " + feedName + ".json file content: " + gbfsFileValidation.fileContents();
                logger.error(msg, ex);
                throw new TiamatBusinessException(TiamatBusinessException.GBFS_INVALID, msg);
            }
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
