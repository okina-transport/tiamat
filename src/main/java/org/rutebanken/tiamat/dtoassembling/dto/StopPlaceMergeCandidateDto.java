package org.rutebanken.tiamat.dtoassembling.dto;

public class StopPlaceMergeCandidateDto {

    private final String netexId;
    private final String name;
    private final Double longitude;
    private final Double latitude;
    private final String modality;
    private final String provider;

    public StopPlaceMergeCandidateDto(Object[] row, int offset) {
        this.netexId = (String) row[offset];
        this.name = (String) row[offset + 1];
        this.longitude = (Double) row[offset + 2];
        this.latitude = (Double) row[offset + 3];
        this.modality = (String) row[offset + 4];
        this.provider = (String) row[offset + 5];
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
