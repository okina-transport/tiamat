package org.rutebanken.tiamat.rest.netex.publicationdelivery;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.rutebanken.tiamat.model.Quay;

import java.math.BigDecimal;

@XmlRootElement(name = "quay")
@XmlAccessorType(XmlAccessType.FIELD)
public class QuayView {

    @XmlElement
    private Long id;

    @XmlElement
    private String name;

    @XmlElement
    private String netexId;

    @XmlElement
    private BigDecimal latitude;

    @XmlElement
    private BigDecimal longitude;

    @XmlElement
    private String importedId;

    @XmlElement
    private String netexStopPlaceId;

    @XmlElement
    private String stopPlaceImportedId;

    public String getStopPlaceImportedId() {
        return stopPlaceImportedId;
    }

    public void setStopPlaceImportedId(String stopPlaceImportedId) {
        this.stopPlaceImportedId = stopPlaceImportedId;
    }

    public String getNetexStopPlaceId() {
        return netexStopPlaceId;
    }

    public void setNetexStopPlaceId(String netexStopPlaceId) {
        this.netexStopPlaceId = netexStopPlaceId;
    }

    public String getNetexId() {
        return netexId;
    }

    public void setNetexId(String netexId) {
        this.netexId = netexId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getImportedId() {
        return importedId;
    }

    public void setImportedId(String importedId) {
        this.importedId = importedId;
    }

    public QuayView() {
    }

    public QuayView(Quay quay){
        id = quay.getId();
        name = String.join(",", quay.getOriginalNames());
        netexId = quay.getNetexId();
        latitude = new BigDecimal(quay.getCentroid().getCoordinate().y);
        longitude = new BigDecimal(quay.getCentroid().getCoordinate().x);
        importedId = String.join(",", quay.getOriginalIds());

    }


}
