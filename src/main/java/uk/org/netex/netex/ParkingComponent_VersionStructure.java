//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Type for a PARKING COMPONENT.
 * 
 * <p>Java class for ParkingComponent_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ParkingComponent_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}SiteComponent_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}ParkingComponentGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ParkingComponent_VersionStructure", propOrder = {
    "parkingPaymentCode",
    "label",
    "maximumLength",
    "maximumWidth",
    "maximumHeight",
    "maximumWeight"
})
@XmlSeeAlso({
    ParkingComponent.class,
    ParkingBay_VersionStructure.class,
    ParkingArea_VersionStructure.class
})
public class ParkingComponent_VersionStructure
    extends SiteComponent_VersionStructure
{

    @XmlElement(name = "ParkingPaymentCode")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    @XmlSchemaType(name = "normalizedString")
    protected String parkingPaymentCode;
    @XmlElement(name = "Label")
    protected MultilingualString label;
    @XmlElement(name = "MaximumLength")
    protected BigDecimal maximumLength;
    @XmlElement(name = "MaximumWidth")
    protected BigDecimal maximumWidth;
    @XmlElement(name = "MaximumHeight")
    protected BigDecimal maximumHeight;
    @XmlElement(name = "MaximumWeight")
    protected BigDecimal maximumWeight;

    /**
     * Gets the value of the parkingPaymentCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getParkingPaymentCode() {
        return parkingPaymentCode;
    }

    /**
     * Sets the value of the parkingPaymentCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setParkingPaymentCode(String value) {
        this.parkingPaymentCode = value;
    }

    /**
     * Gets the value of the label property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualString }
     *     
     */
    public MultilingualString getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualString }
     *     
     */
    public void setLabel(MultilingualString value) {
        this.label = value;
    }

    /**
     * Gets the value of the maximumLength property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getMaximumLength() {
        return maximumLength;
    }

    /**
     * Sets the value of the maximumLength property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setMaximumLength(BigDecimal value) {
        this.maximumLength = value;
    }

    /**
     * Gets the value of the maximumWidth property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getMaximumWidth() {
        return maximumWidth;
    }

    /**
     * Sets the value of the maximumWidth property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setMaximumWidth(BigDecimal value) {
        this.maximumWidth = value;
    }

    /**
     * Gets the value of the maximumHeight property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getMaximumHeight() {
        return maximumHeight;
    }

    /**
     * Sets the value of the maximumHeight property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setMaximumHeight(BigDecimal value) {
        this.maximumHeight = value;
    }

    /**
     * Gets the value of the maximumWeight property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getMaximumWeight() {
        return maximumWeight;
    }

    /**
     * Sets the value of the maximumWeight property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setMaximumWeight(BigDecimal value) {
        this.maximumWeight = value;
    }

}
