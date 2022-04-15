package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class CSVHelper {


    public static Iterable<CSVRecord> getRecords(InputStream csvFile) throws IOException {



        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = csvFile.read(buffer)) > -1) {
            baos.write(buffer, 0, len);
        }
        baos.flush();

        InputStream is1 = new ByteArrayInputStream(baos.toByteArray());
        InputStream is2 = new ByteArrayInputStream(baos.toByteArray());
        String result = IOUtils.toString(is1, StandardCharsets.UTF_8);
        String delimiter = guessDelimiter(result);

        Reader reader = new InputStreamReader(is2);

        return CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDelimiter(delimiter)
                .build()
                .parse(reader);
    }

    private static String guessDelimiter(String fileContent){

        String[] lines = fileContent.split("\n");
        String firstLine = lines[0];
        long nbOfSemiColon = firstLine.chars()
                .filter(ch -> ch == ';')
                .count();

        long nbOfComma = firstLine.chars()
                .filter(ch -> ch == ',')
                .count();

        return nbOfSemiColon > nbOfComma ? ";" : ",";



    }
}
