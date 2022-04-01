package org.rutebanken.tiamat.rest.dto;

import com.google.common.base.MoreObjects;

public class DtoPointOfInterest {

    private final static String delimeterToIgnoreCommasInQuotes1 = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

    private String street;
    private String id;
    private String name;
    private String houseNumber;
    private String city;
    private String postCode;
    private String longitude;
    private String latitude;
    private String amenity;
    private String leisure;
    private String office;
    private String shop;
    private String lpImportId;
    private String building;
    private String historic;
    private String landuse;
    private String tourism;

    public DtoPointOfInterest() {
    }

    public DtoPointOfInterest(String rawString) {

        String[] values = rawString.split(delimeterToIgnoreCommasInQuotes1);

        id = removeQuotes(values[0]);
        name = removeQuotes(values[1]);
        houseNumber = removeQuotes(values[2]);
        street = removeQuotes(values[3]);
        city = removeQuotes(values[4]);
        postCode = removeQuotes(values[5]);
        longitude = removeQuotes(values[6].replace(",","."));
        latitude = removeQuotes(values[7].replace(",","."));
        amenity = removeQuotes(values[8]);
        building = removeQuotes(values[9]);
        historic =  removeQuotes(values[10]);
        landuse =  removeQuotes(values[11]);
        leisure = removeQuotes(values[12]);
        tourism = removeQuotes(values[13]);
        office = removeQuotes(values[14]);
        shop = removeQuotes(values[15]);
        lpImportId = removeQuotes(values[16].replace("\r",""));

    }

    private String removeQuotes(String rawInput){

        if (rawInput.startsWith("\"") && rawInput.endsWith("\"")){
            return rawInput.substring(1,rawInput.length() - 1);
        }

        return rawInput;
    }


    public static String getDelimeterToIgnoreCommasInQuotes1() {
        return delimeterToIgnoreCommasInQuotes1;
    }

    public String getStreet() {
        return street;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public String getCity() {
        return city;
    }

    public String getPostCode() {
        return postCode;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getAmenity() {
        return amenity;
    }

    public String getLeisure() {
        return leisure;
    }

    public String getOffice() {
        return office;
    }

    public String getShop() {
        return shop;
    }

    public String getLpImportId() {
        return lpImportId;
    }

    public String getBuilding() {
        return building;
    }

    public String getHistoric() {
        return historic;
    }

    public String getLanduse() {
        return landuse;
    }

    public String getTourism() {
        return tourism;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("name", name)
                .add("houseNumber", houseNumber)
                .add("street", street)
                .add("city", city)
                .add("postCode", postCode)
                .add("longitude", longitude)
                .add("latitude", latitude)
                .add("amenity", amenity)
                .add("building", building)
                .add("historic", historic)
                .add("landuse", landuse)
                .add("leisure", leisure)
                .add("tourism", tourism)
                .add("office", office)
                .add("shop", shop)
                .add("lpImportId", lpImportId)
                .toString();
    }
}
