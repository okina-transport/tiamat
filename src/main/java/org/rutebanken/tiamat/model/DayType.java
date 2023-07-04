package org.rutebanken.tiamat.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class DayType extends EntityInVersionStructure{

    @Enumerated(EnumType.STRING)
    private DayOfWeekEnumeration dayOfWeek;

    @OneToMany(cascade = CascadeType.ALL)
    private Set<TimeBand> timeBand = new HashSet<>();

    public DayOfWeekEnumeration getDays() {
        return this.dayOfWeek;
    }

    public void setDays(DayOfWeekEnumeration value) {
        this.dayOfWeek = value;
    }

    public Set<TimeBand> getTimeBand() {
        return this.timeBand;
    }

    public void setTimeBand(Set<TimeBand> value) {
        this.timeBand = value;
    }


}
