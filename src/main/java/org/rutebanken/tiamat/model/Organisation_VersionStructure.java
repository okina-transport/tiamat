//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.06 at 10:37:32 AM CET 
//


package org.rutebanken.tiamat.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for an ORGANISATION.
 * 
 * <p>Java class for Organisation_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Organisation_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}DataManagedObjectStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}OrganisationGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Organisation_VersionStructure", propOrder = {
    "rest"
})
@XmlSeeAlso({
    Organisation.class,
    Operator_VersionStructure.class,
    Authority_VersionStructure.class,
    OtherOrganisation_VersionStructure.class
})
public abstract class Organisation_VersionStructure
    extends DataManagedObjectStructure
{

    })
    protected List<JAXBElement<?>> rest;

    /**
     * Gets the rest of the content model. 
     * 
     * <p>
     * You are getting this "catch-all" property because of the following reason: 
     * The field name "Status" is used by two different parts of a schema. See: 
     * line 144 of file:/Users/cris/git/NeTEx/xsd/netex_framework/netex_genericFramework/netex_organisation_version-v1.0.xsd
     * line 158 of file:/Users/cris/git/NeTEx/xsd/netex_framework/netex_responsibility/netex_version_support-v1.0.xsd
     * <p>
     * To get rid of this property, apply a property customization to one 
     * of both of the following declarations to change their names: 
     * Gets the value of the rest property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rest property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRest().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link MultilingualStringEntity }{@code >}
     * {@link JAXBElement }{@code <}{@link ResponsibilitySet_VersionStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link ValidityPeriod }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link ExternalObjectRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link MultilingualStringEntity }{@code >}
     * {@link JAXBElement }{@code <}{@link AlternativeNames_RelStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link OrganisationRefs_RelStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link LocaleStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link MultilingualStringEntity }{@code >}
     * {@link JAXBElement }{@code <}{@link ResponsibilitySetRefStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link List }{@code <}{@link OrganisationTypeEnumeration }{@code >}{@code >}
     * {@link JAXBElement }{@code <}{@link ContactStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link PrivateCodeStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link MultilingualStringEntity }{@code >}
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * {@link JAXBElement }{@code <}{@link ContactStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link MultilingualStringEntity }{@code >}
     * {@link JAXBElement }{@code <}{@link MultilingualStringEntity }{@code >}
     * {@link JAXBElement }{@code <}{@link TypeOfOrganisationRefs_RelStructure }{@code >}
     * {@link JAXBElement }{@code <}{@link Parts }{@code >}
     * 
     * 
     */
    public List<JAXBElement<?>> getRest() {
        if (rest == null) {
            rest = new ArrayList<JAXBElement<?>>();
        }
        return this.rest;
    }

}
