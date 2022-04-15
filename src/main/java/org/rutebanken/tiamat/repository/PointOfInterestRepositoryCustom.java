package org.rutebanken.tiamat.repository;


import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.PointOfInterestFacilitySet;
import org.rutebanken.tiamat.model.TicketingFacilityEnumeration;
import org.rutebanken.tiamat.model.TicketingServiceFacilityEnumeration;

import java.util.Iterator;

public interface PointOfInterestRepositoryCustom  extends DataManagedObjectStructureRepository<PointOfInterest>{
    int countResult();

    Iterator<PointOfInterest> scrollPointsOfInterest();

    void clearAllPois();
}
