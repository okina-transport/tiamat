package org.rutebanken.tiamat.externalapis.gbfs;

import org.apache.commons.collections4.CollectionUtils;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStation;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStationInformation;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSData;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSSystemInformation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Checks that the text values of a GBFS feed imported by Tiamat fit in their database column. The values are checked
 * on the GBFS objects, before they are mapped, so that the errors give the GBFS field (ex: rental_uris.web) and the
 * station. Only the values stored by the mappers are checked (ex: only the selected translation of a name). The
 * address of a station is not checked: it is truncated (see {@code GbfsParkingImporter}).
 */
public final class GbfsFieldLengthChecker {

    /**
     * Gives the size of a text column of the database
     */
    @FunctionalInterface
    public interface ColumnLengths {
        /**
         * @return the maximum length of the column, null when it is unknown or unlimited
         */
        Integer maxLength(String table, String column);
    }

    /**
     * A text value longer than its database column
     *
     * @param file     name of the file (ex: station_information.json)
     * @param entity   "station", or null for a field of system_information.json
     * @param entityId station_id, null when it is itself missing
     * @param position position of the station in its list, starting at 1; null when entity is null
     * @param field    GBFS field (ex: rental_uris.web, name.language)
     * @param length   length of the value, in characters
     * @param maxLength size of the database column
     */
    public record Violation(String file, String entity, String entityId, Integer position, String field, int length,
                            int maxLength) implements Serializable {
    }

    private static final String STATION_INFORMATION_FILE = "station_information.json";
    private static final String SYSTEM_INFORMATION_FILE = "system_information.json";
    private static final String FRENCH = "fr";

    // database tables
    private static final String ORGANISATION_TABLE = "organisation";
    private static final String PARKING_TABLE = "parking";
    private static final String PARKING_AREA_TABLE = "parking_area";
    private static final String VALUE_ITEMS_TABLE = "value_items";
    private static final String VALUE_ITEMS_COLUMN = "items";

    // GBFS fields stored in a column of the same name
    private static final String OPERATOR = "operator";
    private static final String SHORT_NAME = "short_name";

    private GbfsFieldLengthChecker() {
    }

    /**
     * @return the text values exceeding their database column, the fields of system_information.json first
     */
    public static List<Violation> check(GBFSSystemInformation systemInformation, GBFSStationInformation stationInformation,
                                        ColumnLengths columnLengths) {
        List<Violation> violations = new ArrayList<>();
        if (systemInformation != null && systemInformation.getData() != null) {
            checkSystemInformation(systemInformation.getData(), columnLengths, violations);
        }
        if (stationInformation != null && stationInformation.getData() != null && stationInformation.getData().getStations() != null) {
            List<GBFSStation> stations = stationInformation.getData().getStations();
            for (int i = 0; i < stations.size(); i++) {
                checkStation(stations.get(i), i + 1, columnLengths, violations);
            }
        }
        return violations;
    }

    /**
     * Same selection of the values as SystemInformationMapper
     */
    private static void checkSystemInformation(GBFSData data, ColumnLengths columnLengths, List<Violation> violations) {
        FieldChecker checker = new FieldChecker(SYSTEM_INFORMATION_FILE, null, null, null, columnLengths, violations);
        checker.check("system_id", data.getSystemId(), VALUE_ITEMS_TABLE, VALUE_ITEMS_COLUMN);
        String language = CollectionUtils.isNotEmpty(data.getLanguages()) ? data.getLanguages().getFirst() : null;
        checker.check("languages", language, ORGANISATION_TABLE, "language");
        if (CollectionUtils.isNotEmpty(data.getName())) {
            checker.check("name", data.getName().getFirst().getText(), ORGANISATION_TABLE, "name");
        }
        if (data.getShortName() != null) {
            data.getShortName().stream().filter(shortName -> Objects.equals(language, shortName.getLanguage())).findFirst()
                    .ifPresent(shortName -> checker.check(SHORT_NAME, shortName.getText(), ORGANISATION_TABLE, SHORT_NAME));
        }
        if (data.getOperator() != null) {
            data.getOperator().stream().filter(operator -> Objects.equals(language, operator.getLanguage())).findFirst()
                    .ifPresent(operator -> {
                        checker.check(OPERATOR, operator.getText(), ORGANISATION_TABLE, OPERATOR);
                        // the operator of the organisation is also the operator of its parkings
                        checker.check(OPERATOR, operator.getText(), PARKING_TABLE, OPERATOR);
                    });
        }
        checker.check("url", data.getUrl(), ORGANISATION_TABLE, "organisation_url");
        checker.check("purchase_url", data.getPurchaseUrl(), ORGANISATION_TABLE, "purchase_url");
        checker.check("phone_number", data.getPhoneNumber(), ORGANISATION_TABLE, "phone_number");
        checker.check("email", data.getEmail(), ORGANISATION_TABLE, "email");
        if (data.getRentalApps() != null) {
            if (data.getRentalApps().getAndroid() != null) {
                checker.check("rental_apps.android.store_uri", data.getRentalApps().getAndroid().getStoreUri(), ORGANISATION_TABLE, "android_store_uri");
                checker.check("rental_apps.android.discovery_uri", data.getRentalApps().getAndroid().getDiscoveryUri(), ORGANISATION_TABLE, "android_discovery_uri");
            }
            if (data.getRentalApps().getIos() != null) {
                checker.check("rental_apps.ios.store_uri", data.getRentalApps().getIos().getStoreUri(), ORGANISATION_TABLE, "ios_store_uri");
                checker.check("rental_apps.ios.discovery_uri", data.getRentalApps().getIos().getDiscoveryUri(), ORGANISATION_TABLE, "ios_discovery_uri");
            }
        }
    }

    /**
     * Same selection of the values as StationInformationMapper: the French name, or the first one
     */
    private static void checkStation(GBFSStation station, int position, ColumnLengths columnLengths, List<Violation> violations) {
        FieldChecker checker = new FieldChecker(STATION_INFORMATION_FILE, "station", station.getStationId(), position,
                columnLengths, violations);
        checker.check("station_id", station.getStationId(), VALUE_ITEMS_TABLE, VALUE_ITEMS_COLUMN);
        if (CollectionUtils.isNotEmpty(station.getName())) {
            var name = station.getName().stream().filter(n -> FRENCH.equals(n.getLanguage())).findFirst()
                    .orElse(station.getName().getFirst());
            checker.check("name", name.getText(), PARKING_TABLE, "name_value");
            checker.check("name", name.getText(), PARKING_AREA_TABLE, "name_value");
            checker.check("name.language", name.getLanguage(), PARKING_TABLE, "name_lang");
            checker.check("name.language", name.getLanguage(), PARKING_AREA_TABLE, "name_lang");
        }
        if (CollectionUtils.isNotEmpty(station.getShortName())) {
            var shortName = station.getShortName().stream().filter(n -> FRENCH.equals(n.getLanguage())).findFirst()
                    .orElse(station.getShortName().getFirst());
            checker.check(SHORT_NAME, shortName.getText(), PARKING_TABLE, "short_name_value");
            checker.check("short_name.language", shortName.getLanguage(), PARKING_TABLE, "short_name_lang");
        }
        checker.check("cross_street", station.getCrossStreet(), "multilingual_string_entity", "value");
        if (station.getRentalUris() != null) {
            checker.check("rental_uris.web", station.getRentalUris().getWeb(), PARKING_TABLE, "booking_url");
            checker.check("rental_uris.ios", station.getRentalUris().getIos(), PARKING_TABLE, "rental_uri_ios");
            checker.check("rental_uris.android", station.getRentalUris().getAndroid(), PARKING_TABLE, "rental_uri_android");
        }
    }

    private record FieldChecker(String file, String entity, String entityId, Integer position,
                                ColumnLengths columnLengths, List<Violation> violations) {

        /**
         * Reports the value when it exceeds the column. A GBFS field stored in several columns is reported once.
         */
        void check(String field, String value, String table, String column) {
            if (value == null) {
                return;
            }
            Integer maxLength = columnLengths.maxLength(table, column);
            if (maxLength == null) {
                return;
            }
            int length = value.codePointCount(0, value.length());
            if (length <= maxLength) {
                return;
            }
            boolean alreadyReported = violations.stream().anyMatch(violation -> Objects.equals(violation.file(), file)
                    && Objects.equals(violation.entityId(), entityId) && Objects.equals(violation.position(), position)
                    && violation.field().equals(field));
            if (!alreadyReported) {
                violations.add(new Violation(file, entity, entityId, entity != null ? position : null, field, length, maxLength));
            }
        }
    }
}
