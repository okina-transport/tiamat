//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a LIFT EQUIPMENT.
 * 
 * <p>Java class for LiftEquipment_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LiftEquipment_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}AccessEquipment_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}LiftEquipmentGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LiftEquipment_VersionStructure", propOrder = {
    "depth",
    "maximumLoad",
    "wheelchairPasssable",
    "wheelchairTurningCircle",
    "internalWidth",
    "handrailType",
    "handrailHeight",
    "lowerHandrailHeight",
    "callButtonHeight",
    "directionButtonHeight",
    "raisedButtons",
    "brailleButtons",
    "throughLoader",
    "mirrorOnOppositeSide",
    "attendant",
    "automatic",
    "alarmButton",
    "tactileActuators",
    "accousticAnnouncements",
    "signageToLift",
    "suitableForCycles"
})
@XmlSeeAlso({
    LiftEquipment.class
})
public class LiftEquipment_VersionStructure
    extends AccessEquipment_VersionStructure
{

    protected BigDecimal depth;
    protected BigDecimal maximumLoad;
    protected Boolean wheelchairPasssable;
    protected BigDecimal wheelchairTurningCircle;
    protected BigDecimal internalWidth;
    @XmlSchemaType(name = "string")
    protected HandrailEnumeration handrailType;
    protected BigDecimal handrailHeight;
    protected BigDecimal lowerHandrailHeight;
    protected BigDecimal callButtonHeight;
    protected BigDecimal directionButtonHeight;
    protected Boolean raisedButtons;
    protected Boolean brailleButtons;
    protected Boolean throughLoader;
    protected Boolean mirrorOnOppositeSide;
    protected Boolean attendant;
    protected Boolean automatic;
    protected Boolean alarmButton;
    protected Boolean tactileActuators;
    protected Boolean accousticAnnouncements;
    protected Boolean signageToLift;
    protected Boolean suitableForCycles;

    /**
     * Gets the value of the depth property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getDepth() {
        return depth;
    }

    /**
     * Sets the value of the depth property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setDepth(BigDecimal value) {
        this.depth = value;
    }

    /**
     * Gets the value of the maximumLoad property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getMaximumLoad() {
        return maximumLoad;
    }

    /**
     * Sets the value of the maximumLoad property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setMaximumLoad(BigDecimal value) {
        this.maximumLoad = value;
    }

    /**
     * Gets the value of the wheelchairPasssable property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isWheelchairPasssable() {
        return wheelchairPasssable;
    }

    /**
     * Sets the value of the wheelchairPasssable property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWheelchairPasssable(Boolean value) {
        this.wheelchairPasssable = value;
    }

    /**
     * Gets the value of the wheelchairTurningCircle property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getWheelchairTurningCircle() {
        return wheelchairTurningCircle;
    }

    /**
     * Sets the value of the wheelchairTurningCircle property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setWheelchairTurningCircle(BigDecimal value) {
        this.wheelchairTurningCircle = value;
    }

    /**
     * Gets the value of the internalWidth property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getInternalWidth() {
        return internalWidth;
    }

    /**
     * Sets the value of the internalWidth property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setInternalWidth(BigDecimal value) {
        this.internalWidth = value;
    }

    /**
     * Gets the value of the handrailType property.
     * 
     * @return
     *     possible object is
     *     {@link HandrailEnumeration }
     *     
     */
    public HandrailEnumeration getHandrailType() {
        return handrailType;
    }

    /**
     * Sets the value of the handrailType property.
     * 
     * @param value
     *     allowed object is
     *     {@link HandrailEnumeration }
     *     
     */
    public void setHandrailType(HandrailEnumeration value) {
        this.handrailType = value;
    }

    /**
     * Gets the value of the handrailHeight property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getHandrailHeight() {
        return handrailHeight;
    }

    /**
     * Sets the value of the handrailHeight property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setHandrailHeight(BigDecimal value) {
        this.handrailHeight = value;
    }

    /**
     * Gets the value of the lowerHandrailHeight property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getLowerHandrailHeight() {
        return lowerHandrailHeight;
    }

    /**
     * Sets the value of the lowerHandrailHeight property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setLowerHandrailHeight(BigDecimal value) {
        this.lowerHandrailHeight = value;
    }

    /**
     * Gets the value of the callButtonHeight property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getCallButtonHeight() {
        return callButtonHeight;
    }

    /**
     * Sets the value of the callButtonHeight property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setCallButtonHeight(BigDecimal value) {
        this.callButtonHeight = value;
    }

    /**
     * Gets the value of the directionButtonHeight property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getDirectionButtonHeight() {
        return directionButtonHeight;
    }

    /**
     * Sets the value of the directionButtonHeight property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setDirectionButtonHeight(BigDecimal value) {
        this.directionButtonHeight = value;
    }

    /**
     * Gets the value of the raisedButtons property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isRaisedButtons() {
        return raisedButtons;
    }

    /**
     * Sets the value of the raisedButtons property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRaisedButtons(Boolean value) {
        this.raisedButtons = value;
    }

    /**
     * Gets the value of the brailleButtons property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBrailleButtons() {
        return brailleButtons;
    }

    /**
     * Sets the value of the brailleButtons property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBrailleButtons(Boolean value) {
        this.brailleButtons = value;
    }

    /**
     * Gets the value of the throughLoader property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isThroughLoader() {
        return throughLoader;
    }

    /**
     * Sets the value of the throughLoader property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setThroughLoader(Boolean value) {
        this.throughLoader = value;
    }

    /**
     * Gets the value of the mirrorOnOppositeSide property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isMirrorOnOppositeSide() {
        return mirrorOnOppositeSide;
    }

    /**
     * Sets the value of the mirrorOnOppositeSide property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setMirrorOnOppositeSide(Boolean value) {
        this.mirrorOnOppositeSide = value;
    }

    /**
     * Gets the value of the attendant property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAttendant() {
        return attendant;
    }

    /**
     * Sets the value of the attendant property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAttendant(Boolean value) {
        this.attendant = value;
    }

    /**
     * Gets the value of the automatic property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAutomatic() {
        return automatic;
    }

    /**
     * Sets the value of the automatic property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAutomatic(Boolean value) {
        this.automatic = value;
    }

    /**
     * Gets the value of the alarmButton property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAlarmButton() {
        return alarmButton;
    }

    /**
     * Sets the value of the alarmButton property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAlarmButton(Boolean value) {
        this.alarmButton = value;
    }

    /**
     * Gets the value of the tactileActuators property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isTactileActuators() {
        return tactileActuators;
    }

    /**
     * Sets the value of the tactileActuators property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setTactileActuators(Boolean value) {
        this.tactileActuators = value;
    }

    /**
     * Gets the value of the accousticAnnouncements property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAccousticAnnouncements() {
        return accousticAnnouncements;
    }

    /**
     * Sets the value of the accousticAnnouncements property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAccousticAnnouncements(Boolean value) {
        this.accousticAnnouncements = value;
    }

    /**
     * Gets the value of the signageToLift property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isSignageToLift() {
        return signageToLift;
    }

    /**
     * Sets the value of the signageToLift property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSignageToLift(Boolean value) {
        this.signageToLift = value;
    }

    /**
     * Gets the value of the suitableForCycles property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isSuitableForCycles() {
        return suitableForCycles;
    }

    /**
     * Sets the value of the suitableForCycles property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSuitableForCycles(Boolean value) {
        this.suitableForCycles = value;
    }

}
