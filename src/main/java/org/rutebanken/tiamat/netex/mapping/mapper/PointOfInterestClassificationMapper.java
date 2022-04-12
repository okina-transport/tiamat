package org.rutebanken.tiamat.netex.mapping.mapper;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.rutebanken.netex.model.PointOfInterest;
import org.rutebanken.netex.model.PointOfInterestClassification;

public class PointOfInterestClassificationMapper extends CustomMapper<PointOfInterestClassification, org.rutebanken.tiamat.model.PointOfInterestClassification> {


    @Override
    public void mapAtoB(PointOfInterestClassification pointOfInterestClassification, org.rutebanken.tiamat.model.PointOfInterestClassification pointOfInterestClassification2, MappingContext context) {
        super.mapAtoB(pointOfInterestClassification, pointOfInterestClassification2, context);
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.PointOfInterestClassification pointOfInterestClassification, PointOfInterestClassification pointOfInterestClassification2, MappingContext context) {
        super.mapBtoA(pointOfInterestClassification, pointOfInterestClassification2, context);
    }
}
