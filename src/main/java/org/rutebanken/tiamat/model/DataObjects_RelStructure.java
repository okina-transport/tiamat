package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;


public class DataObjects_RelStructure {

    protected List<Common_VersionFrameStructure> compositeFrameOrCommonFrame;

    public List<Common_VersionFrameStructure> getCompositeFrameOrCommonFrame() {
        if (compositeFrameOrCommonFrame == null) {
            compositeFrameOrCommonFrame = new ArrayList<Common_VersionFrameStructure>();
        }
        return this.compositeFrameOrCommonFrame;
    }

}
