//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a VERSION FRAME.
 * 
 * <p>Java class for VersionFrame_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="VersionFrame_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}DataManagedObjectStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}VersionFrameGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VersionFrame_VersionStructure", propOrder = {
    "name",
    "description",
    "typeOfFrameRef",
    "baselineVersionFrameRef",
    "codespaces",
    "frameDefaults",
    "versions",
    "traces",
    "contentValidityConditions"
})
@XmlSeeAlso({
    Common_VersionFrameStructure.class
})
public class VersionFrame_VersionStructure
    extends DataManagedObjectStructure
{

    @XmlElement(name = "Name")
    protected MultilingualStringEntity name;
    @XmlElement(name = "Description")
    protected MultilingualStringEntity description;
    @XmlElement(name = "TypeOfFrameRef")
    protected TypeOfFrameRefStructure typeOfFrameRef;
    @XmlElement(name = "BaselineVersionFrameRef")
    protected VersionRefStructure baselineVersionFrameRef;
    protected Codespaces_RelStructure codespaces;
    @XmlElement(name = "FrameDefaults")
    protected VersionFrameDefaultsStructure frameDefaults;
    protected Versions_RelStructure versions;
    protected Traces_RelStructure traces;
    protected ValidityConditions_RelStructure contentValidityConditions;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setName(MultilingualStringEntity value) {
        this.name = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setDescription(MultilingualStringEntity value) {
        this.description = value;
    }

    /**
     * Reference to a TYPE OF VERSION FRAME.
     * 
     * @return
     *     possible object is
     *     {@link TypeOfFrameRefStructure }
     *     
     */
    public TypeOfFrameRefStructure getTypeOfFrameRef() {
        return typeOfFrameRef;
    }

    /**
     * Sets the value of the typeOfFrameRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeOfFrameRefStructure }
     *     
     */
    public void setTypeOfFrameRef(TypeOfFrameRefStructure value) {
        this.typeOfFrameRef = value;
    }

    /**
     * Gets the value of the baselineVersionFrameRef property.
     * 
     * @return
     *     possible object is
     *     {@link VersionRefStructure }
     *     
     */
    public VersionRefStructure getBaselineVersionFrameRef() {
        return baselineVersionFrameRef;
    }

    /**
     * Sets the value of the baselineVersionFrameRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link VersionRefStructure }
     *     
     */
    public void setBaselineVersionFrameRef(VersionRefStructure value) {
        this.baselineVersionFrameRef = value;
    }

    /**
     * Gets the value of the codespaces property.
     * 
     * @return
     *     possible object is
     *     {@link Codespaces_RelStructure }
     *     
     */
    public Codespaces_RelStructure getCodespaces() {
        return codespaces;
    }

    /**
     * Sets the value of the codespaces property.
     * 
     * @param value
     *     allowed object is
     *     {@link Codespaces_RelStructure }
     *     
     */
    public void setCodespaces(Codespaces_RelStructure value) {
        this.codespaces = value;
    }

    /**
     * Gets the value of the frameDefaults property.
     * 
     * @return
     *     possible object is
     *     {@link VersionFrameDefaultsStructure }
     *     
     */
    public VersionFrameDefaultsStructure getFrameDefaults() {
        return frameDefaults;
    }

    /**
     * Sets the value of the frameDefaults property.
     * 
     * @param value
     *     allowed object is
     *     {@link VersionFrameDefaultsStructure }
     *     
     */
    public void setFrameDefaults(VersionFrameDefaultsStructure value) {
        this.frameDefaults = value;
    }

    /**
     * Gets the value of the versions property.
     * 
     * @return
     *     possible object is
     *     {@link Versions_RelStructure }
     *     
     */
    public Versions_RelStructure getVersions() {
        return versions;
    }

    /**
     * Sets the value of the versions property.
     * 
     * @param value
     *     allowed object is
     *     {@link Versions_RelStructure }
     *     
     */
    public void setVersions(Versions_RelStructure value) {
        this.versions = value;
    }

    /**
     * Gets the value of the traces property.
     * 
     * @return
     *     possible object is
     *     {@link Traces_RelStructure }
     *     
     */
    public Traces_RelStructure getTraces() {
        return traces;
    }

    /**
     * Sets the value of the traces property.
     * 
     * @param value
     *     allowed object is
     *     {@link Traces_RelStructure }
     *     
     */
    public void setTraces(Traces_RelStructure value) {
        this.traces = value;
    }

    /**
     * Gets the value of the contentValidityConditions property.
     * 
     * @return
     *     possible object is
     *     {@link ValidityConditions_RelStructure }
     *     
     */
    public ValidityConditions_RelStructure getContentValidityConditions() {
        return contentValidityConditions;
    }

    /**
     * Sets the value of the contentValidityConditions property.
     * 
     * @param value
     *     allowed object is
     *     {@link ValidityConditions_RelStructure }
     *     
     */
    public void setContentValidityConditions(ValidityConditions_RelStructure value) {
        this.contentValidityConditions = value;
    }

}
