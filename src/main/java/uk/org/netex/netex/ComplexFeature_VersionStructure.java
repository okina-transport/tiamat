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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a COMPLEX FEATURE.
 * 
 * <p>Java class for ComplexFeature_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ComplexFeature_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}GroupOfPoints_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}ComplexFeatureGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ComplexFeature_VersionStructure", propOrder = {
    "groupOfEntitiesRef",
    "featureMembers"
})
@XmlSeeAlso({
    ComplexFeature.class
})
public class ComplexFeature_VersionStructure
    extends GroupOfPoints_VersionStructure
{

    @XmlElement(name = "GroupOfEntitiesRef")
    protected GroupOfEntitiesRef groupOfEntitiesRef;
    protected ComplexFeatureMembers_RelStructure featureMembers;

    /**
     * Gets the value of the groupOfEntitiesRef property.
     * 
     * @return
     *     possible object is
     *     {@link GroupOfEntitiesRef }
     *     
     */
    public GroupOfEntitiesRef getGroupOfEntitiesRef() {
        return groupOfEntitiesRef;
    }

    /**
     * Sets the value of the groupOfEntitiesRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link GroupOfEntitiesRef }
     *     
     */
    public void setGroupOfEntitiesRef(GroupOfEntitiesRef value) {
        this.groupOfEntitiesRef = value;
    }

    /**
     * Gets the value of the featureMembers property.
     * 
     * @return
     *     possible object is
     *     {@link ComplexFeatureMembers_RelStructure }
     *     
     */
    public ComplexFeatureMembers_RelStructure getFeatureMembers() {
        return featureMembers;
    }

    /**
     * Sets the value of the featureMembers property.
     * 
     * @param value
     *     allowed object is
     *     {@link ComplexFeatureMembers_RelStructure }
     *     
     */
    public void setFeatureMembers(ComplexFeatureMembers_RelStructure value) {
        this.featureMembers = value;
    }

}
