package org.rutebanken.tiamat.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class PointOfInterestOpeningHours extends EntityInVersionStructure{

    @OneToMany(cascade = CascadeType.ALL)
    private Set<DayType> dayType= new HashSet<>();;

    public Set<DayType> getDaysType() {
        return dayType;
    }

    public void setDaysType(Set<DayType> value) {
        this.dayType = value;
    }

}
