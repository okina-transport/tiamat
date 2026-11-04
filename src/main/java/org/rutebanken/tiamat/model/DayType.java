package org.rutebanken.tiamat.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
public class DayType extends EntityInVersionStructure{

    @ElementCollection(targetClass = DayOfWeekEnumeration.class, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @Enumerated(EnumType.STRING)
    private List<DayOfWeekEnumeration> daysOfWeek = new ArrayList<>();

    @ElementCollection(targetClass = HolidayTypeEnumeration.class, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @Enumerated(EnumType.STRING)
    private List<HolidayTypeEnumeration> holidayTypes = new ArrayList<>();


    @OneToMany(cascade = CascadeType.ALL)
    private Set<TimeBand> timeBand = new HashSet<>();

    public List<DayOfWeekEnumeration> getDaysOfWeek() {
        return this.daysOfWeek;
    }

    public void setDaysOfWeek(List<DayOfWeekEnumeration> value) {
        this.daysOfWeek = value == null ? new ArrayList<>() : new ArrayList<>(value);
    }

    public List<HolidayTypeEnumeration> getHolidayTypes() {
        return this.holidayTypes;
    }

    public void setHolidayTypes(List<HolidayTypeEnumeration> value) {
        this.holidayTypes = value == null ? new ArrayList<>() : new ArrayList<>(value);
    }

    public Set<TimeBand> getTimeBand() {
        return this.timeBand;
    }

    public void setTimeBand(Set<TimeBand> value) {
        this.timeBand = value;
    }


}
