package org.rutebanken.tiamat.rest.netex.publicationdelivery;


import org.rutebanken.tiamat.model.Quay;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
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

    public QuayView() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNetexId() {
        return netexId;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getImportedId() {
        return importedId;
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
