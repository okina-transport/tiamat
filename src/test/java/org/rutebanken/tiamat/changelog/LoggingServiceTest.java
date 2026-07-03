package org.rutebanken.tiamat.changelog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.springframework.jms.core.JmsTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoggingSetestrviceTest {

    @Mock
    JmsTemplate jmsTemplate;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    LoggingService loggingService;

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
    void logPoiDeleteAll_sendsDeleteAllEntry() throws Exception {
        loggingService.logPoiDeleteAll("alice");

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

    // --- Accessibility ---

    @Test
    void logQuayAccessibilityUpdate_sendsMetadataWithFilename() throws Exception {
        loggingService.logQuayAccessibilityUpdate("alice", "quay_acc.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("QUAY-ACCESSIBILITY-UPDATE");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata.get("filename")).isEqualTo("quay_acc.csv");
    }

    @Test
    void logStopPlaceAccessibilityUpdate_sendsMetadataWithFilename() throws Exception {
        loggingService.logStopPlaceAccessibilityUpdate("alice", "sp_acc.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("STOP-PLACE-ACCESSIBILITY-UPDATE");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata.get("filename")).isEqualTo("sp_acc.csv");
    }

    // --- TAD ---

    @Test
    void logTadImport_sendsMetadataWithFilename() throws Exception {
        loggingService.logTadImport("alice", "tad.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("TAD-CSV-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata.get("filename")).isEqualTo("tad.csv");
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
        assertThat(metadata.get("filename")).isEqualTo("stops.xml");
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
        assertThat(metadata.get("filename")).isEqualTo("pois.xml");
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
        assertThat(metadata.get("filename")).isEqualTo("parkings.xml");
    }

    // --- GBFS ---

    @Test
    void logGbfsParkingImport_sendsMetadataWithUrl() throws Exception {
        loggingService.logGbfsParkingImport("alice", "https://gbfs.example.com/feed");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("GBFS-PARKING-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata.get("url")).isEqualTo("https://gbfs.example.com/feed");
    }

    // --- CSV imports ---

    @Test
    void logBikeParkingCsvImport_sendsMetadataWithFilename() throws Exception {
        loggingService.logBikeParkingCsvImport("alice", "bikes.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("BIKE-PARKING-CSV-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata.get("filename")).isEqualTo("bikes.csv");
    }

    @Test
    void logParkingCsvImport_sendsMetadataWithFilename() throws Exception {
        loggingService.logParkingCsvImport("alice", "parkings.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("PARKING-CSV-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata.get("filename")).isEqualTo("parkings.csv");
    }

    @Test
    void logRentalBikeCsvImport_sendsMetadataWithFilename() throws Exception {
        loggingService.logRentalBikeCsvImport("alice", "rental.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("RENTAL-BIKE-CSV-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata.get("filename")).isEqualTo("rental.csv");
    }

    @Test
    void logPoiCsvImport_sendsMetadataWithFilename() throws Exception {
        loggingService.logPoiCsvImport("alice", "pois.csv");

        LogEntryDto entry = captureLogEntry();
        assertThat(entry.getActionType()).isEqualTo("POI-CSV-IMPORT");
        assertThat(entry.getUser()).isEqualTo("alice");
        Map<String, Object> metadata = parseMetadata(entry);
        assertThat(metadata.get("filename")).isEqualTo("pois.csv");
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
}
