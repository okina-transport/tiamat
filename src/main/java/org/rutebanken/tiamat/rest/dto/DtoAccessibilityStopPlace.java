package org.rutebanken.tiamat.rest.dto;

public class DtoAccessibilityStopPlace {
    private final String id;
    private final String name;
    private final String insee;
    private final String city;
    private final String idPoint;
    private final String direction;
    private final String line;
    private final String lineDirection;
    private final String longitude;
    private final String latitude;
    private final String url;
    private final String wheelchairAccess;
    private final String stepFreeAccess;
    private final String escalatorFreeAccess;
    private final String liftFreeAccess;
    private final String audibleSignalsAvailable;
    private final String visualSignsAvailable;


    public DtoAccessibilityStopPlace(String id, String name, String insee, String city, String idPoint, String direction, String line,
                                     String lineDirection, String longitude, String latitude, String url, String wheelchairAccess, String stepFreeAccess,
                                     String escalatorFreeAccess, String liftFreeAccess, String audibleSignalsAvailable, String visualSignsAvailable) {
        this.id = id;
        this.name = name;
        this.insee = insee;
        this.city = city;
        this.idPoint = idPoint;
        this.direction = direction;
        this.line = line;
        this.lineDirection = lineDirection;
        this.longitude = longitude;
        this.latitude = latitude;
        this.url = url;
        this.wheelchairAccess = wheelchairAccess;
        this.stepFreeAccess = stepFreeAccess;
        this.escalatorFreeAccess = escalatorFreeAccess;
        this.liftFreeAccess = liftFreeAccess;
        this.audibleSignalsAvailable = audibleSignalsAvailable;
        this.visualSignsAvailable = visualSignsAvailable;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getInsee() { return insee; }
    public String getCity() { return city; }
    public String getIdPoint() { return idPoint; }
    public String getDirection() { return direction; }
    public String getLine() { return line; }
    public String getLineDirection() { return lineDirection; }
    public String getLongitude() { return longitude; }
    public String getLatitude() { return latitude; }
    public String getUrl() { return url; }
    public String getWheelchairAccess() { return wheelchairAccess; }
    public String getStepFreeAccess() { return  stepFreeAccess; }
    public String getEscalatorFreeAccess() { return escalatorFreeAccess; }
    public String getLiftFreeAccess() { return liftFreeAccess; }
    public String getAudibleSignalsAvailable() { return audibleSignalsAvailable; }
    public String getVisualSignsAvailable() { return visualSignsAvailable; }
}
