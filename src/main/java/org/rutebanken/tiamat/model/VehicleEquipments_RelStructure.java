

package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


public class VehicleEquipments_RelStructure
    extends ContainmentAggregationStructure
{

    protected List<ActualVehicleEquipment_VersionStructure> accessVehicleEquipmentOrWheelchairVehicleEquipment;

    public List<ActualVehicleEquipment_VersionStructure> getAccessVehicleEquipmentOrWheelchairVehicleEquipment() {
        if (accessVehicleEquipmentOrWheelchairVehicleEquipment == null) {
            accessVehicleEquipmentOrWheelchairVehicleEquipment = new ArrayList<ActualVehicleEquipment_VersionStructure>();
        }
        return this.accessVehicleEquipmentOrWheelchairVehicleEquipment;
    }

}
