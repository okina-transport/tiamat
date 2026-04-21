package org.rutebanken.tiamat.versioning.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class FileNameUtils {

    private FileNameUtils() {
        throw new IllegalStateException();
    }
    private static final Pattern ZIP_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]+(?:\\.zip)?$", Pattern.CASE_INSENSITIVE);

    private static final String INVALID_FILENAME = "Invalid filename";
    private static final String FILENAME_STRUCTURE_INFOS = "The file name is invalid. It must contain only letters, numbers, hyphens (-), underscores (_) or dots (.), and may optionally end with .zip.";

    public static boolean isValidZipName(String fileName) {
        return StringUtils.isNotBlank(fileName) && ZIP_NAME_PATTERN.matcher(fileName).matches();
    }

    public static String extractBaseNameIfValid(String fileName) {
        if (!isValidZipName(fileName)) {
            throw new IllegalArgumentException(
                    String.format("%s: '%s'. %s", INVALID_FILENAME, fileName, FILENAME_STRUCTURE_INFOS)
            );
        }
        if (fileName.toLowerCase().endsWith(".zip")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }
}
