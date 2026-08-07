package org.rutebanken.tiamat.general;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.job.AnalyzeImportError;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.rest.dto.DtoPointOfInterest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PointOfInterestCSVHelperTest {

    private static final String RESOURCES_DIR = "src/test/resources/manualImports/poiErrors/";

    private final PointOfInterestCSVHelper poiHelper = new PointOfInterestCSVHelper();

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
            assertThat(errors).allSatisfy(error -> assertThat(error.getType()).isEqualTo(AnalyzeImportErrorType.MISSING_DATA));
            assertThat(errors).size().isGreaterThanOrEqualTo(3);
            assertThat(errors).extracting(AnalyzeImportError::getLine).doesNotContainNull();
        }
    }

    private InputStream fileStream(String fileName) throws IOException {
        return new FileInputStream(RESOURCES_DIR + fileName);
    }
}
