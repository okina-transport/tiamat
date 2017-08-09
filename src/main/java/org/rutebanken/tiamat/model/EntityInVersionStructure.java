package org.rutebanken.tiamat.model;

import javax.persistence.*;
import javax.persistence.Version;
import java.time.Instant;

@MappedSuperclass
public class EntityInVersionStructure extends EntityStructure {


    @AttributeOverrides({
            @AttributeOverride(name = "fromDate", column = @Column(name = "from_date")),
            @AttributeOverride(name = "toDate", column = @Column(name = "to_date"))
    })
    @Embedded
    private ValidBetween validBetween;

    private String versionComment;

    private String changedBy;

    @Transient
    protected String dataSourceRef;

    protected Instant created;

    protected Instant changed;

    @Transient
    protected ModificationEnumeration modification;

    protected long version;

    @Version
    private Long concurrentCheck;

    @Transient
    protected StatusEnumeration status;

    @Transient
    protected String derivedFromVersionRef;

    @Transient
    protected String compatibleWithVersionFrameVersionRef;

    @Transient
    protected String derivedFromObjectRef;

    public String getDataSourceRef() {
        return dataSourceRef;
    }


    public void setDataSourceRef(String value) {
        this.dataSourceRef = value;
    }


    public Instant getCreated() {
        return created;
    }


    public void setCreated(Instant value) {
        this.created = value;
    }


    public Instant getChanged() {
        return changed;
    }


    public void setChanged(Instant value) {
        this.changed = value;
    }

    public ValidBetween getValidBetween() {
        return validBetween;
    }

    public void setValidBetween(ValidBetween validBetween) {
        this.validBetween = validBetween;
    }

    public ModificationEnumeration getModification() {
        if (modification == null) {
            return ModificationEnumeration.NEW;
        } else {
            return modification;
        }
    }


    public void setModification(ModificationEnumeration value) {
        this.modification = value;
    }


    public long getVersion() {
        return version;
    }


    public void setVersion(long value) {
        this.version = value;
    }


    public StatusEnumeration getStatus() {
        if (status == null) {
            return StatusEnumeration.ACTIVE;
        } else {
            return status;
        }
    }


    public void setStatus(StatusEnumeration value) {
        this.status = value;
    }


    public String getDerivedFromVersionRef() {
        return derivedFromVersionRef;
    }


    public void setDerivedFromVersionRef(String value) {
        this.derivedFromVersionRef = value;
    }


    public String getCompatibleWithVersionFrameVersionRef() {
        return compatibleWithVersionFrameVersionRef;
    }


    public void setCompatibleWithVersionFrameVersionRef(String value) {
        this.compatibleWithVersionFrameVersionRef = value;
    }


    public String getDerivedFromObjectRef() {
        return derivedFromObjectRef;
    }


    public void setDerivedFromObjectRef(String value) {
        this.derivedFromObjectRef = value;
    }

    public String getVersionComment() {
        return versionComment;
    }

    public void setVersionComment(String versionComment) {
        this.versionComment = versionComment;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}
