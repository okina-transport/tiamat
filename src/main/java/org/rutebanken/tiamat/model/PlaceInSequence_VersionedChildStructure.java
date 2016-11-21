//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.06 at 10:37:32 AM CET 
//


package org.rutebanken.tiamat.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Type for a PLACE in SEQUENCE.
 * 
 * <p>Java class for PlaceInSequence_VersionedChildStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PlaceInSequence_VersionedChildStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}PointInLinkSequence_VersionedChildStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}PlaceInSequenceGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PlaceInSequence_VersionedChildStructure", propOrder = {
    "placeRef",
    "branchLevel",
    "description",
    "onwardLinks"
})
@XmlSeeAlso({
    PlaceInSequence.class
})
public class PlaceInSequence_VersionedChildStructure
    extends PointInLinkSequence_VersionedChildStructure
{

    protected PlaceRefStructure placeRef;
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NMTOKEN")
    protected String branchLevel;
    protected MultilingualStringEntity description;
    protected OnwardLinks onwardLinks;

    /**
     * Gets the value of the placeRef property.
     * 
     * @return
     *     possible object is
     *     {@link PlaceRefStructure }
     *     
     */
    public PlaceRefStructure getPlaceRef() {
        return placeRef;
    }

    /**
     * Sets the value of the placeRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link PlaceRefStructure }
     *     
     */
    public void setPlaceRef(PlaceRefStructure value) {
        this.placeRef = value;
    }

    /**
     * Gets the value of the branchLevel property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBranchLevel() {
        return branchLevel;
    }

    /**
     * Sets the value of the branchLevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBranchLevel(String value) {
        this.branchLevel = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setDescription(MultilingualStringEntity value) {
        this.description = value;
    }

    /**
     * Gets the value of the onwardLinks property.
     * 
     * @return
     *     possible object is
     *     {@link OnwardLinks }
     *     
     */
    public OnwardLinks getOnwardLinks() {
        return onwardLinks;
    }

    /**
     * Sets the value of the onwardLinks property.
     * 
     * @param value
     *     allowed object is
     *     {@link OnwardLinks }
     *     
     */
    public void setOnwardLinks(OnwardLinks value) {
        this.onwardLinks = value;
    }

}
