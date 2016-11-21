package org.rutebanken.tiamat.model;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.Duration;


public class JourneyLayoverStructure
        extends JourneyTiming_VersionedChildStructure {

    protected Duration layover;
    protected JAXBElement<? extends PointRefStructure> pointRef;

    public Duration getLayover() {
        return layover;
    }

    public void setLayover(Duration value) {
        this.layover = value;
    }

    public JAXBElement<? extends PointRefStructure> getPointRef() {
        return pointRef;
    }

    public void setPointRef(JAXBElement<? extends PointRefStructure> value) {
        this.pointRef = value;
    }

}
