

package org.rutebanken.tiamat.model;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


    "assistanceAvailability",
    "wheelchairBookingRequired",
    "bookingContact",
    "bookingArrangements",
    "vehicleMode",
    "transportOrganisationRef",
    "lineRef",
    "bookedObjectRef",
public class AssistanceBookingService_VersionStructure
    extends LocalService_VersionStructure
{

    protected AssistanceAvailabilityEnumeration assistanceAvailability;
    protected Boolean wheelchairBookingRequired;
    protected ContactStructure bookingContact;
    protected BookingArrangementsStructure bookingArrangements;
    protected AllModesEnumeration vehicleMode;
    protected JAXBElement<? extends OrganisationRefStructure> transportOrganisationRef;
    protected JAXBElement<? extends LineRefStructure> lineRef;
    protected VersionOfObjectRefStructure bookedObjectRef;
    protected NoticeAssignments_RelStructure noticeAssignmenrts;

    public AssistanceAvailabilityEnumeration getAssistanceAvailability() {
        return assistanceAvailability;
    }

    public void setAssistanceAvailability(AssistanceAvailabilityEnumeration value) {
        this.assistanceAvailability = value;
    }

    public Boolean isWheelchairBookingRequired() {
        return wheelchairBookingRequired;
    }

    public void setWheelchairBookingRequired(Boolean value) {
        this.wheelchairBookingRequired = value;
    }

    public ContactStructure getBookingContact() {
        return bookingContact;
    }

    public void setBookingContact(ContactStructure value) {
        this.bookingContact = value;
    }

    public BookingArrangementsStructure getBookingArrangements() {
        return bookingArrangements;
    }

    public void setBookingArrangements(BookingArrangementsStructure value) {
        this.bookingArrangements = value;
    }

    public AllModesEnumeration getVehicleMode() {
        return vehicleMode;
    }

    public void setVehicleMode(AllModesEnumeration value) {
        this.vehicleMode = value;
    }

    public JAXBElement<? extends OrganisationRefStructure> getTransportOrganisationRef() {
        return transportOrganisationRef;
    }

    public void setTransportOrganisationRef(JAXBElement<? extends OrganisationRefStructure> value) {
        this.transportOrganisationRef = value;
    }

    public JAXBElement<? extends LineRefStructure> getLineRef() {
        return lineRef;
    }

    public void setLineRef(JAXBElement<? extends LineRefStructure> value) {
        this.lineRef = value;
    }

    public VersionOfObjectRefStructure getBookedObjectRef() {
        return bookedObjectRef;
    }

    public void setBookedObjectRef(VersionOfObjectRefStructure value) {
        this.bookedObjectRef = value;
    }

    public NoticeAssignments_RelStructure getNoticeAssignmenrts() {
        return noticeAssignmenrts;
    }

    public void setNoticeAssignmenrts(NoticeAssignments_RelStructure value) {
        this.noticeAssignmenrts = value;
    }

}
