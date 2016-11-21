

package org.rutebanken.tiamat.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


public class Point_VersionStructure
    extends DataManagedObjectStructure
{

    protected MultilingualStringEntity name;
    protected String pointNumber;
    protected TypeOfPointRefs_RelStructure types;
    protected Projections_RelStructure projections;
    protected GroupMembershipRefs_RelStructure groupMemberships;

    public MultilingualStringEntity getName() {
        return name;
    }

    public void setName(MultilingualStringEntity value) {
        this.name = value;
    }

    public String getPointNumber() {
        return pointNumber;
    }

    public void setPointNumber(String value) {
        this.pointNumber = value;
    }

    public TypeOfPointRefs_RelStructure getTypes() {
        return types;
    }

    public void setTypes(TypeOfPointRefs_RelStructure value) {
        this.types = value;
    }

    public Projections_RelStructure getProjections() {
        return projections;
    }

    public void setProjections(Projections_RelStructure value) {
        this.projections = value;
    }

    public GroupMembershipRefs_RelStructure getGroupMemberships() {
        return groupMemberships;
    }

    public void setGroupMemberships(GroupMembershipRefs_RelStructure value) {
        this.groupMemberships = value;
    }

}
