package org.rutebanken.tiamat.rest.dto;


import org.apache.commons.csv.CSVRecord;


public class DtoTadStop {

    private String stopId;

    private String stopName;

    private String stopLat;

    private String stopLon;

    private String zoneId;

    private String locationType;

    private String parentStation;

    public DtoTadStop(CSVRecord csvRec) {

        stopId = removeQuotes(csvRec.get(0));
        stopName = removeQuotes(csvRec.get(1));
        stopLat = removeQuotes(csvRec.get(2));
        stopLon = removeQuotes(csvRec.get(3));
        zoneId = removeQuotes(csvRec.get(4));
        locationType = removeQuotes(csvRec.get(5));
        parentStation = removeQuotes(csvRec.get(6));
    }

    private String removeQuotes(String rawInput){

        if (rawInput.startsWith("\"") && rawInput.endsWith("\"")){
            return rawInput.substring(1,rawInput.length() - 1);
        }

        return rawInput;
    }

    public String getStopId() {
        return stopId;
    }

    public String getStopName() {
        return stopName;
    }

    public String getStopLat() {
        return stopLat;
    }

    public String getStopLon() { return stopLon; }

    public String getZoneId() { return zoneId; }

    public String getLocationType() { return locationType; }

    public String getParentStation() { return parentStation; }
}
