package org.rutebanken.tiamat.model;

import java.math.BigDecimal;


public class PointProjection_VersionStructure
        extends Projection_VersionStructure {

    protected PointRefStructure projectedPointRef;
    protected PointRefStructure projectToPointRef;
    protected LinkRefStructure projectToLinkRef;
    protected BigDecimal distance;

    public PointRefStructure getProjectedPointRef() {
        return projectedPointRef;
    }

    public void setProjectedPointRef(PointRefStructure value) {
        this.projectedPointRef = value;
    }

    public PointRefStructure getProjectToPointRef() {
        return projectToPointRef;
    }

    public void setProjectToPointRef(PointRefStructure value) {
        this.projectToPointRef = value;
    }

    public LinkRefStructure getProjectToLinkRef() {
        return projectToLinkRef;
    }

    public void setProjectToLinkRef(LinkRefStructure value) {
        this.projectToLinkRef = value;
    }

    public BigDecimal getDistance() {
        return distance;
    }

    public void setDistance(BigDecimal value) {
        this.distance = value;
    }

}
