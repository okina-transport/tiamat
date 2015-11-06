//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for containment in frame of ROUTE.
 * 
 * <p>Java class for routesInFrame_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="routesInFrame_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}containmentAggregationStructure">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.netex.org.uk/netex}Route_" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "routesInFrame_RelStructure", propOrder = {
    "route_"
})
public class RoutesInFrame_RelStructure
    extends ContainmentAggregationStructure
{

    @XmlElementRef(name = "Route_", namespace = "http://www.netex.org.uk/netex", type = JAXBElement.class)
    protected List<JAXBElement<? extends LinkSequence_VersionStructure>> route_;

    /**
     * Gets the value of the route property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the route property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRoute_().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link FlexibleRoute }{@code >}
     * {@link JAXBElement }{@code <}{@link LinkSequence_VersionStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link Route }{@code >}
     * 
     * 
     */
    public List<JAXBElement<? extends LinkSequence_VersionStructure>> getRoute_() {
        if (route_ == null) {
            route_ = new ArrayList<JAXBElement<? extends LinkSequence_VersionStructure>>();
        }
        return this.route_;
    }

}
