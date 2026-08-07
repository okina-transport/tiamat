package org.rutebanken.tiamat.model.csv;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum PointOfInterestCsvHeader {

    ID("id"),
    NAME("name"),
    HOUSE_NUMBER("addr:housenumber"),
    STREET("addr:street"),
    CITY("addr:city"),
    POST_CODE("addr:postcode"),
    LONGITUDE("lon"),
    LATITUDE("lat"),
    AMENITY("amenity"),
    BUILDING("building"),
    HISTORIC("historic"),
    LANDUSE("landuse"),
    LEISURE("leisure"),
    TOURISM("tourism"),
    OFFICE("office"),
    SHOP("shop"),
    LP_IMPORT_ID("LP_IMPORT_ID"),
    OPERATOR("operator");

    private final String value;

    PointOfInterestCsvHeader(String value) {
        this.value = value;
    }

    public static List<String> headerNames() {
        return Arrays.stream(values()).map(PointOfInterestCsvHeader::value).collect(Collectors.toList());
    }

    public String value() {
        return value;
    }
}