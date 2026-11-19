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
        assertThat(job.getErrors()).extracting(JobError::getCode).containsExactly(AnalyzeImportErrorType.ENCODING);
    }

    @Test
    void populatesStructuredErrorsOnMalformedXml() throws IOException {
        Job job = runNetexPoiImport("poi_malformed.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).extracting(JobError::getCode).containsExactly(AnalyzeImportErrorType.TEMPLATE);
    }

    @Test
    void populatesStructuredErrorsOnMissingCentroid() throws IOException {
        Job job = runNetexPoiImport("poi_missing_centroid.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                missingPoiField("Centroid/Location/Longitude", "15"),
                missingPoiField("Centroid/Location/Latitude", "15"));
    }

    private static JobError missingPoiField(String field, String line) {
        return JobError.error(AnalyzeImportErrorType.MISSING_DATA)
                .withParam("field", field)
                .withParam("entity", "PointOfInterest")
                .withParam("entityId", "MOBIITI:PointOfInterest:70")
                .withParam("line", line);
    }

    @Test
    void populatesStructuredErrorsOnFileWithoutPointOfInterest() throws IOException {
        Job job = runNetexPoiImport("poi_no_poi.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.NO_DATA_ROWS));
    }

    @Test
    void rejectsWholeFileOnDuplicatedIds() throws IOException {
        Job job = runNetexPoiImport("poi_duplicated_ids.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                duplicatedId("poi_duplicated_ids.xml", "MOBIITI:PointOfInterest:70", "61"),
                duplicatedId("poi_duplicated_ids.xml", "MOBIITI:AccessibilityAssessment:8054", "79"),
                duplicatedId("poi_duplicated_ids.xml", "14850", "82"));
        assertThat(poiRepository.count()).isZero();
    }

    private static JobError duplicatedId(String fileName, String id, String line) {
        return JobError.error(AnalyzeImportErrorType.DUPLICATE_ID_FILE)
                .withParam("fileName", fileName)
                .withParam("field", "id")
                .withParam("errorValue", id)
                .withParam("line", line);
    }

    @Test
    void rejectsWholeFileOnValuesTooLong() throws IOException {
        Job job = runNetexPoiImport("poi_values_too_long.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactlyInAnyOrder(
                tooLong("Name", 300),
                tooLong("KeyValues/Items", 260));
        assertThat(poiRepository.count()).isZero();
    }

    private static JobError tooLong(String field, int length) {
        return JobError.error(AnalyzeImportErrorType.FIELD_TOO_LONG)
                .withParam("field", field)
                .withParam("entity", "PointOfInterest")
                .withParam("entityId", "MOBIITI:PointOfInterest:70")
                .withParam("line", "29")
                .withParam("length", String.valueOf(length))
                .withParam("maxLength", "255");
    }

    /**
     * A general frame without parking (here: organisations) before the site frame must not break the validation of
     * the points of interest
     */
    @Test
    void populatesStructuredErrorsOnMissingCentroidAfterGeneralFrame() throws IOException {
        Job job = runNetexPoiImport("poi_missing_centroid_with_general_frame.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                missingPoiField("Centroid/Location/Longitude", "29"),
                missingPoiField("Centroid/Location/Latitude", "29"));
    }

    @Test
    void populatesStructuredErrorsOnInvalidIdFormat() throws IOException {
        Job job = runNetexPoiImport("poi_invalid_id.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_ID)
                .withParam("field", "id")
                .withParam("entity", "PointOfInterest")
                .withParam("entityId", "INVALID_ID")
                .withParam("line", "15")
                .withParam("errorValue", "INVALID_ID"));
    }

    @Test
    void stopsAfterTooManyErrors() throws IOException {
        // 150 points of interest without centroid: 2 missing data each
        Job job = runNetexPoiImport("poi_too_many_errors.xml");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).hasSize(201);
        assertThat(job.getErrors().subList(0, 200)).extracting(JobError::getCode).containsOnly(AnalyzeImportErrorType.MISSING_DATA);
        assertThat(job.getErrors().getLast()).isEqualTo(JobError.error(AnalyzeImportErrorType.TOO_MANY_ERRORS).withParam("maxErrors", "200"));
        assertThat(poiRepository.count()).isZero();
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
