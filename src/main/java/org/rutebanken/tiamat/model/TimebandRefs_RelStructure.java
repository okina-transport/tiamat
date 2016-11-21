

package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


public class TimebandRefs_RelStructure
    extends OneToManyRelationshipStructure
{

    protected List<TimebandRefStructure> timebandRef;

    public List<TimebandRefStructure> getTimebandRef() {
        if (timebandRef == null) {
            timebandRef = new ArrayList<TimebandRefStructure>();
        }
        return this.timebandRef;
    }

}
