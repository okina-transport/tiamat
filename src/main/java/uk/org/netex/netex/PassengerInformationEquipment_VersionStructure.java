//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.06 at 10:37:32 AM CET 
//


package uk.org.netex.netex;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a PASSENGER INFORMATION EQUIPMENT.
 * 
 * <p>Java class for PassengerInformationEquipment_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PassengerInformationEquipment_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}PassengerEquipment_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}PassengerInformationEquipmentGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PassengerInformationEquipment_VersionStructure", propOrder = {
    "logicalDisplayRef",
    "stopPlaceRef",
    "siteComponentRef",
    "typeOfPassengerInformationEquipmentRef",
    "passengerInformationFacilityList",
    "accessibilityInfoFacilityList"
})
@XmlSeeAlso({
    PassengerInformationEquipment.class
})
public class PassengerInformationEquipment_VersionStructure
    extends PassengerEquipment_VersionStructure
{

    @XmlElement(name = "LogicalDisplayRef")
    protected LogicalDisplayRefStructure logicalDisplayRef;
    @XmlElement(name = "StopPlaceRef")
    protected StopPlaceReference stopPlaceRef;
    @XmlElementRef(name = "SiteComponentRef", namespace = "http://www.netex.org.uk/netex", type = JAXBElement.class, required = false)
    protected JAXBElement<? extends SiteComponentRefStructure> siteComponentRef;
    @XmlElement(name = "TypeOfPassengerInformationEquipmentRef")
    protected TypeOfPassengerInformationEquipmentRefStructure typeOfPassengerInformationEquipmentRef;
    @XmlList
    @XmlElement(name = "PassengerInformationFacilityList")
    @XmlSchemaType(name = "anySimpleType")
    protected List<PassengerInformationFacilityEnumeration> passengerInformationFacilityList;
    @XmlList
    @XmlElement(name = "AccessibilityInfoFacilityList")
    @XmlSchemaType(name = "anySimpleType")
    protected List<AccessibilityInfoFacilityEnumeration> accessibilityInfoFacilityList;

    /**
     * Gets the value of the logicalDisplayRef property.
     * 
     * @return
     *     possible object is
     *     {@link LogicalDisplayRefStructure }
     *     
     */
    public LogicalDisplayRefStructure getLogicalDisplayRef() {
        return logicalDisplayRef;
    }

    /**
     * Sets the value of the logicalDisplayRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link LogicalDisplayRefStructure }
     *     
     */
    public void setLogicalDisplayRef(LogicalDisplayRefStructure value) {
        this.logicalDisplayRef = value;
    }

    /**
     * Gets the value of the stopPlaceRef property.
     * 
     * @return
     *     possible object is
     *     {@link StopPlaceReference }
     *     
     */
    public StopPlaceReference getStopPlaceRef() {
        return stopPlaceRef;
    }

    /**
     * Sets the value of the stopPlaceRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link StopPlaceReference }
     *     
     */
    public void setStopPlaceRef(StopPlaceReference value) {
        this.stopPlaceRef = value;
    }

    /**
     * Gets the value of the siteComponentRef property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link VehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link VehicleStoppingPositionRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link BoardingPositionRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingBayRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link AccessSpaceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingEntranceForVehiclesRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link SiteComponentRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link QuayRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceSpaceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestVehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceVehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link EntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestSpaceRefStructure }{@code >}
     *     
     */
    public JAXBElement<? extends SiteComponentRefStructure> getSiteComponentRef() {
        return siteComponentRef;
    }

    /**
     * Sets the value of the siteComponentRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link VehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link VehicleStoppingPositionRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link BoardingPositionRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingBayRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link AccessSpaceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingEntranceForVehiclesRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link SiteComponentRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link QuayRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceSpaceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestVehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceVehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link EntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestSpaceRefStructure }{@code >}
     *     
     */
    public void setSiteComponentRef(JAXBElement<? extends SiteComponentRefStructure> value) {
        this.siteComponentRef = value;
    }

    /**
     * Gets the value of the typeOfPassengerInformationEquipmentRef property.
     * 
     * @return
     *     possible object is
     *     {@link TypeOfPassengerInformationEquipmentRefStructure }
     *     
     */
    public TypeOfPassengerInformationEquipmentRefStructure getTypeOfPassengerInformationEquipmentRef() {
        return typeOfPassengerInformationEquipmentRef;
    }

    /**
     * Sets the value of the typeOfPassengerInformationEquipmentRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeOfPassengerInformationEquipmentRefStructure }
     *     
     */
    public void setTypeOfPassengerInformationEquipmentRef(TypeOfPassengerInformationEquipmentRefStructure value) {
        this.typeOfPassengerInformationEquipmentRef = value;
    }

    /**
     * List of predefined Passenger Info EQUIPMENT f.Gets the value of the passengerInformationFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the passengerInformationFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPassengerInformationFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PassengerInformationFacilityEnumeration }
     * 
     * 
     */
    public List<PassengerInformationFacilityEnumeration> getPassengerInformationFacilityList() {
        if (passengerInformationFacilityList == null) {
            passengerInformationFacilityList = new ArrayList<PassengerInformationFacilityEnumeration>();
        }
        return this.passengerInformationFacilityList;
    }

    /**
     * Gets the value of the accessibilityInfoFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accessibilityInfoFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccessibilityInfoFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AccessibilityInfoFacilityEnumeration }
     * 
     * 
     */
    public List<AccessibilityInfoFacilityEnumeration> getAccessibilityInfoFacilityList() {
        if (accessibilityInfoFacilityList == null) {
            accessibilityInfoFacilityList = new ArrayList<AccessibilityInfoFacilityEnumeration>();
        }
        return this.accessibilityInfoFacilityList;
    }

}
