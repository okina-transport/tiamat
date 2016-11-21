//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a NETWORK.
 * 
 * <p>Java class for Network_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Network_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}GroupOfLines_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}NetworkGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Network_VersionStructure", propOrder = {
    "transportOrganisationRef",
    "groupsOfLines"
})
@XmlSeeAlso({
    Network.class
})
public class Network_VersionStructure
    extends GroupOfLines_VersionStructure
{

    protected JAXBElement<? extends OrganisationRefStructure> transportOrganisationRef;
    protected GroupsOfLinesInFrame_RelStructure groupsOfLines;

    /**
     * Gets the value of the transportOrganisationRef property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link AuthorityRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link OrganisationRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link OperatorRefStructure }{@code >}
     *     
     */
    public JAXBElement<? extends OrganisationRefStructure> getTransportOrganisationRef() {
        return transportOrganisationRef;
    }

    /**
     * Sets the value of the transportOrganisationRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link AuthorityRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link OrganisationRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link OperatorRefStructure }{@code >}
     *     
     */
    public void setTransportOrganisationRef(JAXBElement<? extends OrganisationRefStructure> value) {
        this.transportOrganisationRef = value;
    }

    /**
     * Gets the value of the groupsOfLines property.
     * 
     * @return
     *     possible object is
     *     {@link GroupsOfLinesInFrame_RelStructure }
     *     
     */
    public GroupsOfLinesInFrame_RelStructure getGroupsOfLines() {
        return groupsOfLines;
    }

    /**
     * Sets the value of the groupsOfLines property.
     * 
     * @param value
     *     allowed object is
     *     {@link GroupsOfLinesInFrame_RelStructure }
     *     
     */
    public void setGroupsOfLines(GroupsOfLinesInFrame_RelStructure value) {
        this.groupsOfLines = value;
    }

}
