//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for POINT OF INTEREST CLASSIFICATION HIERARCHY.
 * 
 * <p>Java class for PointOfInterestClassificationHierarchy_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PointOfInterestClassificationHierarchy_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}GroupOfEntities_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}PointOfInterestClassificationHierarchyGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PointOfInterestClassificationHierarchy_VersionStructure", propOrder = {
    "members"
})
@XmlSeeAlso({
    PointOfInterestClassificationHierarchy.class
})
public class PointOfInterestClassificationHierarchy_VersionStructure
    extends GroupOfEntities_VersionStructure
{

    protected PointOfInterestClassificationHierarchyMembers_RelStructure members;

    /**
     * Gets the value of the members property.
     * 
     * @return
     *     possible object is
     *     {@link PointOfInterestClassificationHierarchyMembers_RelStructure }
     *     
     */
    public PointOfInterestClassificationHierarchyMembers_RelStructure getMembers() {
        return members;
    }

    /**
     * Sets the value of the members property.
     * 
     * @param value
     *     allowed object is
     *     {@link PointOfInterestClassificationHierarchyMembers_RelStructure }
     *     
     */
    public void setMembers(PointOfInterestClassificationHierarchyMembers_RelStructure value) {
        this.members = value;
    }

}
