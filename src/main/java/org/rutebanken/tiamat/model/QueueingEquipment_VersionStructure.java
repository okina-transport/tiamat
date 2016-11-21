//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a QUEUEING EQUIPMENT.
 * 
 * <p>Java class for QueueingEquipment_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueueingEquipment_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}AccessEquipment_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}QueueingEquipmentGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueueingEquipment_VersionStructure", propOrder = {
    "numberOfServers",
    "railedQueue",
    "ticketedQueue"
})
@XmlSeeAlso({
    QueueingEquipment.class
})
public class QueueingEquipment_VersionStructure
    extends AccessEquipment_VersionStructure
{

    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger numberOfServers;
    protected Boolean railedQueue;
    protected Boolean ticketedQueue;

    /**
     * Gets the value of the numberOfServers property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getNumberOfServers() {
        return numberOfServers;
    }

    /**
     * Sets the value of the numberOfServers property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setNumberOfServers(BigInteger value) {
        this.numberOfServers = value;
    }

    /**
     * Gets the value of the railedQueue property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isRailedQueue() {
        return railedQueue;
    }

    /**
     * Sets the value of the railedQueue property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRailedQueue(Boolean value) {
        this.railedQueue = value;
    }

    /**
     * Gets the value of the ticketedQueue property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isTicketedQueue() {
        return ticketedQueue;
    }

    /**
     * Sets the value of the ticketedQueue property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setTicketedQueue(Boolean value) {
        this.ticketedQueue = value;
    }

}
