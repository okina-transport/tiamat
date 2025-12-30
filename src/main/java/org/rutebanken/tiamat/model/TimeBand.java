package org.rutebanken.tiamat.model;

import jakarta.persistence.Entity;
import java.time.Instant;

@Entity
public class TimeBand extends EntityInVersionStructure{

    private Instant startTime;

    private Instant endTime;

    public Instant getStartTime() {
        return this.startTime;
    }

    public void setStartTime(Instant value) {
        this.startTime = value;
    }

    public Instant getEndTime() {
        return this.endTime;
    }

    public void setEndTime(Instant value) {
        this.endTime = value;
    }
}
