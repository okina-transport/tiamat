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
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.ENCODING);
    }

    @Test
    void populatesStructuredErrorsOnMalformedXml() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_malformed.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.TEMPLATE);
    }

    @Test
    void populatesStructuredErrorsOnMissingRequiredData() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_missing_required_fields_valid_tof.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.MISSING_DATA);
    }

    @Test
    void populatesStructuredErrorsOnStructuralMismatch() throws IOException {
        Job job = runNetexParkingImport("parkings_v1.2_valid_parking_tof_invalid.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.TEMPLATE);
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
