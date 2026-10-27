package org.rutebanken.tiamat.general;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CSVHelperTest {

    private static final List<String> EXPECTED = List.of("id", "nom", "Xlong", "Ylat");

    @Test
    void acceptsExpectedHeaders() {
        assertDoesNotThrow(() -> CSVHelper.validateHeaders(EXPECTED, List.of("id", "nom", "Xlong", "Ylat"), "test"));
    }

    @Test
    void reportsMissingAndUnexpectedColumns() {
        InvalidTemplateException exception = assertThrows(InvalidTemplateException.class,
                () -> CSVHelper.validateHeaders(EXPECTED, List.of("id", "name", "Xlong"), "test"));

        assertThat(exception.getMissingColumns()).containsExactly("nom", "Ylat");
        assertThat(exception.getUnexpectedColumns()).containsExactly("name");
        assertThat(exception.isOrderIssue()).isFalse();
    }

    @Test
    void reportsOrderIssue() {
        InvalidTemplateException exception = assertThrows(InvalidTemplateException.class,
                () -> CSVHelper.validateHeaders(EXPECTED, List.of("id", "nom", "Ylat", "Xlong"), "test"));

        assertThat(exception.getMissingColumns()).isEmpty();
        assertThat(exception.getUnexpectedColumns()).isEmpty();
        assertThat(exception.isOrderIssue()).isTrue();
    }

    @Test
    void reportsOrderIssueAlongWithMissingColumns() {
        InvalidTemplateException exception = assertThrows(InvalidTemplateException.class,
                () -> CSVHelper.validateHeaders(EXPECTED, List.of("nom", "id", "Xlong"), "test"));

        assertThat(exception.getMissingColumns()).containsExactly("Ylat");
        assertThat(exception.isOrderIssue()).isTrue();
    }
}
