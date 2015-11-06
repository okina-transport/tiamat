//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for an ACCESSIBILITY LIMITATION.
 * 
 * <p>Java class for AccessibilityLimitation_VersionedChildStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AccessibilityLimitation_VersionedChildStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}VersionedChildStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}MobilityLimitationGroup"/>
 *         &lt;group ref="{http://www.netex.org.uk/netex}SensoryLimitationGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AccessibilityLimitation_VersionedChildStructure", propOrder = {
    "wheelchairAccess",
    "stepFreeAccess",
    "escalatorFreeAccess",
    "liftFreeAccess",
    "audibleSignalsAvailable",
    "visualSignsAvailable"
})
@XmlSeeAlso({
    AccessibilityLimitation.class
})
public class AccessibilityLimitation_VersionedChildStructure
    extends VersionedChildStructure
{

    @XmlElement(name = "WheelchairAccess", required = true, defaultValue = "false")
    @XmlSchemaType(name = "string")
    protected LimitationStatusEnumeration wheelchairAccess;
    @XmlElement(name = "StepFreeAccess", defaultValue = "unknown")
    @XmlSchemaType(name = "string")
    protected LimitationStatusEnumeration stepFreeAccess;
    @XmlElement(name = "EscalatorFreeAccess", defaultValue = "unknown")
    @XmlSchemaType(name = "string")
    protected LimitationStatusEnumeration escalatorFreeAccess;
    @XmlElement(name = "LiftFreeAccess", defaultValue = "unknown")
    @XmlSchemaType(name = "string")
    protected LimitationStatusEnumeration liftFreeAccess;
    @XmlElement(name = "AudibleSignalsAvailable", defaultValue = "false")
    @XmlSchemaType(name = "string")
    protected LimitationStatusEnumeration audibleSignalsAvailable;
    @XmlElement(name = "VisualSignsAvailable", defaultValue = "unknown")
    @XmlSchemaType(name = "string")
    protected LimitationStatusEnumeration visualSignsAvailable;

    /**
     * Gets the value of the wheelchairAccess property.
     * 
     * @return
     *     possible object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public LimitationStatusEnumeration getWheelchairAccess() {
        return wheelchairAccess;
    }

    /**
     * Sets the value of the wheelchairAccess property.
     * 
     * @param value
     *     allowed object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public void setWheelchairAccess(LimitationStatusEnumeration value) {
        this.wheelchairAccess = value;
    }

    /**
     * Gets the value of the stepFreeAccess property.
     * 
     * @return
     *     possible object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public LimitationStatusEnumeration getStepFreeAccess() {
        return stepFreeAccess;
    }

    /**
     * Sets the value of the stepFreeAccess property.
     * 
     * @param value
     *     allowed object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public void setStepFreeAccess(LimitationStatusEnumeration value) {
        this.stepFreeAccess = value;
    }

    /**
     * Gets the value of the escalatorFreeAccess property.
     * 
     * @return
     *     possible object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public LimitationStatusEnumeration getEscalatorFreeAccess() {
        return escalatorFreeAccess;
    }

    /**
     * Sets the value of the escalatorFreeAccess property.
     * 
     * @param value
     *     allowed object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public void setEscalatorFreeAccess(LimitationStatusEnumeration value) {
        this.escalatorFreeAccess = value;
    }

    /**
     * Gets the value of the liftFreeAccess property.
     * 
     * @return
     *     possible object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public LimitationStatusEnumeration getLiftFreeAccess() {
        return liftFreeAccess;
    }

    /**
     * Sets the value of the liftFreeAccess property.
     * 
     * @param value
     *     allowed object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public void setLiftFreeAccess(LimitationStatusEnumeration value) {
        this.liftFreeAccess = value;
    }

    /**
     * Whether a PLACE has audible signals for the visually impaired.
     * 
     * @return
     *     possible object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public LimitationStatusEnumeration getAudibleSignalsAvailable() {
        return audibleSignalsAvailable;
    }

    /**
     * Sets the value of the audibleSignalsAvailable property.
     * 
     * @param value
     *     allowed object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public void setAudibleSignalsAvailable(LimitationStatusEnumeration value) {
        this.audibleSignalsAvailable = value;
    }

    /**
     * Whether a PLACE has visual signals for the hearing impaired.
     * 
     * @return
     *     possible object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public LimitationStatusEnumeration getVisualSignsAvailable() {
        return visualSignsAvailable;
    }

    /**
     * Sets the value of the visualSignsAvailable property.
     * 
     * @param value
     *     allowed object is
     *     {@link LimitationStatusEnumeration }
     *     
     */
    public void setVisualSignsAvailable(LimitationStatusEnumeration value) {
        this.visualSignsAvailable = value;
    }

}
