

package org.rutebanken.tiamat.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;


    "defaultDuration",
    "frequentTravellerDuration",
    "occasionalTravellerDuration",
public class TransferDurationStructure {

    protected Duration defaultDuration;
    protected Duration frequentTravellerDuration;
    protected Duration occasionalTravellerDuration;
    protected Duration mobilityRestrictedTravellerDuration;

    public Duration getDefaultDuration() {
        return defaultDuration;
    }

    public void setDefaultDuration(Duration value) {
        this.defaultDuration = value;
    }

    public Duration getFrequentTravellerDuration() {
        return frequentTravellerDuration;
    }

    public void setFrequentTravellerDuration(Duration value) {
        this.frequentTravellerDuration = value;
    }

    public Duration getOccasionalTravellerDuration() {
        return occasionalTravellerDuration;
    }

    public void setOccasionalTravellerDuration(Duration value) {
        this.occasionalTravellerDuration = value;
    }

    public Duration getMobilityRestrictedTravellerDuration() {
        return mobilityRestrictedTravellerDuration;
    }

    public void setMobilityRestrictedTravellerDuration(Duration value) {
        this.mobilityRestrictedTravellerDuration = value;
    }

}
