package org.rutebanken.tiamat.rest.dto;




public class DTOClusterMarker {

    private int clusterId;
    private double longitude;
    private double latitude;
    private Long size;


    public DTOClusterMarker(Object[] clusterMarkerFromDB) {
        this.clusterId = (int) clusterMarkerFromDB[0];
        this.longitude = (double) clusterMarkerFromDB[1];
        this.latitude = (double) clusterMarkerFromDB[2];
        this.size = (Long) clusterMarkerFromDB[3];
    }
}
