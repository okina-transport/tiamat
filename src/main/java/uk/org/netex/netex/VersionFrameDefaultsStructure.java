//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Type for frame defaults.
 * 
 * <p>Java class for VersionFrameDefaultsStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="VersionFrameDefaultsStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="DefaultCodespaceRef" type="{http://www.netex.org.uk/netex}CodespaceRefStructure" minOccurs="0"/>
 *         &lt;element name="DefaultDataSourceRef" type="{http://www.netex.org.uk/netex}DataSourceRefStructure" minOccurs="0"/>
 *         &lt;element name="DefaultResponsibilitySetRef" type="{http://www.netex.org.uk/netex}ResponsibilitySetRefStructure" minOccurs="0"/>
 *         &lt;element name="DefaultLocale" type="{http://www.netex.org.uk/netex}LocaleStructure" minOccurs="0"/>
 *         &lt;element name="DefaultLocationSystem" type="{http://www.w3.org/2001/XMLSchema}normalizedString" minOccurs="0"/>
 *         &lt;element name="DefaultSystemOfUnits" type="{http://www.netex.org.uk/netex}SystemOfUnits" minOccurs="0"/>
 *         &lt;element name="DefaultCurrency" type="{http://www.netex.org.uk/netex}CurrencyType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VersionFrameDefaultsStructure", propOrder = {
    "defaultCodespaceRef",
    "defaultDataSourceRef",
    "defaultResponsibilitySetRef",
    "defaultLocale",
    "defaultLocationSystem",
    "defaultSystemOfUnits",
    "defaultCurrency"
})
public class VersionFrameDefaultsStructure {

    @XmlElement(name = "DefaultCodespaceRef")
    protected CodespaceRefStructure defaultCodespaceRef;
    @XmlElement(name = "DefaultDataSourceRef")
    protected DataSourceRefStructure defaultDataSourceRef;
    @XmlElement(name = "DefaultResponsibilitySetRef")
    protected ResponsibilitySetRefStructure defaultResponsibilitySetRef;
    @XmlElement(name = "DefaultLocale")
    protected LocaleStructure defaultLocale;
    @XmlElement(name = "DefaultLocationSystem")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    @XmlSchemaType(name = "normalizedString")
    protected String defaultLocationSystem;
    @XmlElement(name = "DefaultSystemOfUnits", defaultValue = "SiMetres")
    @XmlSchemaType(name = "normalizedString")
    protected SystemOfUnits defaultSystemOfUnits;
    @XmlElement(name = "DefaultCurrency")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NMTOKEN")
    protected String defaultCurrency;

    /**
     * Gets the value of the defaultCodespaceRef property.
     * 
     * @return
     *     possible object is
     *     {@link CodespaceRefStructure }
     *     
     */
    public CodespaceRefStructure getDefaultCodespaceRef() {
        return defaultCodespaceRef;
    }

    /**
     * Sets the value of the defaultCodespaceRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link CodespaceRefStructure }
     *     
     */
    public void setDefaultCodespaceRef(CodespaceRefStructure value) {
        this.defaultCodespaceRef = value;
    }

    /**
     * Gets the value of the defaultDataSourceRef property.
     * 
     * @return
     *     possible object is
     *     {@link DataSourceRefStructure }
     *     
     */
    public DataSourceRefStructure getDefaultDataSourceRef() {
        return defaultDataSourceRef;
    }

    /**
     * Sets the value of the defaultDataSourceRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataSourceRefStructure }
     *     
     */
    public void setDefaultDataSourceRef(DataSourceRefStructure value) {
        this.defaultDataSourceRef = value;
    }

    /**
     * Gets the value of the defaultResponsibilitySetRef property.
     * 
     * @return
     *     possible object is
     *     {@link ResponsibilitySetRefStructure }
     *     
     */
    public ResponsibilitySetRefStructure getDefaultResponsibilitySetRef() {
        return defaultResponsibilitySetRef;
    }

    /**
     * Sets the value of the defaultResponsibilitySetRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link ResponsibilitySetRefStructure }
     *     
     */
    public void setDefaultResponsibilitySetRef(ResponsibilitySetRefStructure value) {
        this.defaultResponsibilitySetRef = value;
    }

    /**
     * Gets the value of the defaultLocale property.
     * 
     * @return
     *     possible object is
     *     {@link LocaleStructure }
     *     
     */
    public LocaleStructure getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * Sets the value of the defaultLocale property.
     * 
     * @param value
     *     allowed object is
     *     {@link LocaleStructure }
     *     
     */
    public void setDefaultLocale(LocaleStructure value) {
        this.defaultLocale = value;
    }

    /**
     * Gets the value of the defaultLocationSystem property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDefaultLocationSystem() {
        return defaultLocationSystem;
    }

    /**
     * Sets the value of the defaultLocationSystem property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDefaultLocationSystem(String value) {
        this.defaultLocationSystem = value;
    }

    /**
     * Gets the value of the defaultSystemOfUnits property.
     * 
     * @return
     *     possible object is
     *     {@link SystemOfUnits }
     *     
     */
    public SystemOfUnits getDefaultSystemOfUnits() {
        return defaultSystemOfUnits;
    }

    /**
     * Sets the value of the defaultSystemOfUnits property.
     * 
     * @param value
     *     allowed object is
     *     {@link SystemOfUnits }
     *     
     */
    public void setDefaultSystemOfUnits(SystemOfUnits value) {
        this.defaultSystemOfUnits = value;
    }

    /**
     * Gets the value of the defaultCurrency property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    /**
     * Sets the value of the defaultCurrency property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDefaultCurrency(String value) {
        this.defaultCurrency = value;
    }

}
