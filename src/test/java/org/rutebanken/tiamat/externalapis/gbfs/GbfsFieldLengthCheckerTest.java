package org.rutebanken.tiamat.externalapis.gbfs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStationInformation;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSSystemInformation;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GbfsFieldLengthCheckerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Sizes of the database columns (as in the migrations), deliberately small for the tests
     */
    private static final Map<String, Integer> COLUMN_LENGTHS = Map.ofEntries(
            Map.entry("value_items.items", 10),
            Map.entry("organisation.language", 10),
            Map.entry("organisation.name", 10),
            Map.entry("organisation.short_name", 10),
            Map.entry("organisation.operator", 10),
            Map.entry("parking.operator", 10),
            Map.entry("organisation.organisation_url", 10),
            Map.entry("organisation.purchase_url", 10),
            Map.entry("organisation.phone_number", 5),
            Map.entry("organisation.email", 10),
            Map.entry("organisation.android_store_uri", 10),
            Map.entry("organisation.android_discovery_uri", 10),
            Map.entry("organisation.ios_store_uri", 10),
            Map.entry("organisation.ios_discovery_uri", 10),
            Map.entry("parking.name_value", 10),
            Map.entry("parking_area.name_value", 10),
            Map.entry("parking.name_lang", 5),
            Map.entry("parking_area.name_lang", 5),
            Map.entry("parking.short_name_value", 10),
            Map.entry("parking.short_name_lang", 5),
            Map.entry("multilingual_string_entity.value", 10),
            Map.entry("parking.booking_url", 10),
            Map.entry("parking.rental_uri_ios", 10),
            Map.entry("parking.rental_uri_android", 10));

    private static final GbfsFieldLengthChecker.ColumnLengths LENGTHS = (table, column) -> COLUMN_LENGTHS.get(table + "." + column);

    @Test
    void acceptsValuesFittingTheirColumn() throws Exception {
        GBFSSystemInformation systemInformation = systemInformation("""
                {"system_id": "sys", "languages": ["fr"], "name": [{"text": "Velos", "language": "fr"}],
                 "phone_number": "12345", "url": "http://a"}
                """);
        GBFSStationInformation stationInformation = stationInformation("""
                {"station_id": "42", "name": [{"text": "Gare", "language": "fr"}], "lat": 45.19, "lon": 5.72,
                 "cross_street": "Rue A", "rental_uris": {"web": "http://w"}}
                """);

        assertThat(GbfsFieldLengthChecker.check(systemInformation, stationInformation, LENGTHS)).isEmpty();
    }

    @Test
    void reportsEveryStationFieldTooLong() throws Exception {
        GBFSStationInformation stationInformation = stationInformation("""
                {"station_id": "42", "name": [{"text": "Nom de station trop long", "language": "fr"}], "lat": 45.19, "lon": 5.72,
                 "short_name": [{"text": "Nom court trop long", "language": "zh-Hant"}],
                 "cross_street": "Rue tres longue", "address": "%s",
                 "rental_uris": {"web": "http://web.example", "ios": "http://ios.example", "android": "http://android.example"}}
                """.formatted("a".repeat(50)));

        assertThat(GbfsFieldLengthChecker.check(null, stationInformation, LENGTHS)).containsExactly(
                stationViolation("name", 24, 10),
                stationViolation("short_name", 19, 10),
                stationViolation("short_name.language", 7, 5),
                stationViolation("cross_street", 15, 10),
                stationViolation("rental_uris.web", 18, 10),
                stationViolation("rental_uris.ios", 18, 10),
                stationViolation("rental_uris.android", 22, 10));
    }

    @Test
    void checksOnlyTheFrenchNameAndItsLanguage() throws Exception {
        GBFSStationInformation stationInformation = stationInformation("""
                {"station_id": "42", "lat": 45.19, "lon": 5.72, "name": [
                  {"text": "English name too long", "language": "en"},
                  {"text": "Gare", "language": "fr"}]}
                """);

        assertThat(GbfsFieldLengthChecker.check(null, stationInformation, LENGTHS)).isEmpty();
    }

    @Test
    void reportsStationIdTooLongAndPositionOfStationWithoutId() throws Exception {
        GBFSStationInformation stationInformation = stationInformation("""
                {"station_id": "identifiant_trop_long", "name": [{"text": "Gare", "language": "zh-Hant"}], "lat": 45.19, "lon": 5.72}
                """);

        assertThat(GbfsFieldLengthChecker.check(null, stationInformation, LENGTHS)).containsExactly(
                new GbfsFieldLengthChecker.Violation("station_information.json", "station", "identifiant_trop_long", 1, "station_id", 21, 10),
                new GbfsFieldLengthChecker.Violation("station_information.json", "station", "identifiant_trop_long", 1, "name.language", 7, 5));
    }

    @Test
    void reportsEverySystemInformationFieldTooLong() throws Exception {
        GBFSSystemInformation systemInformation = systemInformation("""
                {"system_id": "systeme_trop_long", "languages": ["fr"],
                 "name": [{"text": "Nom trop long", "language": "fr"}],
                 "short_name": [{"text": "Court trop long", "language": "fr"}],
                 "operator": [{"text": "Operateur trop long", "language": "fr"}],
                 "url": "http://url.example", "purchase_url": "http://purchase.example",
                 "phone_number": "01 02 03 04 05", "email": "contact@example.com",
                 "rental_apps": {
                   "android": {"store_uri": "http://store.android", "discovery_uri": "android://discovery"},
                   "ios": {"store_uri": "http://store.ios", "discovery_uri": "ios://discovery"}}}
                """);

        assertThat(GbfsFieldLengthChecker.check(systemInformation, null, LENGTHS))
                .extracting(GbfsFieldLengthChecker.Violation::field)
                .containsExactly("system_id", "name", "short_name", "operator", "url", "purchase_url", "phone_number",
                        "email", "rental_apps.android.store_uri", "rental_apps.android.discovery_uri",
                        "rental_apps.ios.store_uri", "rental_apps.ios.discovery_uri");
        assertThat(GbfsFieldLengthChecker.check(systemInformation, null, LENGTHS))
                .filteredOn(violation -> violation.field().equals("phone_number"))
                .containsExactly(new GbfsFieldLengthChecker.Violation("system_information.json", null, null, null, "phone_number", 14, 5));
    }

    private static GbfsFieldLengthChecker.Violation stationViolation(String field, int length, int maxLength) {
        return new GbfsFieldLengthChecker.Violation("station_information.json", "station", "42", 1, field, length, maxLength);
    }

    private static GBFSStationInformation stationInformation(String station) throws Exception {
        return MAPPER.readValue("""
                {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"stations": [%s]}}
                """.formatted(station), GBFSStationInformation.class);
    }

    private static GBFSSystemInformation systemInformation(String data) throws Exception {
        return MAPPER.readValue("""
                {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": %s}
                """.formatted(data), GBFSSystemInformation.class);
    }
}
