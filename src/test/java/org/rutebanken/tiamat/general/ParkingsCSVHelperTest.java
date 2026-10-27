package org.rutebanken.tiamat.general;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingLayoutEnumeration;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.ParkingUserEnumeration;
import org.rutebanken.tiamat.model.job.AnalyzeImportError;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;
import org.rutebanken.tiamat.rest.dto.DtoParking;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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
    void mapsFrenchAndNetexUserTypes() throws IOException {
        try (InputStream is = fileStream("parkings_valid.csv")) {
            List<Parking> parkings = ParkingsCSVHelper.mapFromDtoToEntity(ParkingsCSVHelper.parseDocument(is),
                    ParkingLayoutEnumeration.COVERED, ParkingTypeEnumeration.PARKING_ZONE, false);

            assertThat(parkings)
                    .extracting(parking -> parking.getParkingProperties().get(0).getParkingUserTypes())
                    .containsExactly(List.of(ParkingUserEnumeration.ALL), List.of(ParkingUserEnumeration.REGISTERED));
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
    void reportsLineOfDuplicatedParkings() throws IOException {
        try (InputStream is = fileStream("parkings_duplicated_id.csv")) {
            List<DtoParking> parkings = ParkingsCSVHelper.parseDocument(is);

            DuplicatedIdentifierException exception = assertThrows(DuplicatedIdentifierException.class,
                    () -> ParkingsCSVHelper.checkDuplicatedParkings(parkings, "parkings_duplicated_id.csv"));

            assertThat(exception.toJobErrors()).containsExactly(
                    JobError.error(AnalyzeImportErrorType.DUPLICATE_ID_FILE)
                            .withParam("fileName", "parkings_duplicated_id.csv")
                            .withParam("field", "id")
                            .withParam("errorValue", "P1")
                            .withParam("line", "3"));
        }
    }

    @Test
    void detectsFileWithoutDataRows() throws IOException {
        try (InputStream is = fileStream("parkings_header_only.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> ParkingsCSVHelper.parseDocument(is));

            assertThat(exception.getErrors()).extracting(AnalyzeImportError::getType)
                    .containsExactly(AnalyzeImportErrorType.NO_DATA_ROWS);
        }
    }

    @Test
    void detectsEmptyFile() throws IOException {
        try (InputStream is = fileStream("parkings_empty.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> ParkingsCSVHelper.parseDocument(is));

            assertThat(exception.getErrors()).extracting(AnalyzeImportError::getType)
                    .containsExactly(AnalyzeImportErrorType.EMPTY_FILE);
        }
    }

    @Test
    void detectsUnparsableNumbers() throws IOException {
        try (InputStream is = fileStream("parkings_invalid_numbers.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> ParkingsCSVHelper.parseDocument(is));

            assertThat(exception.getErrors())
                    .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.INVALID_FORMAT_NUMBER))
                    .extracting(AnalyzeImportError::getLine, AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                    .containsExactly(
                            tuple(2, "nb_places", "dix"),
                            tuple(3, "nb_velo", "1.5"),
                            tuple(3, "hauteur_max", "2m"));
        }
    }

    @Test
    void reportsPhysicalLineDespiteMultiLineFieldsAndBlankLines() throws IOException {
        try (InputStream is = fileStream("parkings_multiline_and_blank_lines.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> ParkingsCSVHelper.parseDocument(is));

            assertThat(exception.getErrors())
                    .extracting(AnalyzeImportError::getLine, AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                    .containsExactly(tuple(5, "nb_places", "dix"));
        }
    }

    @Test
    void detectsNonDecimalCoordinates() throws IOException {
        try (InputStream is = fileStream("parkings_invalid_coordinates.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> ParkingsCSVHelper.parseDocument(is));

            assertThat(exception.getErrors())
                    .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.INVALID_FORMAT_COORDINATE))
                    .extracting(AnalyzeImportError::getLine, AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                    .containsExactly(
                            tuple(2, "Xlong", "2"),
                            tuple(3, "Ylat", "48.86N"));
        }
    }

    @Test
    void detectsValuesOutsideOfEnums() throws IOException {
        try (InputStream is = fileStream("parkings_invalid_enums.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> ParkingsCSVHelper.parseDocument(is));

            assertThat(exception.getErrors())
                    .allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.INVALID_FORMAT_ENUM))
                    .extracting(AnalyzeImportError::getLine, AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                    .containsExactly(
                            tuple(2, "type_usagers", "visiteurs"),
                            tuple(3, "type_ouvrage", "souterrain"));
            assertThat(exception.getErrors().get(0).getAllowedValues()).startsWith("tous", "abonnés").contains("all", "registered", "visitors");
            assertThat(exception.getErrors().get(1).getAllowedValues()).contains("enclos_en_surface", "ouvrage", "underground");
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
                    .contains("type_usagers", "gratuit", "nb_places", "hauteur_max", "num_siret", "operator");
        }
    }

    @Test
    void aggregatesMissingRequiredDataAcrossRows() throws IOException {
        try (InputStream is = fileStream("parkings_missing_data.csv")) {
            AnalyzeImportException exception = assertThrows(AnalyzeImportException.class,
                    () -> ParkingsCSVHelper.parseDocument(is));

            List<AnalyzeImportError> errors = exception.getErrors();
            // row 2 (missing id), row 3 (missing name+insee) -> at least 3 missing data errors
            assertThat(errors).filteredOn(error -> error.getType() == AnalyzeImportErrorType.MISSING_DATA)
                    .size().isGreaterThanOrEqualTo(3);
            // row 4 (bad longitude)
            assertThat(errors).filteredOn(error -> error.getType() == AnalyzeImportErrorType.INVALID_FORMAT_COORDINATE)
                    .extracting(AnalyzeImportError::getField, AnalyzeImportError::getErrorValue)
                    .containsExactly(tuple("Xlong", "notanumber"));
            assertThat(errors).extracting(AnalyzeImportError::getLine).doesNotContainNull();
        }
    }

    private InputStream fileStream(String fileName) throws IOException {
        return new FileInputStream(RESOURCES_DIR + fileName);
    }
}
