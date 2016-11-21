//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a list of PROJECTIONS.
 * 
 * <p>Java class for projections_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="projections_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}containmentAggregationStructure">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element ref="{http://www.netex.org.uk/netex}ProjectionRef"/>
 *         &lt;element ref="{http://www.netex.org.uk/netex}Projection"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "projections_RelStructure", propOrder = {
    "projectionRefOrProjection"
})
public class Projections_RelStructure
    extends ContainmentAggregationStructure
{

    })
    protected List<JAXBElement<?>> projectionRefOrProjection;

    /**
     * Gets the value of the projectionRefOrProjection property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the projectionRefOrProjection property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProjectionRefOrProjection().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Projection_VersionStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link ComplexFeatureProjectionRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link ComplexFeatureProjection }{@code >}
     * {@link JAXBElement }{@code <}{@link ZoneProjection }{@code >}
     * {@link JAXBElement }{@code <}{@link LinkProjection }{@code >}
     * {@link JAXBElement }{@code <}{@link PointProjection }{@code >}
     * {@link JAXBElement }{@code <}{@link LinkSequenceProjection }{@code >}
     * {@link JAXBElement }{@code <}{@link LinkProjectionRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link LinkSequenceProjectionRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link PointProjectionRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link ProjectionRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link ZoneProjectionRefStructure }{@code >}
     * 
     * 
     */
    public List<JAXBElement<?>> getProjectionRefOrProjection() {
        if (projectionRefOrProjection == null) {
            projectionRefOrProjection = new ArrayList<JAXBElement<?>>();
        }
        return this.projectionRefOrProjection;
    }

}
