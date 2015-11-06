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
 * Type for a list of TRAIN COMPONENTs.
 * 
 * <p>Java class for trainComponents_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="trainComponents_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}containmentAggregationStructure">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element ref="{http://www.netex.org.uk/netex}TrainComponentRef"/>
 *         &lt;element ref="{http://www.netex.org.uk/netex}TrainComponent"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "trainComponents_RelStructure", propOrder = {
    "trainComponentRefOrTrainComponent"
})
public class TrainComponents_RelStructure
    extends ContainmentAggregationStructure
{

    @XmlElements({
        @XmlElement(name = "TrainComponentRef", type = TrainComponentRefStructure.class),
        @XmlElement(name = "TrainComponent", type = TrainComponent.class)
    })
    protected List<Object> trainComponentRefOrTrainComponent;

    /**
     * Gets the value of the trainComponentRefOrTrainComponent property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the trainComponentRefOrTrainComponent property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTrainComponentRefOrTrainComponent().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TrainComponentRefStructure }
     * {@link TrainComponent }
     * 
     * 
     */
    public List<Object> getTrainComponentRefOrTrainComponent() {
        if (trainComponentRefOrTrainComponent == null) {
            trainComponentRefOrTrainComponent = new ArrayList<Object>();
        }
        return this.trainComponentRefOrTrainComponent;
    }

}
