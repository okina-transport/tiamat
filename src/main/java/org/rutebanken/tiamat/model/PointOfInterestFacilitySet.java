package org.rutebanken.tiamat.model;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Entity
public class PointOfInterestFacilitySet extends EntityInVersionStructure{

    @Enumerated(EnumType.STRING)
    private TicketingFacilityEnumeration ticketingFacility;

    @Enumerated(EnumType.STRING)
    private TicketingServiceFacilityEnumeration ticketingServiceFacility;

    public TicketingFacilityEnumeration getTicketingFacility() {
        return ticketingFacility;
    }

    public void setTicketingFacility(TicketingFacilityEnumeration ticketingFacility) {
        this.ticketingFacility = ticketingFacility;
    }

    public TicketingServiceFacilityEnumeration getTicketingServiceFacility() {
        return ticketingServiceFacility;
    }

    public void setTicketingServiceFacility(TicketingServiceFacilityEnumeration ticketingServiceFacility) {
        this.ticketingServiceFacility = ticketingServiceFacility;
    }
}
