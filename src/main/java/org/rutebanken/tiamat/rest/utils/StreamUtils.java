package org.rutebanken.tiamat.rest.utils;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {

    public static InputStream copyToInputStream(InputStream input) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(input, outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

}
