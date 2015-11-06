//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a PASSENGER SAFETY EQUIPMENT.
 * 
 * <p>Java class for PassengerSafetyEquipment_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PassengerSafetyEquipment_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}PassengerEquipment_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}PassengerSafetyEquipmentGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PassengerSafetyEquipment_VersionStructure", propOrder = {
    "cctv",
    "mobilePhoneCoverage",
    "panicButton",
    "sosPhones",
    "heightOfSosPanel",
    "lighting",
    "acousticAnnouncements"
})
@XmlSeeAlso({
    PassengerSafetyEquipment.class
})
public class PassengerSafetyEquipment_VersionStructure
    extends PassengerEquipment_VersionStructure
{

    @XmlElement(name = "Cctv")
    protected Boolean cctv;
    @XmlElement(name = "MobilePhoneCoverage")
    protected Boolean mobilePhoneCoverage;
    @XmlElement(name = "PanicButton")
    protected Boolean panicButton;
    @XmlElement(name = "SosPhones")
    protected Boolean sosPhones;
    @XmlElement(name = "HeightOfSosPanel")
    protected BigDecimal heightOfSosPanel;
    @XmlElement(name = "Lighting")
    @XmlSchemaType(name = "normalizedString")
    protected LightingEnumeration lighting;
    @XmlElement(name = "AcousticAnnouncements")
    protected Boolean acousticAnnouncements;

    /**
     * Gets the value of the cctv property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isCctv() {
        return cctv;
    }

    /**
     * Sets the value of the cctv property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCctv(Boolean value) {
        this.cctv = value;
    }

    /**
     * Gets the value of the mobilePhoneCoverage property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isMobilePhoneCoverage() {
        return mobilePhoneCoverage;
    }

    /**
     * Sets the value of the mobilePhoneCoverage property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setMobilePhoneCoverage(Boolean value) {
        this.mobilePhoneCoverage = value;
    }

    /**
     * Gets the value of the panicButton property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isPanicButton() {
        return panicButton;
    }

    /**
     * Sets the value of the panicButton property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPanicButton(Boolean value) {
        this.panicButton = value;
    }

    /**
     * Gets the value of the sosPhones property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isSosPhones() {
        return sosPhones;
    }

    /**
     * Sets the value of the sosPhones property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSosPhones(Boolean value) {
        this.sosPhones = value;
    }

    /**
     * Gets the value of the heightOfSosPanel property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getHeightOfSosPanel() {
        return heightOfSosPanel;
    }

    /**
     * Sets the value of the heightOfSosPanel property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setHeightOfSosPanel(BigDecimal value) {
        this.heightOfSosPanel = value;
    }

    /**
     * Gets the value of the lighting property.
     * 
     * @return
     *     possible object is
     *     {@link LightingEnumeration }
     *     
     */
    public LightingEnumeration getLighting() {
        return lighting;
    }

    /**
     * Sets the value of the lighting property.
     * 
     * @param value
     *     allowed object is
     *     {@link LightingEnumeration }
     *     
     */
    public void setLighting(LightingEnumeration value) {
        this.lighting = value;
    }

    /**
     * Gets the value of the acousticAnnouncements property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAcousticAnnouncements() {
        return acousticAnnouncements;
    }

    /**
     * Sets the value of the acousticAnnouncements property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAcousticAnnouncements(Boolean value) {
        this.acousticAnnouncements = value;
    }

}
