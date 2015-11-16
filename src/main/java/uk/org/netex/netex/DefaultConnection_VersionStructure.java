//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.06 at 10:37:32 AM CET 
//


package uk.org.netex.netex;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for DEFAULT TRANSFER.
 * 
 * <p>Java class for DefaultConnection_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DefaultConnection_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}Transfer_VersionStructure">
 *       &lt;group ref="{http://www.netex.org.uk/netex}DefaultConnectionGroup"/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DefaultConnection_VersionStructure", propOrder = {
    "from",
    "to",
    "topographicPlaceView",
    "stopAreaRef",
    "siteElementRef"
})
@XmlSeeAlso({
    DefaultConnection.class
})
public class DefaultConnection_VersionStructure
    extends Transfer_VersionStructure
{

    @XmlElement(name = "From")
    protected DefaultConnectionEndStructure from;
    @XmlElement(name = "To")
    protected DefaultConnectionEndStructure to;
    @XmlElement(name = "TopographicPlaceView")
    protected TopographicPlaceView topographicPlaceView;
    @XmlElement(name = "StopAreaRef")
    protected StopAreaRefStructure stopAreaRef;
    @XmlElementRef(name = "SiteElementRef", namespace = "http://www.netex.org.uk/netex", type = JAXBElement.class, required = false)
    protected JAXBElement<? extends SiteElementRefStructure> siteElementRef;

    /**
     * Gets the value of the from property.
     * 
     * @return
     *     possible object is
     *     {@link DefaultConnectionEndStructure }
     *     
     */
    public DefaultConnectionEndStructure getFrom() {
        return from;
    }

    /**
     * Sets the value of the from property.
     * 
     * @param value
     *     allowed object is
     *     {@link DefaultConnectionEndStructure }
     *     
     */
    public void setFrom(DefaultConnectionEndStructure value) {
        this.from = value;
    }

    /**
     * Gets the value of the to property.
     * 
     * @return
     *     possible object is
     *     {@link DefaultConnectionEndStructure }
     *     
     */
    public DefaultConnectionEndStructure getTo() {
        return to;
    }

    /**
     * Sets the value of the to property.
     * 
     * @param value
     *     allowed object is
     *     {@link DefaultConnectionEndStructure }
     *     
     */
    public void setTo(DefaultConnectionEndStructure value) {
        this.to = value;
    }

    /**
     * Gets the value of the topographicPlaceView property.
     * 
     * @return
     *     possible object is
     *     {@link TopographicPlaceView }
     *     
     */
    public TopographicPlaceView getTopographicPlaceView() {
        return topographicPlaceView;
    }

    /**
     * Sets the value of the topographicPlaceView property.
     * 
     * @param value
     *     allowed object is
     *     {@link TopographicPlaceView }
     *     
     */
    public void setTopographicPlaceView(TopographicPlaceView value) {
        this.topographicPlaceView = value;
    }

    /**
     * Gets the value of the stopAreaRef property.
     * 
     * @return
     *     possible object is
     *     {@link StopAreaRefStructure }
     *     
     */
    public StopAreaRefStructure getStopAreaRef() {
        return stopAreaRef;
    }

    /**
     * Sets the value of the stopAreaRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link StopAreaRefStructure }
     *     
     */
    public void setStopAreaRef(StopAreaRefStructure value) {
        this.stopAreaRef = value;
    }

    /**
     * Gets the value of the siteElementRef property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ServiceSIteRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link VehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link VehicleStoppingPositionRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link BoardingPositionRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingBayRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceReference }{@code >}
     *     {@link JAXBElement }{@code <}{@link SiteElementRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link AccessSpaceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingEntranceForVehiclesRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link SiteRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link SiteComponentRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link QuayReference }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceSpaceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestVehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceVehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link EntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestSpaceRefStructure }{@code >}
     *     
     */
    public JAXBElement<? extends SiteElementRefStructure> getSiteElementRef() {
        return siteElementRef;
    }

    /**
     * Sets the value of the siteElementRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ServiceSIteRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link VehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link VehicleStoppingPositionRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link BoardingPositionRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingBayRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceReference }{@code >}
     *     {@link JAXBElement }{@code <}{@link SiteElementRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link AccessSpaceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingEntranceForVehiclesRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link SiteRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link SiteComponentRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link QuayReference }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceSpaceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ParkingRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestVehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link StopPlaceVehicleEntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link EntranceRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link PointOfInterestSpaceRefStructure }{@code >}
     *     
     */
    public void setSiteElementRef(JAXBElement<? extends SiteElementRefStructure> value) {
        this.siteElementRef = value;
    }

}
