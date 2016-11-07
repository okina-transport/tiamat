package org.rutebanken.tiamat.importers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Remove certain words from strings.
 * Typically words with brackets from stop place or quay names.
 */
@Component
public class WordsRemover {
    private static final Logger logger = LoggerFactory.getLogger(WordsRemover.class);
    private final List<Pattern> patterns = new ArrayList<>();

    public WordsRemover() {
        this(Arrays.asList("båt", "buss", "tog", "bussterminal"));
    }
    public WordsRemover(List<String> words) {

        for(String word : words) {
            patterns.add(Pattern.compile("\\("+word+"\\)", Pattern.CASE_INSENSITIVE));
            patterns.add(Pattern.compile("\\[("+word+")\\]", Pattern.CASE_INSENSITIVE));
        }
    }

    public String remove(String name) {
        String returnString = name;
        for(Pattern pattern : patterns) {
            returnString = pattern.matcher(returnString).replaceAll("");
        }
        return returnString.trim();
    }
}
