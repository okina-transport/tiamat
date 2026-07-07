package org.rutebanken.tiamat.general;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NetexXmlHelper {

    private NetexXmlHelper() {
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;

        while ((len = inputStream.read(buffer)) > -1) {
            baos.write(buffer, 0, len);
        }

        baos.flush();
        return baos.toByteArray();
    }
}
