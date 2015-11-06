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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a  COUNTRY.
 * 
 * <p>Java class for Country_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Country_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}Place_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}CountryGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Country_VersionStructure", propOrder = {
    "uicCode",
    "countryName",
    "alternativeNames"
})
@XmlSeeAlso({
    Country.class
})
public class Country_VersionStructure
    extends Place_VersionStructure
{

    @XmlElement(name = "UicCode")
    protected PrivateCodeStructure uicCode;
    @XmlElement(name = "CountryName")
    protected MultilingualString countryName;
    protected AlternativeNames_RelStructure alternativeNames;

    /**
     * Gets the value of the uicCode property.
     * 
     * @return
     *     possible object is
     *     {@link PrivateCodeStructure }
     *     
     */
    public PrivateCodeStructure getUicCode() {
        return uicCode;
    }

    /**
     * Sets the value of the uicCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link PrivateCodeStructure }
     *     
     */
    public void setUicCode(PrivateCodeStructure value) {
        this.uicCode = value;
    }

    /**
     * Gets the value of the countryName property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualString }
     *     
     */
    public MultilingualString getCountryName() {
        return countryName;
    }

    /**
     * Sets the value of the countryName property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualString }
     *     
     */
    public void setCountryName(MultilingualString value) {
        this.countryName = value;
    }

    /**
     * Gets the value of the alternativeNames property.
     * 
     * @return
     *     possible object is
     *     {@link AlternativeNames_RelStructure }
     *     
     */
    public AlternativeNames_RelStructure getAlternativeNames() {
        return alternativeNames;
    }

    /**
     * Sets the value of the alternativeNames property.
     * 
     * @param value
     *     allowed object is
     *     {@link AlternativeNames_RelStructure }
     *     
     */
    public void setAlternativeNames(AlternativeNames_RelStructure value) {
        this.alternativeNames = value;
    }

}
