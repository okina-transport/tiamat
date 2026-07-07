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
class ImportJobWorkerNetexPoiErrorsTest extends TiamatIntegrationTest {

    private static final String RESOURCES_DIR = "src/test/resources/manualImports/poiNetex/errorClassification/";

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ImportJobWorkerBuilder importJobWorkerBuilder;

    @BeforeEach
    void bindRequestScope() {
        // ImportJobWorkerBuilder is @RequestScope (see class javadoc there); outside a real HTTP request
        // (as in this test) Spring has no request to bind it to, so we fake one for the current thread.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void unbindRequestScope() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void populatesStructuredErrorsOnEncodingFailure() throws IOException {
        Job job = runNetexPoiImport("poi_bad_encoding.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.ENCODING);
    }

    @Test
    void populatesStructuredErrorsOnMalformedXml() throws IOException {
        Job job = runNetexPoiImport("poi_malformed.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.TEMPLATE);
    }

    @Test
    void populatesStructuredErrorsOnMissingCentroid() throws IOException {
        Job job = runNetexPoiImport("poi_missing_centroid.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.MISSING_DATA);
    }

    @Test
    void populatesStructuredErrorsOnInvalidIdFormat() throws IOException {
        Job job = runNetexPoiImport("poi_invalid_id.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(AnalyzeImportErrorType.TEMPLATE);
    }

    private Job runNetexPoiImport(String fileName) throws IOException {
        Job job = new Job();
        job.setFileName(fileName);
        job.setType(JobType.NETEX_POI);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);

        try (InputStream inputStream = new FileInputStream(RESOURCES_DIR + fileName)) {
            ImportJobWorker worker = importJobWorkerBuilder
                    .init(job)
                    .withInputStream(inputStream)
                    .build();
            worker.run();
        }

        return jobRepository.findById(job.getId()).orElseThrow();
    }
}
