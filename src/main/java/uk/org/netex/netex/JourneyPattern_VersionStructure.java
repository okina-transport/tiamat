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
 * Type for JOURNEY PATTERN.
 * 
 * <p>Java class for JourneyPattern_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="JourneyPattern_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}LinkSequence_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}JourneyPatternGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "JourneyPattern_VersionStructure", propOrder = {
    "routeRef",
    "routeView",
    "directionType",
    "directionRef",
    "directionView",
    "destinationDisplayRef",
    "destinationDisplayView",
    "typeOfJourneyPatternRef",
    "operationalContextRef",
    "timingPatternRef",
    "runTimes",
    "waitTimes",
    "headways",
    "layovers",
    "pointsInSequence",
    "linksInSequence"
})
@XmlSeeAlso({
    JourneyPattern.class,
    DeadRunJourneyPattern_VersionStructure.class,
    ServiceJourneyPattern_VersionStructure.class
})
public abstract class JourneyPattern_VersionStructure
    extends LinkSequence_VersionStructure
{

    @XmlElement(name = "RouteRef")
    protected RouteRefStructure routeRef;
    @XmlElement(name = "RouteView")
    protected RouteView routeView;
    @XmlElement(name = "DirectionType")
    @XmlSchemaType(name = "normalizedString")
    protected DirectionTypeEnumeration directionType;
    @XmlElement(name = "DirectionRef")
    protected DirectionRefStructure directionRef;
    @XmlElement(name = "DirectionView")
    protected DirectionView directionView;
    @XmlElement(name = "DestinationDisplayRef")
    protected DestinationDisplayRefStructure destinationDisplayRef;
    @XmlElement(name = "DestinationDisplayView")
    protected DestinationDisplayView destinationDisplayView;
    @XmlElement(name = "TypeOfJourneyPatternRef")
    protected TypeOfJourneyPatternRefStructure typeOfJourneyPatternRef;
    @XmlElement(name = "OperationalContextRef")
    protected OperationalContextRefStructure operationalContextRef;
    @XmlElement(name = "TimingPatternRef")
    protected TimingPatternRefStructure timingPatternRef;
    protected JourneyPatternRunTimes_RelStructure runTimes;
    protected JourneyPatternWaitTimes_RelStructure waitTimes;
    protected JourneyPatternHeadways_RelStructure headways;
    protected JourneyPatternLayovers_RelStructure layovers;
    protected PointsInJourneyPattern_RelStructure pointsInSequence;
    protected LinksInJourneyPattern_RelStructure linksInSequence;

    /**
     * Gets the value of the routeRef property.
     * 
     * @return
     *     possible object is
     *     {@link RouteRefStructure }
     *     
     */
    public RouteRefStructure getRouteRef() {
        return routeRef;
    }

    /**
     * Sets the value of the routeRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link RouteRefStructure }
     *     
     */
    public void setRouteRef(RouteRefStructure value) {
        this.routeRef = value;
    }

    /**
     * Gets the value of the routeView property.
     * 
     * @return
     *     possible object is
     *     {@link RouteView }
     *     
     */
    public RouteView getRouteView() {
        return routeView;
    }

    /**
     * Sets the value of the routeView property.
     * 
     * @param value
     *     allowed object is
     *     {@link RouteView }
     *     
     */
    public void setRouteView(RouteView value) {
        this.routeView = value;
    }

    /**
     * Gets the value of the directionType property.
     * 
     * @return
     *     possible object is
     *     {@link DirectionTypeEnumeration }
     *     
     */
    public DirectionTypeEnumeration getDirectionType() {
        return directionType;
    }

    /**
     * Sets the value of the directionType property.
     * 
     * @param value
     *     allowed object is
     *     {@link DirectionTypeEnumeration }
     *     
     */
    public void setDirectionType(DirectionTypeEnumeration value) {
        this.directionType = value;
    }

    /**
     * Gets the value of the directionRef property.
     * 
     * @return
     *     possible object is
     *     {@link DirectionRefStructure }
     *     
     */
    public DirectionRefStructure getDirectionRef() {
        return directionRef;
    }

    /**
     * Sets the value of the directionRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link DirectionRefStructure }
     *     
     */
    public void setDirectionRef(DirectionRefStructure value) {
        this.directionRef = value;
    }

    /**
     * Gets the value of the directionView property.
     * 
     * @return
     *     possible object is
     *     {@link DirectionView }
     *     
     */
    public DirectionView getDirectionView() {
        return directionView;
    }

    /**
     * Sets the value of the directionView property.
     * 
     * @param value
     *     allowed object is
     *     {@link DirectionView }
     *     
     */
    public void setDirectionView(DirectionView value) {
        this.directionView = value;
    }

    /**
     * Gets the value of the destinationDisplayRef property.
     * 
     * @return
     *     possible object is
     *     {@link DestinationDisplayRefStructure }
     *     
     */
    public DestinationDisplayRefStructure getDestinationDisplayRef() {
        return destinationDisplayRef;
    }

    /**
     * Sets the value of the destinationDisplayRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link DestinationDisplayRefStructure }
     *     
     */
    public void setDestinationDisplayRef(DestinationDisplayRefStructure value) {
        this.destinationDisplayRef = value;
    }

    /**
     * Destination / Direction name for JOURNEY PATTERN.
     * 
     * @return
     *     possible object is
     *     {@link DestinationDisplayView }
     *     
     */
    public DestinationDisplayView getDestinationDisplayView() {
        return destinationDisplayView;
    }

    /**
     * Sets the value of the destinationDisplayView property.
     * 
     * @param value
     *     allowed object is
     *     {@link DestinationDisplayView }
     *     
     */
    public void setDestinationDisplayView(DestinationDisplayView value) {
        this.destinationDisplayView = value;
    }

    /**
     * Gets the value of the typeOfJourneyPatternRef property.
     * 
     * @return
     *     possible object is
     *     {@link TypeOfJourneyPatternRefStructure }
     *     
     */
    public TypeOfJourneyPatternRefStructure getTypeOfJourneyPatternRef() {
        return typeOfJourneyPatternRef;
    }

    /**
     * Sets the value of the typeOfJourneyPatternRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeOfJourneyPatternRefStructure }
     *     
     */
    public void setTypeOfJourneyPatternRef(TypeOfJourneyPatternRefStructure value) {
        this.typeOfJourneyPatternRef = value;
    }

    /**
     * Gets the value of the operationalContextRef property.
     * 
     * @return
     *     possible object is
     *     {@link OperationalContextRefStructure }
     *     
     */
    public OperationalContextRefStructure getOperationalContextRef() {
        return operationalContextRef;
    }

    /**
     * Sets the value of the operationalContextRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link OperationalContextRefStructure }
     *     
     */
    public void setOperationalContextRef(OperationalContextRefStructure value) {
        this.operationalContextRef = value;
    }

    /**
     * Reference to a TIMING PATTERN.
     * 
     * @return
     *     possible object is
     *     {@link TimingPatternRefStructure }
     *     
     */
    public TimingPatternRefStructure getTimingPatternRef() {
        return timingPatternRef;
    }

    /**
     * Sets the value of the timingPatternRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link TimingPatternRefStructure }
     *     
     */
    public void setTimingPatternRef(TimingPatternRefStructure value) {
        this.timingPatternRef = value;
    }

    /**
     * Gets the value of the runTimes property.
     * 
     * @return
     *     possible object is
     *     {@link JourneyPatternRunTimes_RelStructure }
     *     
     */
    public JourneyPatternRunTimes_RelStructure getRunTimes() {
        return runTimes;
    }

    /**
     * Sets the value of the runTimes property.
     * 
     * @param value
     *     allowed object is
     *     {@link JourneyPatternRunTimes_RelStructure }
     *     
     */
    public void setRunTimes(JourneyPatternRunTimes_RelStructure value) {
        this.runTimes = value;
    }

    /**
     * Gets the value of the waitTimes property.
     * 
     * @return
     *     possible object is
     *     {@link JourneyPatternWaitTimes_RelStructure }
     *     
     */
    public JourneyPatternWaitTimes_RelStructure getWaitTimes() {
        return waitTimes;
    }

    /**
     * Sets the value of the waitTimes property.
     * 
     * @param value
     *     allowed object is
     *     {@link JourneyPatternWaitTimes_RelStructure }
     *     
     */
    public void setWaitTimes(JourneyPatternWaitTimes_RelStructure value) {
        this.waitTimes = value;
    }

    /**
     * Gets the value of the headways property.
     * 
     * @return
     *     possible object is
     *     {@link JourneyPatternHeadways_RelStructure }
     *     
     */
    public JourneyPatternHeadways_RelStructure getHeadways() {
        return headways;
    }

    /**
     * Sets the value of the headways property.
     * 
     * @param value
     *     allowed object is
     *     {@link JourneyPatternHeadways_RelStructure }
     *     
     */
    public void setHeadways(JourneyPatternHeadways_RelStructure value) {
        this.headways = value;
    }

    /**
     * Gets the value of the layovers property.
     * 
     * @return
     *     possible object is
     *     {@link JourneyPatternLayovers_RelStructure }
     *     
     */
    public JourneyPatternLayovers_RelStructure getLayovers() {
        return layovers;
    }

    /**
     * Sets the value of the layovers property.
     * 
     * @param value
     *     allowed object is
     *     {@link JourneyPatternLayovers_RelStructure }
     *     
     */
    public void setLayovers(JourneyPatternLayovers_RelStructure value) {
        this.layovers = value;
    }

    /**
     * Gets the value of the pointsInSequence property.
     * 
     * @return
     *     possible object is
     *     {@link PointsInJourneyPattern_RelStructure }
     *     
     */
    public PointsInJourneyPattern_RelStructure getPointsInSequence() {
        return pointsInSequence;
    }

    /**
     * Sets the value of the pointsInSequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link PointsInJourneyPattern_RelStructure }
     *     
     */
    public void setPointsInSequence(PointsInJourneyPattern_RelStructure value) {
        this.pointsInSequence = value;
    }

    /**
     * Gets the value of the linksInSequence property.
     * 
     * @return
     *     possible object is
     *     {@link LinksInJourneyPattern_RelStructure }
     *     
     */
    public LinksInJourneyPattern_RelStructure getLinksInSequence() {
        return linksInSequence;
    }

    /**
     * Sets the value of the linksInSequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link LinksInJourneyPattern_RelStructure }
     *     
     */
    public void setLinksInSequence(LinksInJourneyPattern_RelStructure value) {
        this.linksInSequence = value;
    }

}
