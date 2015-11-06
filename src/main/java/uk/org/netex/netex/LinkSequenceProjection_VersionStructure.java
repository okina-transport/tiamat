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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import net.opengis.gml._3.LineStringType;


/**
 * Type for a LINK SEQUENCE PROJECTION.
 * 
 * <p>Java class for LinkSequenceProjection_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LinkSequenceProjection_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}Projection_VersionStructure">
 *       &lt;sequence>
 *         &lt;element name="ProjectedLinkSequenceRef" type="{http://www.netex.org.uk/netex}LinkSequenceRefStructure" minOccurs="0"/>
 *         &lt;element name="Distance" type="{http://www.netex.org.uk/netex}DistanceType" minOccurs="0"/>
 *         &lt;group ref="{http://www.netex.org.uk/netex}LinkSequenceProjectionGroup" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LinkSequenceProjection_VersionStructure", propOrder = {
    "projectedLinkSequenceRef",
    "distance",
    "points",
    "lineString"
})
@XmlSeeAlso({
    LinkSequenceProjection.class
})
public class LinkSequenceProjection_VersionStructure
    extends Projection_VersionStructure
{

    @XmlElement(name = "ProjectedLinkSequenceRef")
    protected LinkSequenceRefStructure projectedLinkSequenceRef;
    @XmlElement(name = "Distance")
    protected BigDecimal distance;
    protected PointRefs_RelStructure points;
    @XmlElement(name = "LineString", namespace = "http://www.opengis.net/gml/3.2")
    protected LineStringType lineString;

    /**
     * Gets the value of the projectedLinkSequenceRef property.
     * 
     * @return
     *     possible object is
     *     {@link LinkSequenceRefStructure }
     *     
     */
    public LinkSequenceRefStructure getProjectedLinkSequenceRef() {
        return projectedLinkSequenceRef;
    }

    /**
     * Sets the value of the projectedLinkSequenceRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link LinkSequenceRefStructure }
     *     
     */
    public void setProjectedLinkSequenceRef(LinkSequenceRefStructure value) {
        this.projectedLinkSequenceRef = value;
    }

    /**
     * Gets the value of the distance property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getDistance() {
        return distance;
    }

    /**
     * Sets the value of the distance property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setDistance(BigDecimal value) {
        this.distance = value;
    }

    /**
     * Gets the value of the points property.
     * 
     * @return
     *     possible object is
     *     {@link PointRefs_RelStructure }
     *     
     */
    public PointRefs_RelStructure getPoints() {
        return points;
    }

    /**
     * Sets the value of the points property.
     * 
     * @param value
     *     allowed object is
     *     {@link PointRefs_RelStructure }
     *     
     */
    public void setPoints(PointRefs_RelStructure value) {
        this.points = value;
    }

    /**
     * Gets the value of the lineString property.
     * 
     * @return
     *     possible object is
     *     {@link LineStringType }
     *     
     */
    public LineStringType getLineString() {
        return lineString;
    }

    /**
     * Sets the value of the lineString property.
     * 
     * @param value
     *     allowed object is
     *     {@link LineStringType }
     *     
     */
    public void setLineString(LineStringType value) {
        this.lineString = value;
    }

}
