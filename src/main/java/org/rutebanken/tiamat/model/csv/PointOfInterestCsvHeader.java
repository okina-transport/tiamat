package org.rutebanken.tiamat.model.csv;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum PointOfInterestCsvHeader {

    ID("id"),
    NAME("name"),
    HOUSE_NUMBER("houseNumber"),
    STREET("street"),
    CITY("city"),
    POST_CODE("postCode"),
    LONGITUDE("longitude"),
    LATITUDE("latitude"),
    AMENITY("amenity"),
    BUILDING("building"),
    HISTORIC("historic"),
    LANDUSE("landuse"),
    LEISURE("leisure"),
    TOURISM("tourism"),
    OFFICE("office"),
    SHOP("shop"),
    LP_IMPORT_ID("lpImportId"),
    OPERATOR("operator");

    private final String value;

    PointOfInterestCsvHeader(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static List<String> headerNames() {
        return Arrays.stream(values()).map(PointOfInterestCsvHeader::value).collect(Collectors.toList());
    }
}