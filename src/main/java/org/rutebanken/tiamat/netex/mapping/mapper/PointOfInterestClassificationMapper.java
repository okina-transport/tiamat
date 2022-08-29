package org.rutebanken.tiamat.netex.mapping.mapper;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.rutebanken.netex.model.MultilingualString;

public class PointOfInterestClassificationMapper extends CustomMapper<org.rutebanken.netex.model.PointOfInterestClassification, org.rutebanken.tiamat.model.PointOfInterestClassification> {
    @Override
    public void mapAtoB(org.rutebanken.netex.model.PointOfInterestClassification netexPointOfInterestClassification, org.rutebanken.tiamat.model.PointOfInterestClassification tiamatPointOfInterestClassification, MappingContext context) {
        super.mapAtoB(netexPointOfInterestClassification, tiamatPointOfInterestClassification, context);
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.PointOfInterestClassification tiamatPointOfInterestClassification, org.rutebanken.netex.model.PointOfInterestClassification netexPointOfInterestClassification, MappingContext context) {
        super.mapBtoA(tiamatPointOfInterestClassification, netexPointOfInterestClassification, context);
        netexPointOfInterestClassification.setVersion("any");
        MultilingualString netexNamePointOfInterestClassification = new MultilingualString();
        netexNamePointOfInterestClassification.setValue(tiamatPointOfInterestClassification.getName().getValue());
        netexPointOfInterestClassification.setName(netexNamePointOfInterestClassification);
    }
}
