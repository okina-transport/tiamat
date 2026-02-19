package org.rutebanken.tiamat.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.HashSet;
import java.util.Set;

@Entity
public class AvailabilityCondition extends EntityInVersionStructure {

    @OneToMany(cascade = CascadeType.ALL)
    Set<DayType> dayTypes = new HashSet<>();

    boolean isAvailable;

    public Set<DayType> getDayTypes() {
        return dayTypes;
    }

    public void setDayTypes(Set<DayType> dayTypes) {
        this.dayTypes = dayTypes;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
