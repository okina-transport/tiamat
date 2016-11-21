

package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


public class TypeOfZoneRefs_RelStructure
    extends OneToManyRelationshipStructure
{

    protected List<TypeOfZoneRefStructure> typeOfZoneRef;

    public List<TypeOfZoneRefStructure> getTypeOfZoneRef() {
        if (typeOfZoneRef == null) {
            typeOfZoneRef = new ArrayList<TypeOfZoneRefStructure>();
        }
        return this.typeOfZoneRef;
    }

}
