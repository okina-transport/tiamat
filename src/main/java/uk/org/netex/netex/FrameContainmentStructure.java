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
 * Type for an implementation of a frame containment relationship  (ENTITY IN FRAME IN VERSION) A one to many relationship from the containing parent (one) to the contained child (many)
 * 
 * <p>Java class for frameContainmentStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="frameContainmentStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}relationshipStructure">
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "frameContainmentStructure")
@XmlSeeAlso({
    BorderPointsInFrame_RelStructure.class,
    FareSectionsInFrame_RelStructure.class,
    FareZonesInFrame_RelStructure.class,
    FareScheduledStopPointsInFrame_RelStructure.class
})
public class FrameContainmentStructure
    extends RelationshipStructure
{


}
