package org.rutebanken.tiamat.general;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.job.AnalyzeImportError;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Utf8HelperTest {

    @Test
    void doesNotThrowOnValidUtf8() {
        byte[] validBytes = "<Parking>Parc-relais é à ç</Parking>".getBytes(StandardCharsets.UTF_8);

        Utf8Helper.decodeStrictUtf8(validBytes, "NeTEx");
    }

    @Test
    void throwsEncodingErrorOnInvalidUtf8() {
        byte[] invalidBytes = "<Parking>Parking Test".getBytes(StandardCharsets.UTF_8);
        byte[] withInvalidByte = new byte[invalidBytes.length + 1];
        System.arraycopy(invalidBytes, 0, withInvalidByte, 0, invalidBytes.length);
        withInvalidByte[invalidBytes.length] = (byte) 0xE9;

        assertThatThrownBy(() -> Utf8Helper.decodeStrictUtf8(withInvalidByte, "NeTEx"))
                .isInstanceOf(AnalyzeImportException.class)
                .satisfies(e -> assertThat(((AnalyzeImportException) e).getErrors())
                        .extracting(AnalyzeImportError::getType)
                        .containsExactly(AnalyzeImportErrorType.ENCODING));
    }
}