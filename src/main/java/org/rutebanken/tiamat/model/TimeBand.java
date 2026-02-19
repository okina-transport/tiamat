package org.rutebanken.tiamat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalTime;

@Entity
public class TimeBand extends EntityInVersionStructure{

    private LocalTime startTime;

    private LocalTime endTime;

    @Column(name = "day_offset")
    private Integer dayOffset;

    public LocalTime getStartTime() {
        return this.startTime;
    }

    public void setStartTime(LocalTime value) {
        this.startTime = value;
    }

    public LocalTime getEndTime() {
        return this.endTime;
    }

    public void setEndTime(LocalTime value) {
        this.endTime = value;
    }

    public Integer getDayOffset() {
        return dayOffset;
    }

    public void setDayOffset(Integer dayOffset) {
        this.dayOffset = dayOffset;
    }
}
