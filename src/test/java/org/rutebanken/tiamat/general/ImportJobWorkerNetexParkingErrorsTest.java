package org.rutebanken.tiamat.general;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.job.*;
import org.rutebanken.tiamat.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ImportJobWorkerNetexParkingErrorsTest extends TiamatIntegrationTest {

    private static final String RESOURCES_DIR = "src/test/resources/manualImports/parkingsNetex/errorClassification/";

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ImportJobWorkerBuilder importJobWorkerBuilder;

    @BeforeEach
    void bindRequestScope() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void unbindRequestScope() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void populatesStructuredErrorsOnEncodingFailure() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_bad_encoding.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).extracting(JobError::getCode).containsExactly(AnalyzeImportErrorType.ENCODING);
    }

    @Test
    void populatesStructuredErrorsOnMalformedXml() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_malformed.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).extracting(JobError::getCode).containsExactly(AnalyzeImportErrorType.TEMPLATE);
        assertThat(job.getErrors().getFirst().getParams()).containsKeys("line", "column", "detail");
    }

    @Test
    void detailsInvalidParkingIdFormat() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_invalid_parking_id.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).contains(JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_ID)
                .withParam("field", "id")
                .withParam("entity", "Parking")
                .withParam("entityId", "FR__40088___Parking__327__LOC")
                .withParam("line", "14")
                .withParam("errorValue", "FR__40088___Parking__327__LOC"));
    }

    @Test
    void rejectsWholeFileOnDuplicatedIds() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_duplicated_ids.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                duplicatedId("FR:40088:Parking:327:LOC", "93"),
                duplicatedId("MOBIITI:ParkingArea:260:LOC", "146"));
        // the other parkings of the file, which are valid, must not have been imported
        assertThat(parkingRepository.count()).isZero();
    }

    private static JobError duplicatedId(String id, String line) {
        return JobError.error(AnalyzeImportErrorType.DUPLICATE_ID_FILE)
                .withParam("fileName", "parkings_v1.2_duplicated_ids.xml")
                .withParam("field", "id")
                .withParam("errorValue", id)
                .withParam("line", line);
    }

    @Test
    void rejectsWholeFileOnValuesTooLong() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_values_too_long.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactlyInAnyOrder(
                tooLong("Name", "Parking", "FR:40088:Parking:327:LOC", "14", 300, 255),
                tooLong("Name", "ParkingArea", "MOBIITI:ParkingArea:260:LOC", "67", 260, 255),
                tooLong("PostalAddress/Street", "Parking", "FR:40088:Parking:328:LOC", "93", 256, 255));
        // the other parkings of the file, which are valid, must not have been imported
        assertThat(parkingRepository.count()).isZero();
    }

    private static JobError tooLong(String field, String entity, String entityId, String line, int length, int maxLength) {
        return JobError.error(AnalyzeImportErrorType.FIELD_TOO_LONG)
                .withParam("field", field)
                .withParam("entity", entity)
                .withParam("entityId", entityId)
                .withParam("line", line)
                .withParam("length", String.valueOf(length))
                .withParam("maxLength", String.valueOf(maxLength));
    }

    @Test
    void detailsMissingParkingField() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_missing_parking_layout.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.MISSING_DATA)
                .withParam("field", "ParkingLayout")
                .withParam("entity", "Parking")
                .withParam("entityId", "FR:40088:Parking:327:LOC")
                .withParam("line", "14"));
    }

    @Test
    void populatesStructuredErrorsOnFileWithoutParking() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_no_parking.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.NO_DATA_ROWS));
    }

    @Test
    void importsParkingOfGeneralFrameInCompositeFrame() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_composite_frame.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FINISHED);
        assertThat(job.getErrors()).isNullOrEmpty();
        assertThat(parkingRepository.count()).isEqualTo(1);
    }

    @Test
    void validatesParkingOfGeneralFrameInCompositeFrame() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_composite_frame_missing_total_capacity.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                missingField("TotalCapacity", "Parking", "FR:40088:Parking:327:LOC", "17"));
        assertThat(parkingRepository.count()).isZero();
    }

    @Test
    void rejectsParkingsOfSiteFrame() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_site_frame.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.TEMPLATE)
                .withParam("field", "SiteFrame")
                .withParam("errorValue", "FR:SiteFrame:1")
                .withParam("line", "7"));
        assertThat(parkingRepository.count()).isZero();
    }

    @Test
    void failsWhenNoParkingIsImported() throws IOException {
        // the parking is valid but only the first general frame of the file is imported
        Job job = runNetexParkingImport("parkings_v1.2_parking_in_second_general_frame.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.NO_DATA_IMPORTED));
        assertThat(parkingRepository.count()).isZero();
    }

    @Test
    void acceptsPublicationTimestampWithMicroseconds() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_timestamp_microseconds.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FINISHED);
        assertThat(parkingRepository.count()).isEqualTo(1);
    }

    @Test
    void populatesStructuredErrorsOnFileWithoutDataObjects() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_no_data_objects.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.NO_DATA_ROWS));
    }

    @Test
    void reportsLineAndDetailOnMismatchedXmlTag() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_tag_mismatch.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).hasSize(1);
        JobError error = job.getErrors().getFirst();
        assertThat(error.getCode()).isEqualTo(AnalyzeImportErrorType.TEMPLATE);
        assertThat(error.getParams())
                .containsEntry("line", "65")
                .containsKey("column");
        assertThat(error.getParams().get("detail")).contains("ParkingProperties");
    }

    @Test
    void populatesStructuredErrorsOnMissingRequiredData() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_missing_required_fields_valid_tof.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                missingField("Name", "Parking", "FR:75056:Parking:1:LOC", "15"),
                missingField("Centroid/Location/Longitude", "Parking", "FR:75056:Parking:1:LOC", "15"),
                missingField("Centroid/Location/Latitude", "Parking", "FR:75056:Parking:1:LOC", "15"),
                missingField("ParkingType", "Parking", "FR:75056:Parking:1:LOC", "15"),
                missingField("ParkingVehicleTypes", "Parking", "FR:75056:Parking:1:LOC", "15"),
                missingField("ParkingLayout", "Parking", "FR:75056:Parking:1:LOC", "15"),
                missingField("TotalCapacity", "Parking", "FR:75056:Parking:1:LOC", "15"),
                missingField("ParkingPaymentProcess", "Parking", "FR:75056:Parking:1:LOC", "15"),
                missingField("PostalAddress/PostalRegion", "Parking", "FR:75056:Parking:1:LOC", "15"),
                missingField("ParkingVehicleTypes", "ParkingProperties", "MOBIITI:ParkingProperties:1", "38"),
                missingField("MaximumHeight", "ParkingArea", "MOBIITI:ParkingArea:1:LOC", "49"));
    }

    private static JobError missingField(String field, String entity, String entityId, String line) {
        return JobError.error(AnalyzeImportErrorType.MISSING_DATA)
                .withParam("field", field)
                .withParam("entity", entity)
                .withParam("entityId", entityId)
                .withParam("line", line);
    }

    @Test
    void populatesStructuredErrorsOnStructuralMismatch() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_valid_parking_tof_invalid.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).extracting(JobError::getCode).containsExactly(AnalyzeImportErrorType.TEMPLATE);
    }

    private Job runNetexParkingImport(String fileName) throws IOException {
        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.NETEX_PARKING);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);

        ExecutorService importService = Executors.newSingleThreadExecutor();
        try (InputStream inputStream = new FileInputStream(RESOURCES_DIR + fileName)) {
            ImportJobWorker worker = importJobWorkerBuilder
                    .init(job)
                    .withInputStream(inputStream)
                    .build();
            importService.submit(worker);
        } finally {
            importService.shutdown();
        }

        await().atMost(Duration.ofSeconds(10))
                .until(() -> jobRepository.findById(job.getId())
                        .map(j -> j.getStatus() != JobStatus.PROCESSING)
                        .orElse(false));

        return jobRepository.findById(job.getId()).orElseThrow();
    }
}
