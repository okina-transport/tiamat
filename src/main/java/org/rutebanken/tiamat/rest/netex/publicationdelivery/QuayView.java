package org.rutebanken.tiamat.rest.netex.publicationdelivery;


import org.rutebanken.tiamat.model.Quay;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
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


    public QuayView(Quay quay){
        id = quay.getId();
        name = String.join(",", quay.getOriginalNames());
        netexId = quay.getNetexId();
        latitude = new BigDecimal(quay.getCentroid().getCoordinate().y);
        longitude = new BigDecimal(quay.getCentroid().getCoordinate().x);
        importedId = String.join(",", quay.getOriginalIds());

    }


}
