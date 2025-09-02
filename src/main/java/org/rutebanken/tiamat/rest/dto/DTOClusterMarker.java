package org.rutebanken.tiamat.rest.dto;


import java.math.BigInteger;

public class DTOClusterMarker {

    private int clusterId;
    private double longitude;
    private double latitude;
    private BigInteger size;


    public DTOClusterMarker(Object[] clusterMarkerFromDB) {
        this.clusterId = (int) clusterMarkerFromDB[0];
        this.longitude = (double) clusterMarkerFromDB[1];
        this.latitude = (double) clusterMarkerFromDB[2];
        this.size = (BigInteger) clusterMarkerFromDB[3];
    }
}
