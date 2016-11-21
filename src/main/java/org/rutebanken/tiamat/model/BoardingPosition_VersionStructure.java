//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Type for a BOARDING POSITION.
 * 
 * <p>Java class for BoardingPosition_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BoardingPosition_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}StopPlaceSpace_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}BoardingPositionGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BoardingPosition_VersionStructure", propOrder = {
    "publicCode",
    "boardingPositionType",
    "boardingPositionEntrances"
})
@XmlSeeAlso({
    BoardingPosition.class
})
@MappedSuperclass
public class BoardingPosition_VersionStructure
    extends StopPlaceSpace_VersionStructure
{

    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    @XmlSchemaType(name = "normalizedString")
    protected String publicCode;

    @XmlSchemaType(name = "string")
    @Enumerated(EnumType.STRING)
    protected BoardingPositionTypeEnumeration boardingPositionType;

    @Transient
    protected EntranceRefs_RelStructure boardingPositionEntrances;

    /**
     * Gets the value of the publicCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPublicCode() {
        return publicCode;
    }

    /**
     * Sets the value of the publicCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPublicCode(String value) {
        this.publicCode = value;
    }

    /**
     * Gets the value of the boardingPositionType property.
     * 
     * @return
     *     possible object is
     *     {@link BoardingPositionTypeEnumeration }
     *     
     */
    public BoardingPositionTypeEnumeration getBoardingPositionType() {
        return boardingPositionType;
    }

    /**
     * Sets the value of the boardingPositionType property.
     * 
     * @param value
     *     allowed object is
     *     {@link BoardingPositionTypeEnumeration }
     *     
     */
    public void setBoardingPositionType(BoardingPositionTypeEnumeration value) {
        this.boardingPositionType = value;
    }

    /**
     * Gets the value of the boardingPositionEntrances property.
     * 
     * @return
     *     possible object is
     *     {@link EntranceRefs_RelStructure }
     *     
     */
    public EntranceRefs_RelStructure getBoardingPositionEntrances() {
        return boardingPositionEntrances;
    }

    /**
     * Sets the value of the boardingPositionEntrances property.
     * 
     * @param value
     *     allowed object is
     *     {@link EntranceRefs_RelStructure }
     *     
     */
    public void setBoardingPositionEntrances(EntranceRefs_RelStructure value) {
        this.boardingPositionEntrances = value;
    }

}
