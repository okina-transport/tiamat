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
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a list of references to COMPLEX FEATUREs.
 * 
 * <p>Java class for complexFeatureRefs_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="complexFeatureRefs_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}oneToManyRelationshipStructure">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.netex.org.uk/netex}ComplexFeatureRef"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "complexFeatureRefs_RelStructure", propOrder = {
    "complexFeatureRef"
})
public class ComplexFeatureRefs_RelStructure
    extends OneToManyRelationshipStructure
{

    @XmlElement(name = "ComplexFeatureRef", required = true)
    protected ComplexFeatureRefStructure complexFeatureRef;

    /**
     * Gets the value of the complexFeatureRef property.
     * 
     * @return
     *     possible object is
     *     {@link ComplexFeatureRefStructure }
     *     
     */
    public ComplexFeatureRefStructure getComplexFeatureRef() {
        return complexFeatureRef;
    }

    /**
     * Sets the value of the complexFeatureRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link ComplexFeatureRefStructure }
     *     
     */
    public void setComplexFeatureRef(ComplexFeatureRefStructure value) {
        this.complexFeatureRef = value;
    }

}
