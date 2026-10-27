package org.rutebanken.tiamat.general;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.job.AnalyzeImportError;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;
import org.rutebanken.tiamat.rest.dto.DtoPointOfInterest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PointOfInterestCSVHelperTest {

    private static final String RESOURCES_DIR = "src/test/resources/manualImports/poiErrors/";

    private final PointOfInterestCSVHelper poiHelper = new PointOfInterestCSVHelper();

    @Test
    void detectsEmptyFile() throws IOException {
        try (InputStream is = fileStream("poi_empty.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> poiHelper.parseDocument(is));

            assertThat(exception.getErrors()).extracting(AnalyzeImportError::getType)
                    .containsExactly(AnalyzeImportErrorType.EMPTY_FILE);
        }
    }

    @Test
    void reportsDuplicatedIdsWithTheirLine() throws IOException {
        try (InputStream is = fileStream("poi_duplicated_id.csv")) {
            List<DtoPointOfInterest> pois = poiHelper.parseDocument(is);

            DuplicatedIdentifierException exception = assertThrows(DuplicatedIdentifierException.class,
                    () -> PointOfInterestCSVHelper.checkDuplicatedPois(pois, "poi_duplicated_id.csv"));

            assertThat(exception.getMessage()).isEqualTo("There are duplicated POI in your CSV File (With the same ID & Name)");
            assertThat(exception.toJobErrors()).containsExactly(
                    JobError.error(AnalyzeImportErrorType.DUPLICATE_ID_FILE)
                            .withParam("fileName", "poi_duplicated_id.csv")
                            .withParam("field", "id")
                            .withParam("errorValue", "POI1")
                            .withParam("line", "4"));
        }
    }

    @Test
    void detectsUnparsableNumbers() throws IOException {
        try (InputStream is = fileStream("poi_invalid_numbers.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> poiHelper.parseDocument(is));

            // line 5 is valid: a comma is accepted as decimal separator
            assertThat(exception.getErrors())
                    .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.INVALID_FORMAT_NUMBER))
                    .extracting(AnalyzeImportError::getLine, AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                    .containsExactly(
                            tuple(2, "lon", "abc"),
                            tuple(3, "lat", "48.85N"),
                            tuple(4, "lon", "2"));
        }
    }

    @Test
    void reportsPhysicalLineDespiteMultiLineFieldsAndBlankLines() throws IOException {
        try (InputStream is = fileStream("poi_multiline_and_blank_lines.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> poiHelper.parseDocument(is));

            assertThat(exception.getErrors())
                    .extracting(AnalyzeImportError::getLine, AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                    .containsExactly(tuple(5, "lon", "abc"));
        }
    }

    @Test
    void parsesValidFile() throws IOException {
        try (InputStream is = fileStream("poi_valid.csv")) {
            List<DtoPointOfInterest> pois = poiHelper.parseDocument(is);
            assertThat(pois).hasSize(2);
        }
    }

    @Test
    void detectsInvalidUtf8Encoding() throws IOException {
        try (InputStream is = fileStream("../parkings/parkings_bad_encoding.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> poiHelper.parseDocument(is));

            assertThat(exception.getErrors())
                    .hasSize(1)
                    .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.ENCODING));
        }
    }

    @Test
    void detectsTemplateMismatch() throws IOException {
        try (InputStream is = fileStream("poi_bad_headers.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> poiHelper.parseDocument(is));

            assertThat(exception.getErrors())
                    .hasSize(1)
                    .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.TEMPLATE));
            assertThat(exception.getErrors().getFirst().getMessage())
                    .contains("id")
                    .contains("operator");
        }
    }

    @Test
    void aggregatesMissingRequiredDataAcrossRows() throws IOException {
        try (InputStream is = fileStream("poi_missing_data.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> poiHelper.parseDocument(is));

            List<AnalyzeImportError> errors = exception.getErrors();
            assertThat(errors).isNotEmpty();
            assertThat(errors).filteredOn(error -> error.getType() == AnalyzeImportErrorType.MISSING_DATA)
                    .size().isGreaterThanOrEqualTo(2);
            // row 4 (bad longitude)
            assertThat(errors).filteredOn(error -> error.getType() == AnalyzeImportErrorType.INVALID_FORMAT_NUMBER)
                    .extracting(AnalyzeImportError::getLine, AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                    .containsExactly(tuple(4, "lon", "notanumber"));
            assertThat(errors).extracting(AnalyzeImportError::getLine).doesNotContainNull();
        }
    }

    @Test
    void detectsFileWithoutDataRows() throws IOException {
        try (InputStream is = fileStream("poi_header_only.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> poiHelper.parseDocument(is));

            assertThat(exception.getErrors())
                    .hasSize(1)
                    .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.NO_DATA_ROWS));
        }
    }

    private InputStream fileStream(String fileName) throws IOException {
        return new FileInputStream(RESOURCES_DIR + fileName);
    }
}
