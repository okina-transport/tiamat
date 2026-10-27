package org.rutebanken.tiamat.general;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.AnalyzeImportError;
import org.rutebanken.tiamat.model.job.JobError;
import org.rutebanken.tiamat.rest.dto.DtoBikeParking;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BikesCSVHelperTest {

    private static final String RESOURCES_DIR = "src/test/resources/manualImports/bikeParkingErrors/";

    @Test
    void reportsDuplicatedIdsWithTheirLine() throws IOException {
        List<DtoBikeParking> bikeParkings = BikesCSVHelper.parseDocument(fileStream("bike_duplicated_id.csv"));

        DuplicatedIdentifierException exception = assertThrows(DuplicatedIdentifierException.class,
                () -> BikesCSVHelper.checkDuplicatedBikeParkings(bikeParkings, "bike_duplicated_id.csv"));

        assertThat(exception.getMessage())
                .isEqualTo("There are duplicated bike parkings in your CSV File 'With the same ID'. Duplicates:1001");
        assertThat(exception.toJobErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.DUPLICATE_ID_FILE)
                        .withParam("fileName", "bike_duplicated_id.csv")
                        .withParam("field", "id_local")
                        .withParam("errorValue", "1001")
                        .withParam("line", "4"));
    }

    @Test
    void detectsUnparsableNumbers() {
        AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                () -> BikesCSVHelper.parseDocument(fileStream("bike_invalid_numbers.csv")));

        assertThat(exception.getErrors())
                .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.INVALID_FORMAT_NUMBER))
                .extracting(AnalyzeImportError::getLine, AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                .containsExactly(
                        tuple(2, "capacite", "douze"),
                        tuple(3, "capacite_cargo", "2.5"));
    }

    @Test
    void reportsPhysicalLineDespiteMultiLineFieldsAndBlankLines() {
        AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                () -> BikesCSVHelper.parseDocument(fileStream("bike_multiline_and_blank_lines.csv")));

        assertThat(exception.getErrors())
                .extracting(AnalyzeImportError::getLine, AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                .containsExactly(tuple(5, "capacite", "douze"));
    }

    @Test
    void detectsInvalidDates() {
        AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                () -> BikesCSVHelper.parseDocument(fileStream("bike_invalid_dates.csv")));

        assertThat(exception.getErrors())
                .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.INVALID_FORMAT_DATE))
                .extracting(AnalyzeImportError::getLine, AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                .containsExactly(
                        tuple(2, "date_maj", "17/12/2021"),
                        tuple(3, "date_maj", "2021-02-30"));
    }

    @Test
    void detectsInvalidEnumValues() {
        AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                () -> BikesCSVHelper.parseDocument(fileStream("bike_invalid_enums.csv")));

        // "TRUE" is accepted: booleans are read ignoring case
        assertThat(exception.getErrors())
                .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.INVALID_FORMAT_ENUM))
                .extracting(AnalyzeImportError::getLine, AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                .containsExactly(
                        tuple(2, "mobilier", "BANC"),
                        tuple(3, "gratuit", "oui"),
                        tuple(4, "couverture", "non"));
        assertThat(exception.getErrors().get(1).getAllowedValues()).containsExactly("true", "false");
    }

    @Test
    void detectsFileWithoutDataRows() {
        AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                () -> BikesCSVHelper.parseDocument(fileStream("bike_header_only.csv")));

        assertThat(exception.getErrors()).extracting(AnalyzeImportError::getType)
                .containsExactly(AnalyzeImportErrorType.NO_DATA_ROWS);
    }

    @Test
    void detectsEmptyFile() {
        AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                () -> BikesCSVHelper.parseDocument(fileStream("bike_empty.csv")));

        assertThat(exception.getErrors()).extracting(AnalyzeImportError::getType)
                .containsExactly(AnalyzeImportErrorType.EMPTY_FILE);
    }

    @Test
    void parsesValidFile() throws IOException {
        List<DtoBikeParking> bikeParkings = BikesCSVHelper.parseDocument(fileStream("bike_valid.csv"));
        assertThat(bikeParkings).hasSize(2);
    }

    @Test
    void detectsInvalidUtf8Encoding() {
        AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                () -> BikesCSVHelper.parseDocument(fileStream("../parkings/parkings_bad_encoding.csv")));

        assertThat(exception.getErrors())
                .hasSize(1)
                .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.ENCODING));
    }

    @Test
    void detectsTemplateMismatch() {
        AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                () -> BikesCSVHelper.parseDocument(fileStream("bike_bad_headers.csv")));

        assertThat(exception.getErrors())
                .hasSize(1)
                .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.TEMPLATE));
        assertThat(exception.getErrors().get(0).getMessage())
                .contains("id_local")
                .contains("nom");
    }

    @Test
    void aggregatesMissingRequiredDataAcrossRows() {
        AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                () -> BikesCSVHelper.parseDocument(fileStream("bike_missing_data.csv")));

        List<AnalyzeImportError> errors = exception.getErrors();
        assertThat(errors).isNotEmpty();
        assertThat(errors).allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.MISSING_DATA));
        assertThat(errors.size()).isGreaterThanOrEqualTo(3);
        assertThat(errors).extracting(AnalyzeImportError::getLine).doesNotContainNull();
    }

    private InputStream fileStream(String fileName) throws IOException {
        return new FileInputStream(RESOURCES_DIR + fileName);
    }
}
