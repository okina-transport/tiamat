package org.rutebanken.tiamat.changelog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.StopPlace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class LoggingService {

    public static final String SERVICE_TYPE_TIAMAT = "TIAMAT";
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
        logParking("PARKING-CREATE", user, null, entity);
    }

    public void logParkingUpdate(String user, Parking from, Parking to) {
        logParking("PARKING-UPDATE", user, from, to);
    }

    public void logParkingDeletion(String user, Parking entity) {
        logParking("PARKING-DELETE", user, entity, null);
    }

    public void logParkingDeleteAll(String user) {
        try {
            log("PARKING-DELETE-ALL", user, null, DEFAULT_ORGANIZATION, null, null, null);
        } catch (Exception e) {
            log.error("Unable to log PARKING-DELETE-ALL", e);
        }
    }

    public void logPOICreation(String user, PointOfInterest entity) {
        logPOI("POI-CREATE", user, null, entity);
    }

    public void logPOIUpdate(String user, PointOfInterest from, PointOfInterest to) {
        logPOI("POI-UPDATE", user, from, to);
    }

    public void logPOIDeletion(String user, PointOfInterest entity) {
        logPOI("POI-DELETE", user, entity, null);
    }

    public void logPoiDeleteAll(String user) {
        try {
            log("POI-DELETE-ALL", user, null, DEFAULT_ORGANIZATION, null, null, null);
        } catch (Exception e) {
            log.error("Unable to log POI-DELETE-ALL", e);
        }
    }

    public void logStopPlaceCreation(String user, StopPlace entity) {
        logStopPlace("STOP-PLACE-CREATE", user, null, entity);
    }

    public void logStopPlaceUpdate(String user, StopPlace from, StopPlace to) {
        logStopPlace("STOP-PLACE-UPDATE", user, from, to);
    }

    public void logStopPlaceDeletion(String user, StopPlace entity) {
        logStopPlace("STOP-PLACE-DELETE", user, entity, null);
    }

    public void logStopPlaceDeleteByOrganisation(String user, String organisation) {
        try {
            log("STOP-PLACE-DELETE-BY-ORGANISATION", user, null, organisation, null, null, null);
        } catch (Exception e) {
            log.error("Unable to log STOP-PLACE-DELETE-BY-ORGANISATION for organisation {}", organisation, e);
        }
    }

    public void logGbfsParkingImport(String user, String url) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of("url", url));
            log("GBFS-PARKING-IMPORT", user, null, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log GBFS-PARKING-IMPORT for url {}", url, e);
        }
    }

    public void logBikeParkingCsvImport(String user, String fileName) {
        logFileImport("BIKE-PARKING-CSV-IMPORT", user, fileName);
    }

    public void logParkingCsvImport(String user, String fileName) {
        logFileImport("PARKING-CSV-IMPORT", user, fileName);
    }

    public void logRentalBikeCsvImport(String user, String fileName) {
        logFileImport("RENTAL-BIKE-CSV-IMPORT", user, fileName);
    }

    public void logPoiCsvImport(String user, String fileName) {
        logFileImport("POI-CSV-IMPORT", user, fileName);
    }

    public void logStopPlaceNetexImport(String user, String fileName) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of("filename", fileName));
            log("STOP-PLACE-NETEX-IMPORT", user, null, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log STOP-PLACE-NETEX-IMPORT for file {}", fileName, e);
        }
    }

    public void logTadImport(String user, String fileName) {
        logFileImport("TAD-CSV-IMPORT", user, fileName);
    }

    public void logStopPlaceAccessibilityUpdate(String user, String fileName) {
        logFileImport("STOP-PLACE-ACCESSIBILITY-UPDATE", user, fileName);
    }

    public void logQuayAccessibilityUpdate(String user, String fileName) {
        logFileImport("QUAY-ACCESSIBILITY-UPDATE", user, fileName);
    }

    public void logPoiNetexImport(String user, String fileName) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of("filename", fileName));
            log("POI-NETEX-IMPORT", user, null, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log POI-NETEX-IMPORT for file {}", fileName, e);
        }
    }

    public void logParkingNetexImport(String user, String fileName) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of("filename", fileName));
            log("PARKING-NETEX-IMPORT", user, null, DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log PARKING-NETEX-IMPORT for file {}", fileName, e);
        }
    }

    private void logParking(String actionType, String user, @Nullable Parking from, @Nullable Parking to) {
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

    private void logPOI(String actionType, String user, @Nullable PointOfInterest from, @Nullable PointOfInterest to) {
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

    private void logStopPlace(String actionType, String user, @Nullable StopPlace from, @Nullable StopPlace to) {
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
            log(actionType, user, netexId, provider, null, from != null ? objectMapper.writeValueAsString(from) : null, to != null ? objectMapper.writeValueAsString(to) : null);
        } catch (Exception e) {
            log.error("Unable to log {} for id {}", actionType, netexId, e);
        }
    }

    private void logFileImport(String actionType, String user, String fileName) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of("filename", fileName));
            log(actionType, user, null, LoggingService.DEFAULT_ORGANIZATION, metadata, null, null);
        } catch (Exception e) {
            log.error("Unable to log {} for file {}", actionType, fileName, e);
        }
    }

    private void log(String actionType, String user, String objectId, String organization, String metadata, String objectBefore, String objectAfter) throws JsonProcessingException {
        if (!loggingEnabled) {
            return;
        }
        LogEntryDto logEntryDto = new LogEntryDto();
        logEntryDto.setEventTimestamp(Instant.now());
        logEntryDto.setActionType(actionType);
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
