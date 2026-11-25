package org.rutebanken.tiamat.rest.netex.publicationdelivery;

import org.rutebanken.util.LocalDateTimeISO8601XmlAdapter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class FractionalSecondsLocalDateTimeXmlAdapter extends LocalDateTimeISO8601XmlAdapter {

    private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .optionalStart()
            .appendPattern("XXXXX")
            .optionalEnd()
            .parseDefaulting(ChronoField.OFFSET_SECONDS, OffsetDateTime.now().getLong(ChronoField.OFFSET_SECONDS))
            .toFormatter();

    @Override
    public LocalDateTime unmarshal(String value) {
        return LocalDateTime.parse(value, PARSER);
    }
}
