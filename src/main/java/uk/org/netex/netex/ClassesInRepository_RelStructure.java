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
 * Type for a list of Classe Filter referencess.
 * 
 * <p>Java class for classesInRepository_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="classesInRepository_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}oneToManyRelationshipStructure">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element ref="{http://www.netex.org.uk/netex}ClassInFrameRef"/>
 *         &lt;element ref="{http://www.netex.org.uk/netex}ClassInFrame"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "classesInRepository_RelStructure", propOrder = {
    "classInFrameRefOrClassInFrame"
})
public class ClassesInRepository_RelStructure
    extends OneToManyRelationshipStructure
{

    @XmlElements({
        @XmlElement(name = "ClassInFrameRef", type = ClassInFrameRefStructure.class),
        @XmlElement(name = "ClassInFrame", type = ClassInFrameStructure.class)
    })
    protected List<Object> classInFrameRefOrClassInFrame;

    /**
     * Gets the value of the classInFrameRefOrClassInFrame property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the classInFrameRefOrClassInFrame property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getClassInFrameRefOrClassInFrame().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ClassInFrameRefStructure }
     * {@link ClassInFrameStructure }
     * 
     * 
     */
    public List<Object> getClassInFrameRefOrClassInFrame() {
        if (classInFrameRefOrClassInFrame == null) {
            classInFrameRefOrClassInFrame = new ArrayList<Object>();
        }
        return this.classInFrameRefOrClassInFrame;
    }

}
