//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;


/**
 * Type for TIMING POINT.
 * 
 * <p>Java class for TimingPoint_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TimingPoint_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}Point_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}TimingPointGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TimingPoint_VersionStructure", propOrder = {
    "timingPointStatus",
    "allowedForWaitTime"
})
@XmlSeeAlso({
    TimingPoint.class,
    ScheduledStopPoint_VersionStructure.class,
    BorderPoint_ValueStructure.class,
    ReliefPoint_VersionStructure.class
})
public class TimingPoint_VersionStructure
    extends Point_VersionStructure
{

    @XmlSchemaType(name = "normalizedString")
    protected TimingPointStatusEnumeration timingPointStatus;
    protected Duration allowedForWaitTime;

    /**
     * Gets the value of the timingPointStatus property.
     * 
     * @return
     *     possible object is
     *     {@link TimingPointStatusEnumeration }
     *     
     */
    public TimingPointStatusEnumeration getTimingPointStatus() {
        return timingPointStatus;
    }

    /**
     * Sets the value of the timingPointStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link TimingPointStatusEnumeration }
     *     
     */
    public void setTimingPointStatus(TimingPointStatusEnumeration value) {
        this.timingPointStatus = value;
    }

    /**
     * Gets the value of the allowedForWaitTime property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getAllowedForWaitTime() {
        return allowedForWaitTime;
    }

    /**
     * Sets the value of the allowedForWaitTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setAllowedForWaitTime(Duration value) {
        this.allowedForWaitTime = value;
    }

}
