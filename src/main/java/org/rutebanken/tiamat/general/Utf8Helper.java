package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class Utf8Helper {

    private Utf8Helper() {
    }

    public static String decodeStrictUtf8(byte[] bytes, String fileType) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new AnalyzeImportException(AnalyzeImportErrorType.ENCODING,
                    "Le fichier " + fileType + " doit être encodé en UTF-8 valide. Un ou plusieurs caractères invalides ont été détectés.");
        }
    }
}
