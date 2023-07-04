package org.rutebanken.tiamat.rest.graphql.helpers;


import org.rutebanken.netex.model.AvailabilityCondition;
import org.rutebanken.netex.model.ValidBetween_VersionStructure;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = ""
)
public class AvailabilityConditionWrapper {

    @XmlElement(name = "availabilityCondition")
    private AvailabilityCondition availabilityCondition;

    public AvailabilityConditionWrapper(AvailabilityCondition availabilityCondition) {
        this.availabilityCondition = availabilityCondition;
    }
}
