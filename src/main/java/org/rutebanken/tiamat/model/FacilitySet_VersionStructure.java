//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a FACILITY.
 * 
 * <p>Java class for FacilitySet_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FacilitySet_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}DataManagedObjectStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}FacilitySetGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FacilitySet_VersionStructure", propOrder = {
    "providedByRef",
    "description",
    "typeOfFacilityRef",
    "otherFacilities",
    "accessibilityInfoFacilityList",
    "assistanceFacilityList",
    "accessibilityToolList",
    "carServiceFacilityList",
    "cateringFacilityList",
    "familyFacilityList",
    "fareClasses",
    "genderLimitation",
    "mealFacilityList",
    "medicalFacilityList",
    "mobilityFacilityList",
    "nuisanceFacilityList",
    "passengerCommsFacilityList",
    "passengerInformationEquipmentList",
    "passengerInformationFacilityList",
    "retailFacilityList",
    "safetyFacilityList",
    "sanitaryFacilityList",
    "ticketingFacilityList",
    "ticketingServiceFacilityList"
})
@XmlSeeAlso({
    ServiceFacilitySet_VersionStructure.class,
    SiteFacilitySetStructure.class
})
public class FacilitySet_VersionStructure
    extends DataManagedObjectStructure
{

    protected OrganisationRefStructure providedByRef;
    protected MultilingualStringEntity description;
    protected TypeOfFacilityRefStructure typeOfFacilityRef;
    protected TypesOfEquipment_RelStructure otherFacilities;
    @XmlSchemaType(name = "anySimpleType")
    protected List<AccessibilityInfoFacilityEnumeration> accessibilityInfoFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<AssistanceFacilityEnumeration> assistanceFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<AccessibilityToolEnumeration> accessibilityToolList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<CarServiceFacilityEnumeration> carServiceFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<CateringFacilityEnumeration> cateringFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<FamilyFacilityEnumeration> familyFacilityList;

    @XmlSchemaType(name = "normalizedString")
    protected GenderLimitationEnumeration genderLimitation;

    @XmlSchemaType(name = "anySimpleType")
    protected List<MealFacilityEnumeration> mealFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<MedicalFacilityEnumeration> medicalFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<MobilityFacilityEnumeration> mobilityFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<NuisanceFacilityEnumeration> nuisanceFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<PassengerCommsFacilityEnumeration> passengerCommsFacilityList;
    @XmlSchemaType(name = "NMTOKEN")
    protected PassengerInformationEquipmentEnumeration passengerInformationEquipmentList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<PassengerInformationFacilityEnumeration> passengerInformationFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<RetailFacilityEnumeration> retailFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<SafetyFacilityEnumeration> safetyFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<SanitaryFacilityEnumeration> sanitaryFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<TicketingFacilityEnumeration> ticketingFacilityList;
    @XmlSchemaType(name = "anySimpleType")
    protected List<TicketingServiceFacilityEnumeration> ticketingServiceFacilityList;

    /**
     * Gets the value of the providedByRef property.
     * 
     * @return
     *     possible object is
     *     {@link OrganisationRefStructure }
     *     
     */
    public OrganisationRefStructure getProvidedByRef() {
        return providedByRef;
    }

    /**
     * Sets the value of the providedByRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link OrganisationRefStructure }
     *     
     */
    public void setProvidedByRef(OrganisationRefStructure value) {
        this.providedByRef = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setDescription(MultilingualStringEntity value) {
        this.description = value;
    }

    /**
     * Gets the value of the typeOfFacilityRef property.
     * 
     * @return
     *     possible object is
     *     {@link TypeOfFacilityRefStructure }
     *     
     */
    public TypeOfFacilityRefStructure getTypeOfFacilityRef() {
        return typeOfFacilityRef;
    }

    /**
     * Sets the value of the typeOfFacilityRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeOfFacilityRefStructure }
     *     
     */
    public void setTypeOfFacilityRef(TypeOfFacilityRefStructure value) {
        this.typeOfFacilityRef = value;
    }

    /**
     * Gets the value of the otherFacilities property.
     * 
     * @return
     *     possible object is
     *     {@link TypesOfEquipment_RelStructure }
     *     
     */
    public TypesOfEquipment_RelStructure getOtherFacilities() {
        return otherFacilities;
    }

    /**
     * Sets the value of the otherFacilities property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypesOfEquipment_RelStructure }
     *     
     */
    public void setOtherFacilities(TypesOfEquipment_RelStructure value) {
        this.otherFacilities = value;
    }

    /**
     * List of ACCESSIBILITY INFORMATION FACILITies.Gets the value of the accessibilityInfoFacilityList property.
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

    /**
     * List of Couchette FACILITies.Gets the value of the assistanceFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the assistanceFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAssistanceFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AssistanceFacilityEnumeration }
     * 
     * 
     */
    public List<AssistanceFacilityEnumeration> getAssistanceFacilityList() {
        if (assistanceFacilityList == null) {
            assistanceFacilityList = new ArrayList<AssistanceFacilityEnumeration>();
        }
        return this.assistanceFacilityList;
    }

    /**
     * List of TYPEs of ACCESSIBILITY TOOLs.Gets the value of the accessibilityToolList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accessibilityToolList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccessibilityToolList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AccessibilityToolEnumeration }
     * 
     * 
     */
    public List<AccessibilityToolEnumeration> getAccessibilityToolList() {
        if (accessibilityToolList == null) {
            accessibilityToolList = new ArrayList<AccessibilityToolEnumeration>();
        }
        return this.accessibilityToolList;
    }

    /**
     * Gets the value of the carServiceFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the carServiceFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCarServiceFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CarServiceFacilityEnumeration }
     * 
     * 
     */
    public List<CarServiceFacilityEnumeration> getCarServiceFacilityList() {
        if (carServiceFacilityList == null) {
            carServiceFacilityList = new ArrayList<CarServiceFacilityEnumeration>();
        }
        return this.carServiceFacilityList;
    }

    /**
     * Gets the value of the cateringFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cateringFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCateringFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CateringFacilityEnumeration }
     * 
     * 
     */
    public List<CateringFacilityEnumeration> getCateringFacilityList() {
        if (cateringFacilityList == null) {
            cateringFacilityList = new ArrayList<CateringFacilityEnumeration>();
        }
        return this.cateringFacilityList;
    }

    /**
     * Gets the value of the familyFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the familyFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFamilyFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FamilyFacilityEnumeration }
     * 
     * 
     */
    public List<FamilyFacilityEnumeration> getFamilyFacilityList() {
        if (familyFacilityList == null) {
            familyFacilityList = new ArrayList<FamilyFacilityEnumeration>();
        }
        return this.familyFacilityList;
    }

    /**
     * Gets the value of the genderLimitation property.
     * 
     * @return
     *     possible object is
     *     {@link GenderLimitationEnumeration }
     *     
     */
    public GenderLimitationEnumeration getGenderLimitation() {
        return genderLimitation;
    }

    /**
     * Sets the value of the genderLimitation property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenderLimitationEnumeration }
     *     
     */
    public void setGenderLimitation(GenderLimitationEnumeration value) {
        this.genderLimitation = value;
    }

    /**
     * Gets the value of the mealFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mealFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMealFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MealFacilityEnumeration }
     * 
     * 
     */
    public List<MealFacilityEnumeration> getMealFacilityList() {
        if (mealFacilityList == null) {
            mealFacilityList = new ArrayList<MealFacilityEnumeration>();
        }
        return this.mealFacilityList;
    }

    /**
     * Gets the value of the medicalFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the medicalFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMedicalFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MedicalFacilityEnumeration }
     * 
     * 
     */
    public List<MedicalFacilityEnumeration> getMedicalFacilityList() {
        if (medicalFacilityList == null) {
            medicalFacilityList = new ArrayList<MedicalFacilityEnumeration>();
        }
        return this.medicalFacilityList;
    }

    /**
     * Gets the value of the mobilityFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mobilityFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMobilityFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MobilityFacilityEnumeration }
     * 
     * 
     */
    public List<MobilityFacilityEnumeration> getMobilityFacilityList() {
        if (mobilityFacilityList == null) {
            mobilityFacilityList = new ArrayList<MobilityFacilityEnumeration>();
        }
        return this.mobilityFacilityList;
    }

    /**
     * Gets the value of the nuisanceFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the nuisanceFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNuisanceFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link NuisanceFacilityEnumeration }
     * 
     * 
     */
    public List<NuisanceFacilityEnumeration> getNuisanceFacilityList() {
        if (nuisanceFacilityList == null) {
            nuisanceFacilityList = new ArrayList<NuisanceFacilityEnumeration>();
        }
        return this.nuisanceFacilityList;
    }

    /**
     * Gets the value of the passengerCommsFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the passengerCommsFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPassengerCommsFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PassengerCommsFacilityEnumeration }
     * 
     * 
     */
    public List<PassengerCommsFacilityEnumeration> getPassengerCommsFacilityList() {
        if (passengerCommsFacilityList == null) {
            passengerCommsFacilityList = new ArrayList<PassengerCommsFacilityEnumeration>();
        }
        return this.passengerCommsFacilityList;
    }

    /**
     * Gets the value of the passengerInformationEquipmentList property.
     * 
     * @return
     *     possible object is
     *     {@link PassengerInformationEquipmentEnumeration }
     *     
     */
    public PassengerInformationEquipmentEnumeration getPassengerInformationEquipmentList() {
        return passengerInformationEquipmentList;
    }

    /**
     * Sets the value of the passengerInformationEquipmentList property.
     * 
     * @param value
     *     allowed object is
     *     {@link PassengerInformationEquipmentEnumeration }
     *     
     */
    public void setPassengerInformationEquipmentList(PassengerInformationEquipmentEnumeration value) {
        this.passengerInformationEquipmentList = value;
    }

    /**
     * List of PASSENGER INFORMATION FACILITies.Gets the value of the passengerInformationFacilityList property.
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
     * Gets the value of the retailFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the retailFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRetailFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RetailFacilityEnumeration }
     * 
     * 
     */
    public List<RetailFacilityEnumeration> getRetailFacilityList() {
        if (retailFacilityList == null) {
            retailFacilityList = new ArrayList<RetailFacilityEnumeration>();
        }
        return this.retailFacilityList;
    }

    /**
     * Gets the value of the safetyFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the safetyFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSafetyFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SafetyFacilityEnumeration }
     * 
     * 
     */
    public List<SafetyFacilityEnumeration> getSafetyFacilityList() {
        if (safetyFacilityList == null) {
            safetyFacilityList = new ArrayList<SafetyFacilityEnumeration>();
        }
        return this.safetyFacilityList;
    }

    /**
     * Gets the value of the sanitaryFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sanitaryFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSanitaryFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SanitaryFacilityEnumeration }
     * 
     * 
     */
    public List<SanitaryFacilityEnumeration> getSanitaryFacilityList() {
        if (sanitaryFacilityList == null) {
            sanitaryFacilityList = new ArrayList<SanitaryFacilityEnumeration>();
        }
        return this.sanitaryFacilityList;
    }

    /**
     * Gets the value of the ticketingFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ticketingFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTicketingFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TicketingFacilityEnumeration }
     * 
     * 
     */
    public List<TicketingFacilityEnumeration> getTicketingFacilityList() {
        if (ticketingFacilityList == null) {
            ticketingFacilityList = new ArrayList<TicketingFacilityEnumeration>();
        }
        return this.ticketingFacilityList;
    }

    /**
     * Gets the value of the ticketingServiceFacilityList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ticketingServiceFacilityList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTicketingServiceFacilityList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TicketingServiceFacilityEnumeration }
     * 
     * 
     */
    public List<TicketingServiceFacilityEnumeration> getTicketingServiceFacilityList() {
        if (ticketingServiceFacilityList == null) {
            ticketingServiceFacilityList = new ArrayList<TicketingServiceFacilityEnumeration>();
        }
        return this.ticketingServiceFacilityList;
    }

}
