package org.rutebanken.tiamat.general;

import org.entur.gbfs.validation.model.FileValidationError;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.model.ValidationResult;
import org.entur.gbfs.validation.model.ValidationSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.SpecificParkingAreaUsageEnumeration;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportParams;
import org.rutebanken.tiamat.model.job.*;
import org.rutebanken.tiamat.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ImportJobWorkerGbfsParkingErrorsTest extends TiamatIntegrationTest {

    private static final String GLOBAL_URL = "http://mock-gbfs.test/gbfs.json";

    private static final String VALID_DISCOVERY_JSON = """
            {
              "last_updated": "2025-01-01T00:00:00Z",
              "ttl": 60,
              "version": "3.0",
              "data": {
                "feeds": [
                  { "name": "system_information", "url": "http://mock-gbfs.test/system_information.json" },
                  { "name": "station_information", "url": "http://mock-gbfs.test/station_information.json" },
                  { "name": "vehicle_types", "url": "http://mock-gbfs.test/vehicle_types.json" }
                ]
              }
            }
            """;

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
        byte[] invalidUtf8Bytes = {(byte) 0xFF, (byte) 0xFE, (byte) 0xFD};
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> new ByteArrayInputStream(invalidUtf8Bytes));

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.ENCODING);
    }

    @Test
    void populatesStructuredErrorsOnTemplateMismatch() throws IOException {
        byte[] notAGbfsJsonFile = "{}".getBytes(StandardCharsets.UTF_8);
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> new ByteArrayInputStream(notAGbfsJsonFile));
        when(gbfsValidator.validate(anyMap())).thenReturn(new ValidationResult(new ValidationSummary(null, 0L, 0), Map.of()));

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.TEMPLATE);
    }

    @Test
    void populatesStructuredErrorsOnMissingRequiredData() throws IOException {
        byte[] placeholderBytes = "{}".getBytes(StandardCharsets.UTF_8);
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> new ByteArrayInputStream(placeholderBytes));

        FileValidationResult discoveryFile = new FileValidationResult(
                "gbfs", true, true, 0, null, VALID_DISCOVERY_JSON, "3.0", List.of());
        ValidationResult discoveryValidation = new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of("gbfs", discoveryFile));

        FileValidationResult stationInformationWithMissingData = new FileValidationResult(
                "station_information", true, true, 1, null, null, "3.0",
                List.of(new FileValidationError("#/data/stations/0", "capacity", "capacity is required")));
        ValidationResult compositeValidation = new ValidationResult(
                new ValidationSummary("3.0", 0L, 1), Map.of("station_information", stationInformationWithMissingData));

        when(gbfsValidator.validate(anyMap())).thenReturn(discoveryValidation, compositeValidation);

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.MISSING_DATA);
    }

    private Job runGbfsParkingImport() {
        GbfsParkingImportParams params = new GbfsParkingImportParams();
        params.setGlobalUrl(GLOBAL_URL);
        params.setParkingType(ParkingTypeEnumeration.CYCLE_RENTAL);
        params.setParkingAreaType(SpecificParkingAreaUsageEnumeration.PEDAL_CYCLE);

        Job job = new Job();
        job.setFileName(GLOBAL_URL);
        job.setType(JobType.GBFS_PARKING);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);

        ImportJobWorker worker = importJobWorkerBuilder
                .init(job)
                .withGbfsParkingImportParams(params)
                .build();
        worker.run();

        return jobRepository.findById(job.getId()).orElseThrow();
    }
}
