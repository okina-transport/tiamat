package org.rutebanken.tiamat.model;

import org.hibernate.annotations.*;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;

@Entity
public class PathLinkEnd {

    @Id
    @GeneratedValue(generator="idgen")
    @GenericGenerator(name = "idgen",
            strategy = "org.rutebanken.tiamat.repository.GaplessOptionalGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = SequenceStyleGenerator.CONFIG_PREFER_SEQUENCE_PER_ENTITY, value = "true")
            })
    private long id;

    @ManyToOne
    private StopPlace stopPlace;

    @ManyToOne
    private Quay quay;

    @Transient
    @OneToOne
    private PointOfInterest pointOfInterest;

    @Transient
    @OneToOne
    private AccessSpace accessSpace;

    @ManyToOne
    private PathJunction pathJunction;

    @Transient
    private SiteEntrance entrance;

    @Transient
    private Level level;

    public PathLinkEnd(Quay quay) {
        this.quay = quay;
    }

    public PathLinkEnd() {
    }

    public PathLinkEnd(StopPlace stopPlace) {
        this.stopPlace = stopPlace;
    }

    public PathLinkEnd(AccessSpace accessSpace) {
        this.accessSpace = accessSpace;
    }

    public PathLinkEnd(SiteEntrance entrance) {
        this.entrance = entrance;
    }

    public PathLinkEnd(PathJunction pathJunction) { this.pathJunction = pathJunction; }

    public PathLinkEnd(Level level) {
        this.level = level;
    }

    public SiteEntrance getEntrance() {
        return entrance;
    }

    public Level getLevel() {
        return level;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public StopPlace getStopPlace() {
        return stopPlace;
    }

    public Quay getQuay() {
        return quay;
    }

    public PathJunction getPathJunction() {
        return pathJunction;
    }

    public void setPathJunction(PathJunction pathJunction) {
        this.pathJunction = pathJunction;
    }
}
