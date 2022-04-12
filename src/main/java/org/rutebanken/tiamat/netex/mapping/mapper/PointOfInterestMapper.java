package org.rutebanken.tiamat.netex.mapping.mapper;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.model.PointOfInterestFacilitySet;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PointOfInterestMapper extends CustomMapper<PointOfInterest, org.rutebanken.tiamat.model.PointOfInterest> {

    @Autowired
    private PointOfInterestRepository pointOfInterestRepository;

    @Override
    public void mapAtoB(PointOfInterest pointOfInterest, org.rutebanken.tiamat.model.PointOfInterest pointOfInterest2, MappingContext context) {
        super.mapAtoB(pointOfInterest, pointOfInterest2, context);
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.PointOfInterest pointOfInterest, PointOfInterest pointOfInterest2, MappingContext context) {
        super.mapBtoA(pointOfInterest, pointOfInterest2, context);

        if (pointOfInterest.getPointOfInterestFacilitySetId() != null) {
            PointOfInterestFacilitySet pointOfInterestFacilitySet = pointOfInterestRepository.getPoiFacilitySet(pointOfInterest.getPointOfInterestFacilitySetId());
            if (pointOfInterestFacilitySet != null) {
                SiteFacilitySets_RelStructure siteFacilitySets_relStructure = new SiteFacilitySets_RelStructure();

                SiteFacilitySetRefStructure siteFacilitySetRefStructure = new SiteFacilitySetRefStructure();

                SiteFacilitySet siteFacilitySet = new SiteFacilitySet();
                siteFacilitySet.setId(pointOfInterestFacilitySet.getNetexId());
                siteFacilitySet.setVersion(pointOfInterest.getNetexId());
                siteFacilitySet.getTicketingFacilityList().add(TicketingFacilityEnumeration.fromValue(pointOfInterestFacilitySet.getTicketingFacility().value()));
                siteFacilitySet.getTicketingServiceFacilityList().add(TicketingServiceFacilityEnumeration.fromValue(pointOfInterestFacilitySet.getTicketingServiceFacility().value()));

                siteFacilitySets_relStructure.getSiteFacilitySetRefOrSiteFacilitySet().add(siteFacilitySet);
                pointOfInterest2.setFacilities(siteFacilitySets_relStructure);
            }
        }
    }
}
