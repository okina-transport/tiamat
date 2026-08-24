package org.rutebanken.tiamat.dtoassembling.dto;

public class StopPlaceMergeCandidateDto {

    private final String netexId;
    private final String name;
    private final Double longitude;
    private final Double latitude;
    private final String modality;
    private final String provider;

    public StopPlaceMergeCandidateDto(String netexId, String name, Double longitude, Double latitude, String modality, String provider) {
        this.netexId = netexId;
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.modality = modality;
        this.provider = provider;
    }

    public String getNetexId() {
        return netexId;
    }

    public String getName() {
        return name;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public String getModality() {
        return modality;
    }

    public String getProvider() {
        return provider;
    }
}
