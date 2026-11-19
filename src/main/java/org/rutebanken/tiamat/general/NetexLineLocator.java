package org.rutebanken.tiamat.general;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the line of an imported NeTEx file where an entity is declared, from its id.
 * The validation runs on the unmarshalled objects, which do not keep their location in the file.
 */
public class NetexLineLocator {

    /**
     * An id attribute, the lookbehind prevents matching attributes ending with "id", such as "ref" or "xml:id"
     */
    private static final Pattern ID_ATTRIBUTE = Pattern.compile("(?<![\\w:.-])id\\s*=\\s*([\"'])(.*?)\\1");

    private final String content;

    public NetexLineLocator(String content) {
        this.content = content;
    }

    /**
     * @param id id of a NeTEx entity
     * @return the 1-based line of the first {@code id="..."} attribute having this value, or null if not found
     */
    public Integer findIdLine(String id) {
        if (content == null || id == null || id.isEmpty()) {
            return null;
        }
        // the lookbehind prevents matching attributes ending with "id", such as "ref" or "xml:id"
        Pattern pattern = Pattern.compile("(?<![\\w:.-])id\\s*=\\s*([\"'])" + Pattern.quote(escapeXml(id)) + "\\1");
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        int line = 1;
        for (int i = 0; i < matcher.start(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /**
     * @return every {@code id="..."} attribute whose value was already used by a previous one in the file,
     * with the line of this later occurrence, in the order of the file
     */
    public List<DuplicatedIdentifierException.Duplicate> findDuplicatedIds() {
        List<DuplicatedIdentifierException.Duplicate> duplicates = new ArrayList<>();
        if (content == null) {
            return duplicates;
        }
        Set<String> ids = new HashSet<>();
        Matcher matcher = ID_ATTRIBUTE.matcher(content);
        int line = 1;
        int lastPosition = 0;
        while (matcher.find()) {
            for (int i = lastPosition; i < matcher.start(); i++) {
                if (content.charAt(i) == '\n') {
                    line++;
                }
            }
            lastPosition = matcher.start();
            String id = unescapeXml(matcher.group(2));
            if (!ids.add(id)) {
                duplicates.add(new DuplicatedIdentifierException.Duplicate(id, line));
                if (ImportErrorLimit.isExceeded(duplicates.size())) {
                    break;
                }
            }
        }
        return duplicates;
    }

    private static String unescapeXml(String value) {
        return value.replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
