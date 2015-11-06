//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import java.math.BigInteger;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a PROJECTION.
 * 
 * <p>Java class for Projection_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Projection_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}DataManagedObjectStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}ProjectionGroup"/>
 *       &lt;/sequence>
 *       &lt;attribute name="order" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Projection_VersionStructure", propOrder = {
    "typeOfProjectionRef",
    "name",
    "spatialFeatureRef"
})
@XmlSeeAlso({
    ComplexFeatureProjection_VersionStructure.class,
    PointProjection_VersionStructure.class,
    LinkSequenceProjection_VersionStructure.class,
    LinkProjection_VersionStructure.class,
    ZoneProjection_VersionStructure.class
})
public class Projection_VersionStructure
    extends DataManagedObjectStructure
{

    @XmlElement(name = "TypeOfProjectionRef")
    protected TypeOfProjectionRefStructure typeOfProjectionRef;
    @XmlElement(name = "Name")
    protected MultilingualString name;
    @XmlElementRef(name = "SpatialFeatureRef", namespace = "http://www.netex.org.uk/netex", type = JAXBElement.class, required = false)
    protected JAXBElement<? extends GroupOfPointsRefStructure> spatialFeatureRef;
    @XmlAttribute(name = "order")
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger order;

    /**
     * Gets the value of the typeOfProjectionRef property.
     * 
     * @return
     *     possible object is
     *     {@link TypeOfProjectionRefStructure }
     *     
     */
    public TypeOfProjectionRefStructure getTypeOfProjectionRef() {
        return typeOfProjectionRef;
    }

    /**
     * Sets the value of the typeOfProjectionRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeOfProjectionRefStructure }
     *     
     */
    public void setTypeOfProjectionRef(TypeOfProjectionRefStructure value) {
        this.typeOfProjectionRef = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualString }
     *     
     */
    public MultilingualString getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualString }
     *     
     */
    public void setName(MultilingualString value) {
        this.name = value;
    }

    /**
     * Gets the value of the spatialFeatureRef property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ComplexFeatureRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link GroupOfPointsRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link SimpleFeatureRefStructure }{@code >}
     *     
     */
    public JAXBElement<? extends GroupOfPointsRefStructure> getSpatialFeatureRef() {
        return spatialFeatureRef;
    }

    /**
     * Sets the value of the spatialFeatureRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ComplexFeatureRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link GroupOfPointsRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link SimpleFeatureRefStructure }{@code >}
     *     
     */
    public void setSpatialFeatureRef(JAXBElement<? extends GroupOfPointsRefStructure> value) {
        this.spatialFeatureRef = value;
    }

    /**
     * Gets the value of the order property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getOrder() {
        return order;
    }

    /**
     * Sets the value of the order property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setOrder(BigInteger value) {
        this.order = value;
    }

}
