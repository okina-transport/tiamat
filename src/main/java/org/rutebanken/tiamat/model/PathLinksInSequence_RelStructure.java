//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.06 at 10:37:32 AM CET 
//


package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * A collection of one or more PATH LINKs in SEQUENCE.
 * 
 * <p>Java class for pathLinksInSequence_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="pathLinksInSequence_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}strictContainmentAggregationStructure">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.netex.org.uk/netex}PathLinkInSequence" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "pathLinksInSequence_RelStructure", propOrder = {
    "pathLinkInSequence"
})
public class PathLinksInSequence_RelStructure
    extends StrictContainmentAggregationStructure
{

    @XmlElement(name = "PathLinkInSequence", required = true)
    protected List<PathLinkInSequence> pathLinkInSequence;

    /**
     * Gets the value of the pathLinkInSequence property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the pathLinkInSequence property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPathLinkInSequence().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PathLinkInSequence }
     * 
     * 
     */
    public List<PathLinkInSequence> getPathLinkInSequence() {
        if (pathLinkInSequence == null) {
            pathLinkInSequence = new ArrayList<PathLinkInSequence>();
        }
        return this.pathLinkInSequence;
    }

}
