package org.rutebanken.tiamat.rest.dto;

import com.google.common.base.MoreObjects;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

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
    private Map<String, String> tags = new HashMap<>();
    private String operator;

    public DtoPointOfInterest() {
    }

    public DtoPointOfInterest(CSVRecord csvRec) {
        id = removeQuotes(csvRec.get(0));
        name = removeQuotes(csvRec.get(1));
        houseNumber = removeQuotes(csvRec.get(2));
        street = removeQuotes(csvRec.get(3));
        city = removeQuotes(csvRec.get(4));
        postCode = removeQuotes(csvRec.get(5));
        longitude = removeQuotes(csvRec.get(6).replace(",","."));
        latitude = removeQuotes(csvRec.get(7).replace(",","."));
        amenity = removeQuotes(csvRec.get(8));
        building = removeQuotes(csvRec.get(9));
        historic =  removeQuotes(csvRec.get(10));
        landuse =  removeQuotes(csvRec.get(11));
        leisure = removeQuotes(csvRec.get(12));
        tourism = removeQuotes(csvRec.get(13));
        office = removeQuotes(csvRec.get(14));
        shop = removeQuotes(csvRec.get(15));
        lpImportId = removeQuotes(csvRec.get(16).replace("\r",""));
        operator = removeQuotes(csvRec.get(17));

        if (csvRec.size() > 18){
            addTag(csvRec.get(18));
        }
        if (csvRec.size() > 19){
            addTag(csvRec.get(19));
        }
    }

    private void addTag(String rawCell){

        if (StringUtils.isEmpty(rawCell) || !rawCell.contains("=")){
            return;
        }

        String[] cellArray = rawCell.split("=");
        String key = cellArray[0].trim();
        String value = cellArray[1].trim();
        tags.put(key, value);
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

    public Map<String, String> getTags(){
        return tags;
    }

    public void setOperator(String operator) { this.operator = operator; }

    public String getOperator() { return this.operator; }

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
                .add("operator", operator)
                .toString();
    }
}
