//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.06 at 10:37:32 AM CET 
//


package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a collection of one or more SITEs.
 * 
 * <p>Java class for siteRefs_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="siteRefs_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}oneToManyRelationshipStructure">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.netex.org.uk/netex}SiteRef" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "siteRefs_RelStructure", propOrder = {
    "siteRef"
})
public class SiteRefs_RelStructure
    extends OneToManyRelationshipStructure
{

    protected List<JAXBElement<? extends SiteRefStructure>> siteRef;

    /**
     * Gets the value of the siteRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the siteRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSiteRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link ServiceSIteRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link PointOfInterestRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link ParkingRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link StopPlaceReference }{@code >}
     * {@link JAXBElement }{@code <}{@link SiteRefStructure }{@code >}
     * 
     * 
     */
    public List<JAXBElement<? extends SiteRefStructure>> getSiteRef() {
        if (siteRef == null) {
            siteRef = new ArrayList<JAXBElement<? extends SiteRefStructure>>();
        }
        return this.siteRef;
    }

}
