package org.rutebanken.tiamat.general;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.AnalyzeImportError;
import org.rutebanken.tiamat.rest.dto.DtoBikeParking;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BikesCSVHelperTest {

    private static final String RESOURCES_DIR = "src/test/resources/manualImports/bikeParkingErrors/";

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
