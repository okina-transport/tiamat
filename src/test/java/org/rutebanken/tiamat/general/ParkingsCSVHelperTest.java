package org.rutebanken.tiamat.general;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.job.AnalyzeImportError;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.rest.dto.DtoParking;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkingsCSVHelperTest {

    private static final String RESOURCES_DIR = "src/test/resources/manualImports/parkings/";

    @Test
    void parsesValidFile() throws IOException {
        try (InputStream is = fileStream("parkings_valid.csv")) {
            List<DtoParking> parkings = ParkingsCSVHelper.parseDocument(is);
            assertThat(parkings).hasSize(2);
        }
    }

    @Test
    void detectsInvalidUtf8Encoding() throws IOException {
        try (InputStream is = fileStream("parkings_bad_encoding.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> ParkingsCSVHelper.parseDocument(is));

            assertThat(exception.getErrors())
                    .hasSize(1)
                    .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.ENCODING));
        }
    }

    @Test
    void detectsTemplateMismatch() throws IOException {
        try (InputStream is = fileStream("parkings_bad_headers.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> ParkingsCSVHelper.parseDocument(is));

            assertThat(exception.getErrors())
                    .hasSize(1)
                    .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.TEMPLATE));
            assertThat(exception.getErrors().get(0).getMessage())
                    .contains("insee")
                    .contains("operator");
        }
    }

    @Test
    void detectsMissingPdmMandatoryFields() throws IOException {
        try (InputStream is = fileStream("parkings_missing_pdm_mandatory_fields.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> ParkingsCSVHelper.parseDocument(is));

            List<AnalyzeImportError> errors = exception.getErrors();
            assertThat(errors).allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.MISSING_DATA));
            assertThat(errors).extracting(AnalyzeImportError::getField)
                    .contains("userType", "free", "nbOfPlaces", "maxHeight", "siretNumber", "operator");
        }
    }

    @Test
    void aggregatesMissingRequiredDataAcrossRows() throws IOException {
        try (InputStream is = fileStream("parkings_missing_data.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> ParkingsCSVHelper.parseDocument(is));

            List<AnalyzeImportError> errors = exception.getErrors();
            assertThat(errors)
                    .isNotEmpty()
                    .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.MISSING_DATA));
            // row 2 (missing id), row 3 (missing name+insee), row 4 (bad longitude) -> at least 4 distinct field errors
            assertThat(errors).size().isGreaterThanOrEqualTo(4);
            assertThat(errors).extracting(AnalyzeImportError::getLine).doesNotContainNull();
        }
    }

    private InputStream fileStream(String fileName) throws IOException {
        return new FileInputStream(RESOURCES_DIR + fileName);
    }
}
