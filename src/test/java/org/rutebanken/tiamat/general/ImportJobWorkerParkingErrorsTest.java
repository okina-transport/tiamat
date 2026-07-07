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
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ImportJobWorkerParkingErrorsTest extends TiamatIntegrationTest {

    private static final String RESOURCES_DIR = "src/test/resources/manualImports/parkings/";

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ImportJobWorkerBuilder importJobWorkerBuilder;

    @BeforeEach
    void bindRequestScope() {
        // ImportJobWorkerBuilder is @RequestScope; outside a real HTTP request (as in this test) Spring has
        // no request to bind it to, so we fake one for the current thread.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void unbindRequestScope() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void populatesStructuredErrorsOnEncodingFailure() throws IOException {
        Job job = runParkingImport("parkings_bad_encoding.csv");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.ENCODING);
    }

    @Test
    void populatesStructuredErrorsOnTemplateMismatch() throws IOException {
        Job job = runParkingImport("parkings_bad_headers.csv");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.TEMPLATE);
    }

    @Test
    void populatesStructuredErrorsOnMissingRequiredData() throws IOException {
        Job job = runParkingImport("parkings_missing_data.csv");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.MISSING_DATA);
    }

    private Job runParkingImport(String fileName) throws IOException {
        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.CSV_PARKING);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);

        try (InputStream inputStream = new FileInputStream(RESOURCES_DIR + fileName)) {
            ImportJobWorker worker = importJobWorkerBuilder
                    .init(job)
                    .withInputStream(inputStream)
                    .withParkingLayoutParam("openSpace")
                    .withParkingTypeParam("parkingZone")
                    .withParkAndRideDetection(false)
                    .build();
            worker.run();
        }

        return jobRepository.findById(job.getId()).orElseThrow();
    }
}
