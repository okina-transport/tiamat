

package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


public class GroupOfLinkSequences_RelStructure
    extends StrictContainmentAggregationStructure
{

    protected List<GroupOfLinkSequences> groupOfLinkSequences;

    public List<GroupOfLinkSequences> getGroupOfLinkSequences() {
        if (groupOfLinkSequences == null) {
            groupOfLinkSequences = new ArrayList<GroupOfLinkSequences>();
        }
        return this.groupOfLinkSequences;
    }

}
