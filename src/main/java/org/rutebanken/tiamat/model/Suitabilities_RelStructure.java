package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;


public class Suitabilities_RelStructure
        extends StrictContainmentAggregationStructure {

    protected List<Suitability> suitability;

    public List<Suitability> getSuitability() {
        if (suitability == null) {
            suitability = new ArrayList<Suitability>();
        }
        return this.suitability;
    }

}
