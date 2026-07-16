package org.rutebanken.tiamat.changelog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rutebanken.tiamat.model.*;
import org.springframework.jms.core.JmsTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoggingServiceTest {

    @Mock
    JmsTemplate jmsTemplate;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    LoggingService loggingService;

    @BeforeEach
    void setUp() {
        loggingService = new LoggingService(jmsTemplate, objectMapper, true);
    }

    private LogEntryDto captureLogEntry() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jmsTemplate).convertAndSend(eq("logging.service"), captor.capture());
        return objectMapper.readValue(captor.getValue(), LogEntryDto.class);
    }

    private Map<String, Object> parseMetadata(LogEntryDto entry) throws Exception {
        return objectMapper.readValue(entry.getLogContent().getMetadata(), new TypeReference<>() {
        });
    }

    // --- Parking ---

    @Test
    void logParkingCreation_sendsCreateEntry() throws Exception {
        Parking parking = buildParking("MOBIITI:Parking:1", "OP1");

        loggingService.logParkingCreation("alice", parking);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("PARKING-CREATE");
        assertThat(entry.getUser()).isEqualTo("alice");
        assertThat(entry.getObjectId()).isEqualTo("MOBIITI:Parking:1");
        assertThat(entry.getOrganization()).isEqualTo("OP1");
        assertThat(entry.getService()).isEqualTo("TIAMAT");
        assertThat(entry.getLogContent().getObjectBefore()).isNull();
        assertThat(entry.getLogContent().getObjectAfter()).isNotBlank();
    }

    @Test
    void logParkingUpdate_sendsBeforeAndAfter() throws Exception {
        Parking from = buildParking("MOBIITI:Parking:1", "OP1");
        Parking to = buildParking("MOBIITI:Parking:1", "OP1");

        loggingService.logParkingUpdate("alice", from, to);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("PARKING-UPDATE");
        assertThat(entry.getLogContent().getObjectBefore()).isNotBlank();
        assertThat(entry.getLogContent().getObjectAfter()).isNotBlank();
    }

    @Test
    void logParkingDeletion_sendsDeleteEntry() throws Exception {
        Parking parking = buildParking("MOBIITI:Parking:1", "OP1");

        loggingService.logParkingDeletion("alice", parking);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("PARKING-DELETE");
        assertThat(entry.getLogContent().getObjectBefore()).isNotBlank();
        assertThat(entry.getLogContent().getObjectAfter()).isNull();
    }

    @Test
    void logParkingDeleteAll_sendsDeleteAllEntry() throws Exception {
        loggingService.logParkingDeleteAll("alice");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("PARKING-DELETE-ALL");
        assertThat(entry.getUser()).isEqualTo("alice");
        assertThat(entry.getObjectId()).isNull();
        assertThat(entry.getOrganization()).isEqualTo("technique");
        assertThat(entry.getLogContent()).isNull();
    }

    // --- POI ---

    @Test
    void logPOICreation_sendsCreateEntry() throws Exception {
        PointOfInterest poi = buildPOI("MOBIITI:PointOfInterest:1", "OP1");

        loggingService.logPOICreation("alice", poi);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("POI-CREATE");
        assertThat(entry.getUser()).isEqualTo("alice");
        assertThat(entry.getObjectId()).isEqualTo("MOBIITI:PointOfInterest:1");
        assertThat(entry.getOrganization()).isEqualTo("OP1");
        assertThat(entry.getLogContent().getObjectBefore()).isNull();
        assertThat(entry.getLogContent().getObjectAfter()).isNotBlank();
    }

    @Test
    void logPOIUpdate_sendsBeforeAndAfter() throws Exception {
        PointOfInterest from = buildPOI("MOBIITI:PointOfInterest:1", "OP1");
        PointOfInterest to = buildPOI("MOBIITI:PointOfInterest:1", "OP1");

        loggingService.logPOIUpdate("alice", from, to);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("POI-UPDATE");
        assertThat(entry.getLogContent().getObjectBefore()).isNotBlank();
        assertThat(entry.getLogContent().getObjectAfter()).isNotBlank();
    }

    @Test
    void logPOIDeletion_sendsDeleteEntry() throws Exception {
        PointOfInterest poi = buildPOI("MOBIITI:PointOfInterest:1", "OP1");

        loggingService.logPOIDeletion("alice", poi);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("POI-DELETE");
        assertThat(entry.getLogContent().getObjectBefore()).isNotBlank();
        assertThat(entry.getLogContent().getObjectAfter()).isNull();
    }

    @Test
    void logPOIDeleteAll_sendsDeleteAllEntry() throws Exception {
        loggingService.logPOIDeleteAll("alice");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("POI-DELETE-ALL");
        assertThat(entry.getUser()).isEqualTo("alice");
        assertThat(entry.getObjectId()).isNull();
        assertThat(entry.getOrganization()).isEqualTo("technique");
        assertThat(entry.getLogContent()).isNull();
    }

    // --- StopPlace ---

    @Test
    void logStopPlaceDeleteByOrganisation_sendsDeleteEntry() throws Exception {
        loggingService.logStopPlaceDeleteByOrganisation("alice", "RUT");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-DELETE-BY-ORGANISATION");
        assertThat(entry.getUser()).isEqualTo("alice");
        assertThat(entry.getObjectId()).isNull();
        assertThat(entry.getOrganization()).isEqualTo("RUT");
        assertThat(entry.getLogContent()).isNull();
    }

    @Test
    void logStopPlaceCreation_sendsCreateEntry() throws Exception {
        StopPlace stopPlace = buildStopPlace("MOBIITI:StopPlace:1", "RUT");

        loggingService.logStopPlaceCreation("alice", stopPlace);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-CREATE");
        assertThat(entry.getObjectId()).isEqualTo("MOBIITI:StopPlace:1");
        assertThat(entry.getOrganization()).isEqualTo("RUT");
        assertThat(entry.getLogContent().getObjectBefore()).isNull();
        assertThat(entry.getLogContent().getObjectAfter()).isNotBlank();
    }

    @Test
    void logStopPlaceUpdate_sendsBeforeAndAfter() throws Exception {
        StopPlace from = buildStopPlace("MOBIITI:StopPlace:1", "RUT");
        StopPlace to = buildStopPlace("MOBIITI:StopPlace:1", "RUT");

        loggingService.logStopPlaceUpdate("alice", from, to);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-UPDATE");
        assertThat(entry.getLogContent().getObjectBefore()).isNotBlank();
        assertThat(entry.getLogContent().getObjectAfter()).isNotBlank();
    }

    @Test
    void logStopPlaceDeletion_sendsDeleteEntry() throws Exception {
        StopPlace stopPlace = buildStopPlace("MOBIITI:StopPlace:1", "RUT");

        loggingService.logStopPlaceDeletion("alice", stopPlace);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-DELETE");
        assertThat(entry.getLogContent().getObjectBefore()).isNotBlank();
        assertThat(entry.getLogContent().getObjectAfter()).isNull();
    }

    @Test
    void logStopPlaceMerge_sendsMetadataWithFromAndToIds() throws Exception {
        loggingService.logStopPlaceMerge("alice", "MOBIITI:StopPlace:1", "MOBIITI:StopPlace:2");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-MERGE");
        assertThat(entry.getOrganization()).isEqualTo("technique");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsAllEntriesOf(Map.of("fromStopPlaceId", "MOBIITI:StopPlace:1", "toStopPlaceId", "MOBIITI:StopPlace:2"));
    }

    @Test
    void logStopPlaceQuayMove_sendsMetadataWithQuayIdsAndComments() throws Exception {
        loggingService.logStopPlaceQuayMove("alice", List.of("MOBIITI:Quay:1", "MOBIITI:Quay:2"), "MOBIITI:StopPlace:2", "from comment", "to comment");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-QUAY-MOVE");
        assertThat(entry.getObjectId()).isEqualTo("MOBIITI:StopPlace:2");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsAllEntriesOf(Map.of("quayIds", List.of("MOBIITI:Quay:1", "MOBIITI:Quay:2"), "fromVersionComment", "from comment", "toVersionComment", "to" + " comment"));
    }

    @Test
    void logStopPlaceRename_sendsRenameEntry() throws Exception {
        loggingService.logStopPlaceRename("alice");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-RENAME");
        assertThat(entry.getUser()).isEqualTo("alice");
        assertThat(entry.getLogContent()).isNull();
    }

    @Test
    void logStopPlaceReopen_sendsMetadataWithVersionComment() throws Exception {
        loggingService.logStopPlaceReopen("alice", "MOBIITI:StopPlace:1", "reopened comment");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-REOPEN");
        assertThat(entry.getObjectId()).isEqualTo("MOBIITI:StopPlace:1");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("versionComment", "reopened comment");
    }

    @Test
    void logStopPlaceTermination_sendsMetadataWithTerminationDetails() throws Exception {
        Instant suggestedTimeOfTermination = Instant.parse("2026-01-01T00:00:00Z");

        loggingService.logStopPlaceTermination("alice", "MOBIITI:StopPlace:1", suggestedTimeOfTermination, "terminated comment", ModificationEnumeration.DELETE);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-TERMINATION");
        assertThat(entry.getObjectId()).isEqualTo("MOBIITI:StopPlace:1");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsAllEntriesOf(Map.of("suggestedTimeOfTermination", suggestedTimeOfTermination.toEpochMilli(), "versionComment", "terminated comment", "modificationEnumeration", "delete"));
    }

    // --- Quay ---

    @Test
    void logStopPlaceQuayDeletion_sendsDeleteEntry() throws Exception {
        Quay quay = new Quay();
        quay.setNetexId("MOBIITI:Quay:1");

        loggingService.logStopPlaceQuayDeletion("alice", quay);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-QUAY-DELETE");
        assertThat(entry.getObjectId()).isEqualTo("MOBIITI:Quay:1");
        assertThat(entry.getLogContent().getObjectBefore()).isNotBlank();
        assertThat(entry.getLogContent().getObjectAfter()).isNull();
    }

    // --- Multi-modal StopPlace ---

    @Test
    void logMultiModalSPCreation_sendsCreateEntry() throws Exception {
        StopPlace stopPlace = buildStopPlace("MOBIITI:StopPlace:1", "RUT");

        loggingService.logMultiModalSPCreation("alice", stopPlace);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("MULTI-MODAL-STOP-PLACE-CREATE");
        assertThat(entry.getObjectId()).isEqualTo("MOBIITI:StopPlace:1");
        assertThat(entry.getLogContent().getObjectBefore()).isNull();
        assertThat(entry.getLogContent().getObjectAfter()).isNotBlank();
    }

    @Test
    void logMultiModalSPUpdate_sendsBeforeAndAfterWithMetadata() throws Exception {
        StopPlace from = buildStopPlace("MOBIITI:StopPlace:1", "RUT");
        StopPlace to = buildStopPlace("MOBIITI:StopPlace:1", "RUT");

        loggingService.logMultiModalSPUpdate("alice", from, to, "{\"reason\":\"merge\"}");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("MULTI-MODAL-STOP-PLACE-UPDATE");
        assertThat(entry.getLogContent().getMetadata()).isEqualTo("{\"reason\":\"merge\"}");
        assertThat(entry.getLogContent().getObjectBefore()).isNotBlank();
        assertThat(entry.getLogContent().getObjectAfter()).isNotBlank();
    }

    @Test
    void logMultiModalSPDeletion_sendsDeleteEntry() throws Exception {
        StopPlace stopPlace = buildStopPlace("MOBIITI:StopPlace:1", "RUT");

        loggingService.logMultiModalSPDeletion("alice", stopPlace);

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("MULTI-MODAL-STOP-PLACE-DELETE");
        assertThat(entry.getLogContent().getObjectBefore()).isNotBlank();
        assertThat(entry.getLogContent().getObjectAfter()).isNull();
    }

    // --- Accessibility ---

    @Test
    void logQuayAccessibilityUpdate_sendsMetadataWithFilename() throws Exception {
        loggingService.logQuayAccessibilityUpdate("alice", "quay_acc.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("QUAY-ACCESSIBILITY-UPDATE");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("filename", "quay_acc.csv");
    }

    @Test
    void logStopPlaceAccessibilityUpdate_sendsMetadataWithFilename() throws Exception {
        loggingService.logStopPlaceAccessibilityUpdate("alice", "sp_acc.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-ACCESSIBILITY-UPDATE");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("filename", "sp_acc.csv");
    }

    // --- TAD ---

    @Test
    void logTadImport_sendsMetadataWithFilename() throws Exception {
        loggingService.logTadImport("alice", "tad.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("TAD-CSV-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("filename", "tad.csv");
    }

    // --- NeTEx stops ---

    @Test
    void logStopPlaceNetexImport_setsProviderAsOrganizationAndFilenameInMetadata() throws Exception {
        loggingService.logStopPlaceNetexImport("alice", "stops.xml");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-NETEX-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        assertThat(entry.getOrganization()).isEqualTo("technique");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("filename", "stops.xml");
    }

    // --- NeTEx POI ---

    @Test
    void logPoiNetexImport_setsProviderAsOrganizationAndFilenameInMetadata() throws Exception {
        loggingService.logPoiNetexImport("alice", "pois.xml");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("POI-NETEX-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        assertThat(entry.getOrganization()).isEqualTo("technique");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("filename", "pois.xml");
    }

    // --- NeTEx Parking ---

    @Test
    void logParkingNetexImport_setsProviderAsOrganizationAndFilenameInMetadata() throws Exception {
        loggingService.logParkingNetexImport("alice", "parkings.xml");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("PARKING-NETEX-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        assertThat(entry.getOrganization()).isEqualTo("technique");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("filename", "parkings.xml");
    }

    // --- GBFS ---

    @Test
    void logGbfsParkingImport_sendsMetadataWithUrl() throws Exception {
        loggingService.logGbfsParkingImport("alice", "https://gbfs.example.com/feed");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("GBFS-PARKING-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("url", "https://gbfs.example.com/feed");
    }

    // --- CSV imports ---

    @Test
    void logBikeParkingCsvImport_sendsMetadataWithFilename() throws Exception {
        loggingService.logBikeParkingCsvImport("alice", "bikes.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("BIKE-PARKING-CSV-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("filename", "bikes.csv");
    }

    @Test
    void logParkingCsvImport_sendsMetadataWithFilename() throws Exception {
        loggingService.logParkingCsvImport("alice", "parkings.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("PARKING-CSV-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("filename", "parkings.csv");
    }

    @Test
    void logRentalBikeCsvImport_sendsMetadataWithFilename() throws Exception {
        loggingService.logRentalBikeCsvImport("alice", "rental.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("RENTAL-BIKE-CSV-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("filename", "rental.csv");
    }

    @Test
    void logPoiCsvImport_sendsMetadataWithFilename() throws Exception {
        loggingService.logPoiCsvImport("alice", "pois.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("POI-CSV-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata).containsEntry("filename", "pois.csv");
    }

    // --- Helpers ---

    private Parking buildParking(String netexId, String operator) {
        Parking parking = new Parking();
        parking.setNetexId(netexId);
        parking.setOperator(operator);
        return parking;
    }

    private PointOfInterest buildPOI(String netexId, String operator) {
        PointOfInterest poi = new PointOfInterest();
        poi.setNetexId(netexId);
        poi.setOperator(operator);
        return poi;
    }

    private StopPlace buildStopPlace(String netexId, String provider) {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setNetexId(netexId);
        stopPlace.setProvider(provider);
        return stopPlace;
    }
}
