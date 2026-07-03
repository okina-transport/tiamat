package org.rutebanken.tiamat.rest.dto;

public class DTOClusterMarker {

    private final int clusterId;
    private final double longitude;
    private final double latitude;
    private final Long size;


    public DTOClusterMarker(Object[] clusterMarkerFromDB) {
        this.clusterId = (int) clusterMarkerFromDB[0];
        this.longitude = (double) clusterMarkerFromDB[1];
        this.latitude = (double) clusterMarkerFromDB[2];
        this.size = (Long) clusterMarkerFromDB[3];
    }

    public int getClusterId() {
        return clusterId;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public Long getSize() {
        return size;
    }
}
