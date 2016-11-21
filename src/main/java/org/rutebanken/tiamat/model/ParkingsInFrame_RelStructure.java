package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;


public class ParkingsInFrame_RelStructure
        extends ContainmentAggregationStructure {

    protected List<Parking> parking;

    public List<Parking> getParking() {
        if (parking == null) {
            parking = new ArrayList<Parking>();
        }
        return this.parking;
    }

}
