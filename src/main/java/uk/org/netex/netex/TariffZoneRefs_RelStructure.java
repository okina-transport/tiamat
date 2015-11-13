//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.06 at 10:37:32 AM CET 
//


package uk.org.netex.netex;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a list of TARIFF ZONEs.
 * 
 * <p>Java class for tariffZoneRefs_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tariffZoneRefs_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}oneToManyRelationshipStructure">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.netex.org.uk/netex}TariffZoneRef" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tariffZoneRefs_RelStructure", propOrder = {
    "tariffZoneRef"
})
public class TariffZoneRefs_RelStructure
    extends OneToManyRelationshipStructure
{

    @XmlElement(name = "TariffZoneRef", required = true)
    @OneToMany
    @ElementCollection(targetClass = TariffZoneRef.class)
    protected List<TariffZoneRef> tariffZoneRef;

    /**
     * Gets the value of the tariffZoneRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tariffZoneRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTariffZoneRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TariffZoneRef }
     * 
     * 
     */
    public List<TariffZoneRef> getTariffZoneRef() {
        if (tariffZoneRef == null) {
            tariffZoneRef = new ArrayList<TariffZoneRef>();
        }
        return this.tariffZoneRef;
    }

}
