package org.rutebanken.tiamat.repository;


import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.PointOfInterestFacilitySet;
import org.rutebanken.tiamat.model.TicketingFacilityEnumeration;
import org.rutebanken.tiamat.model.TicketingServiceFacilityEnumeration;

public interface PointOfInterestRepositoryCustom  extends DataManagedObjectStructureRepository<PointOfInterest>{

    PointOfInterestFacilitySet getOrCreateFacilitySet(TicketingFacilityEnumeration ticketingFacility, TicketingServiceFacilityEnumeration ticketingServiceFacility);
    void clearAllPois();
}
