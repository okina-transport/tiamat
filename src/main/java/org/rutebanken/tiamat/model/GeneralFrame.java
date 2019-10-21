package org.rutebanken.tiamat.model;

import com.google.common.base.MoreObjects;

public class GeneralFrame extends Common_VersionFrameStructure {

    protected Members members;

    public Members getMembers() {
        return members;
    }

    public void setMembers(Members value) {
        this.members = value;
    }
}
