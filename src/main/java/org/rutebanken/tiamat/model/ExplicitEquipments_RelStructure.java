package org.rutebanken.tiamat.model;

import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;
import org.hibernate.mapping.Property;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import java.util.ArrayList;
import java.util.List;


@Entity
public class ExplicitEquipments_RelStructure
        extends ContainmentAggregationStructure {

    @ManyToAny(metaColumn = @Column(name = "item_type"))
    @AnyMetaDef(
            idType = "integer", metaType = "string",
            metaValues = {
                    @MetaValue(targetEntity = EquipmentRefStructure.class, value = "ERS"),
                    @MetaValue(targetEntity = Equipment_VersionStructure.class, value = "EVS")
            }
    )
    @JoinTable(
            name = "installedEquipment",
            joinColumns = @JoinColumn(name = "id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id")
    )
    protected List<Property> installedEquipment;

    public List<Property> getInstalledEquipment() {
        if (installedEquipment == null) {
            installedEquipment = new ArrayList<>();
        }
        return this.installedEquipment;
    }

}
