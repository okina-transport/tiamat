package org.rutebanken.tiamat.changelog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.rutebanken.tiamat.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.rutebanken.tiamat.changelog.LoggingActionType.*;

@Service
public class LoggingService {

    public static final String SERVICE_TYPE_TIAMAT = "TIAMAT";
    public static final String FILENAME_KEY = "filename";
    private static final String LOGGING_SERVICE_QUEUE = "logging.service";
    private static final String DEFAULT_ORGANIZATION = "technique";
    private static final Logger log = LogManager.getLogger(LoggingService.class);
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final boolean loggingEnabled;

    public LoggingService(JmsTemplate jmsTemplate, ObjectMapper objectMapper, @Value("${user.actions.logging.enabled:false}") boolean loggingEnabled) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.loggingEnabled = loggingEnabled;
    }

    public void logParkingCreation(String user, Parking entity) {
        logParking(PARKING_CREATE, user, null, entity);
    }

    public void logParkingUpdate(String user, Parking from, Parking to) {
        logParking(PARKING_UPDATE, user, from, to);
    }

    public void logParkingDeletion(String user, Parking entity) {
        logParking(PARKING_DELETE, user, entity, null);
    }

    public void logParkingDeleteAll(String user) {
        try {
            log(PARKING_DELETE_ALL, user, null, DEFAULT_ORGANIZATION, null, null, null);
        } catch (Exception e) {
            log.error("Unable to log {}", PARKING_DELETE_ALL, e);
        }
    }

    public void logPOICreation(String user, PointOfInterest entity) {
        logPOI(POI_CREATE, user, null, entity);
    }

    public void logPOIUpdate(String user, PointOfInterest from, PointOfInterest to) {
        logPOI(POI_UPDATE, user, from, to);
    }

    public void logPOIDeletion(String user, PointOfInterest entity) {
        logPOI(POI_DELETE, user, entity, null);
    }

    public void logPOIDeleteAll(String user) {
        try {
            log(POI_DELETE_ALL, user, null, DEFAULT_ORGANIZATION, null, null, null);
        } catch (Exception e) {
            log.error("Unable to log {}", POI_DELETE_ALL, e);
        }
    }

    public void logStopPlaceCreation(String user, StopPlace entity) {
        logStopPlace(STOP_PLACE_CREATE, user, null, null, entity);
    }

    public void logStopPlaceUpdate(String user, StopPlace from, StopPlace to) {
        logStopPlace(STOP_PLACE_UPDATE, user, null, from, to);
    }

    public void logStopPlaceDeletion(String user, StopPlace entity) {
        logStopPlace(STOP_PLACE_DELETE, user, null, entity, null);
    }

    public void logStopPlaceMerge(String user, String fromStopPlaceId, String toStopPlaceId) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of("fromStopPlaceId", fromStopPlaceId, "toStopPlaceId", toStopPlaceId));
            log(STOP_PLACE_MERGE, user, null, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log {} for fromStopPlaceId {} and toStopPlaceId {}", STOP_PLACE_MERGE, fromStopPlaceId, toStopPlaceId, e);
        }
    }

    public void logStopPlaceQuayMove(String user, List<String> quayIds, String destinationStopPlaceId, String fromVersionComment, String toVersionComment) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of("quayIds", quayIds, "fromVersionComment", fromVersionComment, "toVersionComment", toVersionComment));
            log(STOP_PLACE_QUAY_MOVE, user, destinationStopPlaceId, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log {} for quayIds {} and destinationStopPlaceId {}", STOP_PLACE_QUAY_MOVE, quayIds, destinationStopPlaceId, e);
        }
    }

    public void logStopPlaceRename(String user) {
        try {
            log(STOP_PLACE_RENAME, user, null, DEFAULT_ORGANIZATION, null, null, null);
        } catch (Exception e) {
            log.error("Unable to log {}", STOP_PLACE_RENAME, e);
        }
    }

    public void logStopPlaceReopen(String user, String stopPlaceId, String versionComment) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of("versionComment", versionComment));
            log(STOP_PLACE_REOPEN, user, stopPlaceId, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log {} for stopPlaceId {} and versionComment {}", STOP_PLACE_REOPEN, stopPlaceId, versionComment, e);
        }
    }

    public void logStopPlaceTermination(String user, String stopPlaceNetexId, Instant suggestedTimeOfTermination, String versionComment, ModificationEnumeration modificationEnumeration) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of("suggestedTimeOfTermination", suggestedTimeOfTermination.toEpochMilli(), "versionComment", versionComment, "modificationEnumeration", modificationEnumeration.value()));
            log(STOP_PLACE_TERMINATION, user, stopPlaceNetexId, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log {} for stopPlaceNetexId {} and suggestedTimeOfTermination {}", STOP_PLACE_TERMINATION, stopPlaceNetexId, suggestedTimeOfTermination, e);
        }
    }

    public void logStopPlaceQuayDeletion(String user, Quay entity) {
        logQuay(STOP_PLACE_QUAY_DELETE, user, null, entity, null);
    }

    public void logMultiModalSPCreation(String user, StopPlace entity) {
        logStopPlace(MULTI_MODAL_STOP_PLACE_CREATE, user, null, null, entity);
    }

    public void logMultiModalSPUpdate(String user, StopPlace from, StopPlace to, String metadata) {
        logStopPlace(MULTI_MODAL_STOP_PLACE_UPDATE, user, metadata, from, to);
    }

    public void logMultiModalSPDeletion(String user, StopPlace entity) {
        logStopPlace(MULTI_MODAL_STOP_PLACE_DELETE, user, null, entity, null);
    }

    public void logStopPlaceDeleteByOrganisation(String user, String organisation) {
        try {
            log(STOP_PLACE_DELETE_BY_ORGANISATION, user, null, organisation, null, null, null);
        } catch (Exception e) {
            log.error("Unable to log {} for organisation {}", STOP_PLACE_DELETE_BY_ORGANISATION, organisation, e);
        }
    }

    public void logGbfsParkingImport(String user, String url) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of("url", url));
            log(GBFS_PARKING_IMPORT, user, null, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log {} for url {}", GBFS_PARKING_IMPORT, url, e);
        }
    }

    public void logBikeParkingCsvImport(String user, String fileName) {
        logFileImport(BIKE_PARKING_CSV_IMPORT, user, fileName);
    }

    public void logParkingCsvImport(String user, String fileName) {
        logFileImport(PARKING_CSV_IMPORT, user, fileName);
    }

    public void logRentalBikeCsvImport(String user, String fileName) {
        logFileImport(RENTAL_BIKE_CSV_IMPORT, user, fileName);
    }

    public void logPoiCsvImport(String user, String fileName) {
        logFileImport(POI_CSV_IMPORT, user, fileName);
    }

    public void logStopPlaceNetexImport(String user, String fileName) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of(FILENAME_KEY, fileName));
            log(STOP_PLACE_NETEX_IMPORT, user, null, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log {} for file {}", STOP_PLACE_NETEX_IMPORT, fileName, e);
        }
    }

    public void logTadImport(String user, String fileName) {
        logFileImport(TAD_CSV_IMPORT, user, fileName);
    }

    public void logStopPlaceAccessibilityUpdate(String user, String fileName) {
        logFileImport(STOP_PLACE_ACCESSIBILITY_UPDATE, user, fileName);
    }

    public void logQuayAccessibilityUpdate(String user, String fileName) {
        logFileImport(QUAY_ACCESSIBILITY_UPDATE, user, fileName);
    }

    public void logPoiNetexImport(String user, String fileName) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of(FILENAME_KEY, fileName));
            log(POI_NETEX_IMPORT, user, null, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log {} for file {}", POI_NETEX_IMPORT, fileName, e);
        }
    }

    public void logParkingNetexImport(String user, String fileName) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of(FILENAME_KEY, fileName));
            log(PARKING_NETEX_IMPORT, user, null, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log {} for file {}", PARKING_NETEX_IMPORT, fileName, e);
        }
    }

    private void logParking(LoggingActionType actionType, String user, @Nullable Parking from, @Nullable Parking to) {
        String netexId = null;
        String operator = null;
        if (from != null) {
            netexId = from.getNetexId();
            operator = from.getOperator();
        }
        if (to != null) {
            netexId = to.getNetexId();
            operator = to.getOperator();
        }
        try {
            log(actionType, user, netexId, operator, null, from != null ? objectMapper.writeValueAsString(from) : null, to != null ? objectMapper.writeValueAsString(to) : null);
        } catch (Exception e) {
            log.error("Unable to log {} for id {}", actionType, netexId, e);
        }
    }

    private void logPOI(LoggingActionType actionType, String user, @Nullable PointOfInterest from, @Nullable PointOfInterest to) {
        String netexId = null;
        String operator = null;
        if (from != null) {
            netexId = from.getNetexId();
            operator = from.getOperator();
        }
        if (to != null) {
            netexId = to.getNetexId();
            operator = to.getOperator();
        }
        try {
            log(actionType, user, netexId, operator, null, from != null ? objectMapper.writeValueAsString(from) : null, to != null ? objectMapper.writeValueAsString(to) : null);
        } catch (Exception e) {
            log.error("Unable to log {} for id {}", actionType, netexId, e);
        }
    }

    private void logStopPlace(LoggingActionType actionType, String user, @Nullable String metadata, @Nullable StopPlace from,
                              @Nullable StopPlace to) {
        String netexId = null;
        String provider = null;
        if (from != null) {
            netexId = from.getNetexId();
            provider = from.getProvider();
        }
        if (to != null) {
            netexId = to.getNetexId();
            provider = to.getProvider();
        }
        try {
            log(actionType, user, netexId, provider, metadata, from != null ? objectMapper.writeValueAsString(from) : null, to != null ? objectMapper.writeValueAsString(to) : null);
        } catch (Exception e) {
            log.error("Unable to log {} for id {}", actionType, netexId, e);
        }
    }

    private void logQuay(LoggingActionType actionType, String user, String provider, @Nullable Quay from, @Nullable Quay to) {
        String netexId = null;
        if (from != null) {
            netexId = from.getNetexId();
        }
        if (to != null) {
            netexId = to.getNetexId();
        }
        try {
            log(actionType, user, netexId, provider, null, from != null ? objectMapper.writeValueAsString(from) : null, to != null ? objectMapper.writeValueAsString(to) : null);
        } catch (Exception e) {
            log.error("Unable to log {} for id {}", actionType, netexId, e);
        }
    }

    private void logFileImport(LoggingActionType actionType, String user, String fileName) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of(FILENAME_KEY, fileName));
            log(actionType, user, null, LoggingService.DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log {} for file {}", actionType, fileName, e);
        }
    }

    private void log(LoggingActionType actionType, String user, String objectId, String organization, String metadata, String objectBefore, String objectAfter) throws JsonProcessingException {
        if (!loggingEnabled) {
            return;
        }
        LogEntryDto logEntryDto = new LogEntryDto();
        logEntryDto.setEventTimestamp(Instant.now());
        logEntryDto.setActionType(actionType.getValue());
        logEntryDto.setUser(user);
        logEntryDto.setObjectId(objectId);
        logEntryDto.setOrganization(organization);
        logEntryDto.setService(SERVICE_TYPE_TIAMAT);
        if (StringUtils.isNotBlank(metadata) || StringUtils.isNotBlank(objectBefore) || StringUtils.isNotBlank(objectAfter)) {
            LogContentDto logContentDto = new LogContentDto();
            logContentDto.setMetadata(metadata);
            logContentDto.setObjectBefore(objectBefore);
            logContentDto.setObjectAfter(objectAfter);
            logEntryDto.setLogContent(logContentDto);
        }
        jmsTemplate.convertAndSend(LOGGING_SERVICE_QUEUE, objectMapper.writeValueAsString(logEntryDto));
    }

}
