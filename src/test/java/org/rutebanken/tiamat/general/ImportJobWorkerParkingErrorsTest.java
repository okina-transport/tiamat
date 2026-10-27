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
        assertThat(job.getErrors()).extracting(JobError::getCode).containsExactly(AnalyzeImportErrorType.ENCODING);
    }

    @Test
    void populatesStructuredErrorsOnFileWithoutDataRows() throws IOException {
        Job job = runParkingImport("parkings_header_only.csv");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.NO_DATA_ROWS));
    }

    @Test
    void populatesStructuredErrorsOnTemplateMismatch() throws IOException {
        Job job = runParkingImport("parkings_bad_headers.csv");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).hasSize(1);
        JobError error = job.getErrors().get(0);
        assertThat(error.getCode()).isEqualTo(AnalyzeImportErrorType.TEMPLATE);
        assertThat(error.getParams())
                .containsEntry("fileName", "parkings_bad_headers.csv")
                .containsEntry("orderIssue", "false");
        assertThat(error.getParams().get("missingColumns")).startsWith("nom, insee, adresse").endsWith("type_ouvrage, operator");
        assertThat(error.getParams().get("unExpectedColumns")).startsWith("name, inseeCode, address").endsWith("workType, operateur");
    }

    @Test
    void populatesOrderIssueOnWrongColumnOrder() throws IOException {
        Job job = runParkingImport("parkings_wrong_column_order.csv");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.TEMPLATE)
                        .withParam("fileName", "parkings_wrong_column_order.csv")
                        .withParam("unExpectedColumns", "")
                        .withParam("missingColumns", "")
                        .withParam("orderIssue", "true"));
    }

    @Test
    void populatesStructuredErrorsOnMissingRequiredData() throws IOException {
        Job job = runParkingImport("parkings_missing_data.csv");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                missingData("id", 2),
                missingData("num_siret", 2),
                missingData("insee", 3),
                missingData("nom", 3),
                missingData("num_siret", 3),
                missingData("num_siret", 4),
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_COORDINATE)
                        .withParam("field", "Xlong").withParam("line", "4").withParam("errorValue", "notanumber"));
    }

    @Test
    void populatesMessageOnDuplicatedIdentifier() throws IOException {
        Job job = runParkingImport("parkings_duplicated_id.csv");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getMessage())
                .isEqualTo("Error executing import job " + job.getId() + ". DuplicatedIdentifierException - "
                        + "There are duplicated parkings in your CSV File 'With the same ID & Name'. Duplicates:P1Parking Test");
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.DUPLICATE_ID_FILE)
                        .withParam("fileName", "parkings_duplicated_id.csv")
                        .withParam("field", "id")
                        .withParam("errorValue", "P1")
                        .withParam("line", "3"));
    }

    @Test
    void populatesOneErrorPerUnparsableNumber() throws IOException {
        Job job = runParkingImport("parkings_invalid_numbers.csv");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_NUMBER)
                        .withParam("field", "nb_places").withParam("line", "2").withParam("errorValue", "dix"),
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_NUMBER)
                        .withParam("field", "nb_velo").withParam("line", "3").withParam("errorValue", "1.5"),
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_NUMBER)
                        .withParam("field", "hauteur_max").withParam("line", "3").withParam("errorValue", "2m"));
    }

    @Test
    void populatesOneErrorPerNonDecimalCoordinate() throws IOException {
        Job job = runParkingImport("parkings_invalid_coordinates.csv");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_COORDINATE)
                        .withParam("field", "Xlong").withParam("line", "2").withParam("errorValue", "2"),
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_COORDINATE)
                        .withParam("field", "Ylat").withParam("line", "3").withParam("errorValue", "48.86N"));
    }

    @Test
    void populatesOneErrorPerValueOutsideOfEnum() throws IOException {
        Job job = runParkingImport("parkings_invalid_enums.csv");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_ENUM)
                        .withParam("field", "type_usagers").withParam("allowedValues", "tous, abonnés, allUsers, staff, visitors, registeredDisabled, registered, rental, doctors, residentsWithPermits, reservationHolders, emergencyServices, other, all")
                        .withParam("line", "2").withParam("errorValue", "visiteurs"),
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_ENUM)
                        .withParam("field", "type_ouvrage")
                        .withParam("allowedValues", "enclos_en_surface, ouvrage, covered, openSpace, multistorey, underground, roadside, undefined, other, cycleHire")
                        .withParam("line", "3").withParam("errorValue", "souterrain"));
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

    private static JobError missingData(String field, int line) {
        return JobError.error(AnalyzeImportErrorType.MISSING_DATA)
                .withParam("field", field)
                .withParam("line", String.valueOf(line));
    }
}
