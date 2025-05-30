package org.rutebanken.tiamat.model.gbfs;

import org.hibernate.validator.constraints.URL;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.SpecificParkingAreaUsageEnumeration;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class GbfsParkingImportParams {

    @URL
    private String globalUrl;

    @NotNull
    private ParkingTypeEnumeration parkingType;

    @NotNull
    private SpecificParkingAreaUsageEnumeration parkingAreaType;

    public GbfsParkingImportParams() {
    }

    public URI getGlobalUrl() {
        return URI.create(globalUrl);
    }

    public void setGlobalUrl(String globalUrl) {
        this.globalUrl = globalUrl;
    }

    public ParkingTypeEnumeration getParkingType() {
        return parkingType;
    }

    public void setParkingType(ParkingTypeEnumeration parkingType) {
        this.parkingType = parkingType;
    }

    public SpecificParkingAreaUsageEnumeration getParkingAreaType() {
        return parkingAreaType;
    }

    public void setParkingAreaType(SpecificParkingAreaUsageEnumeration parkingAreaType) {
        this.parkingAreaType = parkingAreaType;
    }

}