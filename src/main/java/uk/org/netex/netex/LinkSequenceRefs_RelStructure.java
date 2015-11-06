//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a list of LINK SEQUENCEs.
 * 
 * <p>Java class for linkSequenceRefs_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="linkSequenceRefs_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}oneToManyRelationshipStructure">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.netex.org.uk/netex}LinkSequenceRef" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "linkSequenceRefs_RelStructure", propOrder = {
    "linkSequenceRef"
})
public class LinkSequenceRefs_RelStructure
    extends OneToManyRelationshipStructure
{

    @XmlElementRef(name = "LinkSequenceRef", namespace = "http://www.netex.org.uk/netex", type = JAXBElement.class)
    protected List<JAXBElement<? extends LinkSequenceRefStructure>> linkSequenceRef;

    /**
     * Reference to a TYPE OF LINK.Gets the value of the linkSequenceRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the linkSequenceRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLinkSequenceRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link RouteRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link DeadRunJourneyPatternRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link ServiceJourneyPatternRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link JourneyPatternRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link ServicePatternRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link LinkSequenceRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link NavigationPathRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link TimingPatternRefStructure }{@code >}
     * 
     * 
     */
    public List<JAXBElement<? extends LinkSequenceRefStructure>> getLinkSequenceRef() {
        if (linkSequenceRef == null) {
            linkSequenceRef = new ArrayList<JAXBElement<? extends LinkSequenceRefStructure>>();
        }
        return this.linkSequenceRef;
    }

}
