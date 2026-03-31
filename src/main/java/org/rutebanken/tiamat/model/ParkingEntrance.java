package org.rutebanken.tiamat.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "parking_entrance")
public class ParkingEntrance extends ParkingEntranceForVehicles__VersionStructure {

    @Id
    @GeneratedValue(generator = "sequence_per_table_generator")
    @Schema(description = "Unique id for the entity")
    private Long id;

    private String netexId;

    private String version;

    private String name;

    private BigDecimal longitude;

    private BigDecimal latitude;

    public String getNetexId() {
        return netexId;
    }

    public void setNetexId(String netexId) {
        this.netexId = netexId;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }
}
