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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a list of ACTIVATION ASSIGNMENTs.
 * 
 * <p>Java class for activationAssignments_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="activationAssignments_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}containmentAggregationStructure">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element ref="{http://www.netex.org.uk/netex}ActivationAssignmentRef"/>
 *         &lt;element ref="{http://www.netex.org.uk/netex}ActivationAssignment"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "activationAssignments_RelStructure", propOrder = {
    "activationAssignmentRefOrActivationAssignment"
})
public class ActivationAssignments_RelStructure
    extends ContainmentAggregationStructure
{

    @XmlElements({
        @XmlElement(name = "ActivationAssignmentRef", type = ActivationAssignmentRefStructure.class),
        @XmlElement(name = "ActivationAssignment", type = ActivationAssignment.class)
    })
    protected List<Object> activationAssignmentRefOrActivationAssignment;

    /**
     * Gets the value of the activationAssignmentRefOrActivationAssignment property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the activationAssignmentRefOrActivationAssignment property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getActivationAssignmentRefOrActivationAssignment().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ActivationAssignmentRefStructure }
     * {@link ActivationAssignment }
     * 
     * 
     */
    public List<Object> getActivationAssignmentRefOrActivationAssignment() {
        if (activationAssignmentRefOrActivationAssignment == null) {
            activationAssignmentRefOrActivationAssignment = new ArrayList<Object>();
        }
        return this.activationAssignmentRefOrActivationAssignment;
    }

}
