//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a Member of COMMON SECTION.
 * 
 * <p>Java class for CommonSectionPointMember_VersionedChildStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CommonSectionPointMember_VersionedChildStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}AbstractGroupMember_VersionedChildStructure">
 *       &lt;sequence>
 *         &lt;element name="ParentCommonSectionRef" type="{http://www.netex.org.uk/netex}CommonSectionRefStructure" minOccurs="0"/>
 *         &lt;element ref="{http://www.netex.org.uk/netex}PointRef"/>
 *         &lt;element ref="{http://www.netex.org.uk/netex}LinkRef" minOccurs="0"/>
 *         &lt;element name="Reverse" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CommonSectionPointMember_VersionedChildStructure", propOrder = {
    "parentCommonSectionRef",
    "pointRef",
    "linkRef",
    "reverse"
})
@XmlSeeAlso({
    LineSectionPointMember_VersionStructure.class
})
public class CommonSectionPointMember_VersionedChildStructure
    extends AbstractGroupMember_VersionedChildStructure
{

    @XmlElement(name = "ParentCommonSectionRef")
    protected CommonSectionRefStructure parentCommonSectionRef;
    @XmlElementRef(name = "PointRef", namespace = "http://www.netex.org.uk/netex", type = JAXBElement.class)
    protected JAXBElement<? extends PointRefStructure> pointRef;
    @XmlElementRef(name = "LinkRef", namespace = "http://www.netex.org.uk/netex", type = JAXBElement.class, required = false)
    protected JAXBElement<? extends LinkRefStructure> linkRef;
    @XmlElement(name = "Reverse")
    protected Boolean reverse;

    /**
     * Gets the value of the parentCommonSectionRef property.
     * 
     * @return
     *     possible object is
     *     {@link CommonSectionRefStructure }
     *     
     */
    public CommonSectionRefStructure getParentCommonSectionRef() {
        return parentCommonSectionRef;
    }

    /**
     * Sets the value of the parentCommonSectionRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link CommonSectionRefStructure }
     *     
     */
    public void setParentCommonSectionRef(CommonSectionRefStructure value) {
        this.parentCommonSectionRef = value;
    }

    /**
     * Reference to a POINT.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link RailwayPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link InfrastructurePointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ScheduledStopPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link RoadPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link FareScheduledStopPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link RoutePointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ActivationPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOnLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link BorderPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link TrafficControlPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link GaragePointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link WirePointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link TimingPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link BeaconPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ReliefPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointRefStructure }{@code >}
     *     
     */
    public JAXBElement<? extends PointRefStructure> getPointRef() {
        return pointRef;
    }

    /**
     * Sets the value of the pointRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link RailwayPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link InfrastructurePointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ScheduledStopPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link RoadPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link FareScheduledStopPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link RoutePointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ActivationPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOnLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link BorderPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link TrafficControlPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link GaragePointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link WirePointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link TimingPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link BeaconPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ReliefPointRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointRefStructure }{@code >}
     *     
     */
    public void setPointRef(JAXBElement<? extends PointRefStructure> value) {
        this.pointRef = value;
    }

    /**
     * Reference to a LINK.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link TimingLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link RouteLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ActivationLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link RailwayLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link RoadLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link InfrastructureLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link LinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link WireLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ServiceLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link LineLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PathLinkRefStructure }{@code >}
     *     
     */
    public JAXBElement<? extends LinkRefStructure> getLinkRef() {
        return linkRef;
    }

    /**
     * Sets the value of the linkRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link TimingLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link RouteLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ActivationLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link RailwayLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link RoadLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link InfrastructureLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link LinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link WireLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ServiceLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link LineLinkRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PathLinkRefStructure }{@code >}
     *     
     */
    public void setLinkRef(JAXBElement<? extends LinkRefStructure> value) {
        this.linkRef = value;
    }

    /**
     * Gets the value of the reverse property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isReverse() {
        return reverse;
    }

    /**
     * Sets the value of the reverse property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setReverse(Boolean value) {
        this.reverse = value;
    }

}
