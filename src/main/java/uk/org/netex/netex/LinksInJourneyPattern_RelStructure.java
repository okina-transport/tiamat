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
 * Type for LINK IN JOURNEY PATTERN.
 * 
 * <p>Java class for linksInJourneyPattern_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="linksInJourneyPattern_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}strictContainmentAggregationStructure">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element ref="{http://www.netex.org.uk/netex}ServiceLinkInJourneyPattern"/>
 *         &lt;element ref="{http://www.netex.org.uk/netex}TimingLinkInJourneyPattern"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "linksInJourneyPattern_RelStructure", propOrder = {
    "serviceLinkInJourneyPatternOrTimingLinkInJourneyPattern"
})
public class LinksInJourneyPattern_RelStructure
    extends StrictContainmentAggregationStructure
{

    @XmlElements({
        @XmlElement(name = "ServiceLinkInJourneyPattern", type = ServiceLinkInJourneyPattern_VersionedChildStructure.class),
        @XmlElement(name = "TimingLinkInJourneyPattern", type = TimingLinkInJourneyPattern.class)
    })
    protected List<LinkInLinkSequence_VersionedChildStructure> serviceLinkInJourneyPatternOrTimingLinkInJourneyPattern;

    /**
     * Gets the value of the serviceLinkInJourneyPatternOrTimingLinkInJourneyPattern property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the serviceLinkInJourneyPatternOrTimingLinkInJourneyPattern property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ServiceLinkInJourneyPattern_VersionedChildStructure }
     * {@link TimingLinkInJourneyPattern }
     * 
     * 
     */
    public List<LinkInLinkSequence_VersionedChildStructure> getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern() {
        if (serviceLinkInJourneyPatternOrTimingLinkInJourneyPattern == null) {
            serviceLinkInJourneyPatternOrTimingLinkInJourneyPattern = new ArrayList<LinkInLinkSequence_VersionedChildStructure>();
        }
        return this.serviceLinkInJourneyPatternOrTimingLinkInJourneyPattern;
    }

}
