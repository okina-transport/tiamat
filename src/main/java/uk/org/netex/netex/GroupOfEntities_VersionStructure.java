//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a GROUP OF ENTITies.
 * 
 * <p>Java class for GroupOfEntities_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GroupOfEntities_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}DataManagedObjectStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}GroupOfEntitiesGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GroupOfEntities_VersionStructure", propOrder = {
    "name",
    "shortName",
    "description",
    "purposeOfGroupingRef",
    "privateCode"
})
@XmlSeeAlso({
    PointOfInterestClassificationHierarchy_VersionStructure.class,
    GroupOfLinkSequences_VersionStructure.class,
    GeneralGroupOfEntities_VersionStructure.class,
    GroupOfOperatorsStructure.class,
    GroupOfTimebands_VersionedChildStructure.class,
    GroupOfLines_VersionStructure.class,
    CrewBase_VersionStructure.class,
    GroupOfLinks_VersionStructure.class,
    GroupOfPlaces_VersionStructure.class,
    GroupOfTimingLinks_RelStructure.class,
    Layer_VersionStructure.class,
    CommonSection_VersionStructure.class,
    GroupOfPoints_VersionStructure.class,
    GroupOfStopPlacesStructure.class
})
@MappedSuperclass
public abstract class GroupOfEntities_VersionStructure
    extends DataManagedObjectStructure
{

    @XmlElement(name = "Name")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lang", column = @Column(name = "name_lang")),
            @AttributeOverride(name = "value", column = @Column(name = "name_val")),
            @AttributeOverride(name = "textIdType", column = @Column(name = "name_text_id_type"))
    })
    protected MultilingualString name;

    @XmlElement(name = "ShortName")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lang", column = @Column(name = "short_name_lang")),
            @AttributeOverride(name = "value", column = @Column(name = "short_name_val")),
            @AttributeOverride(name = "textIdType", column = @Column(name = "short_name_text_id_type"))
    })
    protected MultilingualString shortName;

    @XmlElement(name = "Description")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lang", column = @Column(name = "description_lang")),
            @AttributeOverride(name = "value", column = @Column(name = "description_val")),
            @AttributeOverride(name = "textIdType", column = @Column(name = "description_text_id_type"))
    })
    protected MultilingualString description;

    @XmlElement(name = "PurposeOfGroupingRef")
    @Transient
    protected PurposeOfGroupingRefStructure purposeOfGroupingRef;

    @XmlElement(name = "PrivateCode")
    @Transient
    protected PrivateCodeStructure privateCode;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualString }
     *     
     */
    public MultilingualString getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualString }
     *     
     */
    public void setName(MultilingualString value) {
        this.name = value;
    }

    /**
     * Gets the value of the shortName property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualString }
     *     
     */
    public MultilingualString getShortName() {
        return shortName;
    }

    /**
     * Sets the value of the shortName property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualString }
     *     
     */
    public void setShortName(MultilingualString value) {
        this.shortName = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualString }
     *     
     */
    public MultilingualString getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualString }
     *     
     */
    public void setDescription(MultilingualString value) {
        this.description = value;
    }

    /**
     * Reference to a PURPOSE OF GROUPING.
     * 
     * @return
     *     possible object is
     *     {@link PurposeOfGroupingRefStructure }
     *     
     */
    public PurposeOfGroupingRefStructure getPurposeOfGroupingRef() {
        return purposeOfGroupingRef;
    }

    /**
     * Sets the value of the purposeOfGroupingRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link PurposeOfGroupingRefStructure }
     *     
     */
    public void setPurposeOfGroupingRef(PurposeOfGroupingRefStructure value) {
        this.purposeOfGroupingRef = value;
    }

    /**
     * Gets the value of the privateCode property.
     * 
     * @return
     *     possible object is
     *     {@link PrivateCodeStructure }
     *     
     */
    public PrivateCodeStructure getPrivateCode() {
        return privateCode;
    }

    /**
     * Sets the value of the privateCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link PrivateCodeStructure }
     *     
     */
    public void setPrivateCode(PrivateCodeStructure value) {
        this.privateCode = value;
    }

}
