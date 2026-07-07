package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;

public class CSVHelper {


    public static CSVParser getRecords(InputStream csvFile) throws IOException {


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;

        while ((len = csvFile.read(buffer)) > -1) {
            baos.write(buffer, 0, len);
        }


        baos.flush();

        String content = Utf8Helper.decodeStrictUtf8(baos.toByteArray(), "CSV");
        String delimiter = guessDelimiter(content);

        return CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDelimiter(delimiter)
                .build()
                .parse(new StringReader(content));
    }

    public static void validateHeaders(List<String> expectedHeaders, List<String> actualHeaders, String templateName) {
        if (expectedHeaders.equals(actualHeaders)) {
            return;
        }

        List<String> missing = expectedHeaders.stream()
                .filter(header -> !actualHeaders.contains(header))
                .toList();
        List<String> unexpected = actualHeaders.stream()
                .filter(header -> !expectedHeaders.contains(header))
                .toList();

        StringBuilder message = new StringBuilder("The CSV file does not match the expected " + templateName + " template.");
        if (!missing.isEmpty()) {
            message.append(" Missing column(s): ").append(String.join(", ", missing)).append(".");
        }
        if (!unexpected.isEmpty()) {
            message.append(" Unexpected column(s): ").append(String.join(", ", unexpected)).append(".");
        }
        if (missing.isEmpty() && unexpected.isEmpty()) {
            message.append(" Column order does not match the expected template: ").append(String.join(",", actualHeaders)).append(".");
        }

        throw new AnalyzeImportException(AnalyzeImportErrorType.TEMPLATE, message.toString());
    }

    private static String guessDelimiter(String fileContent) {

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
