package org.rutebanken.tiamat.model.job;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobErrorListConverterTest {

    private final JobErrorListConverter converter = new JobErrorListConverter();

    @Test
    void writesErrorsAsJson() {
        List<JobError> errors = List.of(
                JobError.error(AnalyzeImportErrorType.DUPLICATE_ID_FILE)
                        .withParam("fileName", "parkings.csv")
                        .withParam("field", "id")
                        .withParam("value", "P1"),
                JobError.warning(AnalyzeImportErrorType.ROWS_IGNORED));

        assertThat(converter.convertToDatabaseColumn(errors)).isEqualTo(
                "[{\"code\":\"DUPLICATE_ID_FILE\",\"severity\":\"ERROR\",\"params\":{\"fileName\":\"parkings.csv\",\"field\":\"id\",\"value\":\"P1\"}}," +
                        "{\"code\":\"ROWS_IGNORED\",\"severity\":\"WARNING\",\"params\":{}}]");
    }

    @Test
    void readsBackWrittenErrors() {
        List<JobError> errors = List.of(
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_NUMBER).withParam("line", "3").withParam("field", "nb_places"),
                JobError.warning(AnalyzeImportErrorType.POSITION_CHANGED));

        assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(errors))).isEqualTo(errors);
    }

    @Test
    void readsLegacyErrorCodes() {
        assertThat(converter.convertToEntityAttribute("[\"TEMPLATE\",\"MISSING_DATA\"]")).containsExactly(
                JobError.error(AnalyzeImportErrorType.TEMPLATE),
                JobError.error(AnalyzeImportErrorType.MISSING_DATA));
    }

    @Test
    void emptyErrorsAreStoredAsNull() {
        assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }
}
