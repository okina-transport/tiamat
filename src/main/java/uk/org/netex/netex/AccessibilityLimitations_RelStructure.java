//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a list of ACCESSIBILITY LIMITATIONs.
 * 
 * <p>Java class for accessibilityLimitations_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="accessibilityLimitations_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}strictContainmentAggregationStructure">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.netex.org.uk/netex}AccessibilityLimitation" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "accessibilityLimitations_RelStructure", propOrder = {
    "accessibilityLimitation"
})
public class AccessibilityLimitations_RelStructure
    extends StrictContainmentAggregationStructure
{

    @XmlElement(name = "AccessibilityLimitation", required = true)
    protected List<AccessibilityLimitation> accessibilityLimitation;

    /**
     * Assessment of the accessibility of a SITE.Gets the value of the accessibilityLimitation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accessibilityLimitation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccessibilityLimitation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AccessibilityLimitation }
     * 
     * 
     */
    public List<AccessibilityLimitation> getAccessibilityLimitation() {
        if (accessibilityLimitation == null) {
            accessibilityLimitation = new ArrayList<AccessibilityLimitation>();
        }
        return this.accessibilityLimitation;
    }

}
