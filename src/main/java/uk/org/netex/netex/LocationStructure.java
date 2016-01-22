//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import net.opengis.gml._3.DirectPositionType;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.math.BigDecimal;


/**
 * Type for geospatial Position of a point.
 * May be expressed in concrete WGS 84 Coordinates or any gml compatible point coordinates format.
 * 
 * <p>Java class for LocationStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LocationStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;sequence>
 *           &lt;sequence minOccurs="0">
 *             &lt;element name="Longitude" type="{http://www.netex.org.uk/netex}LongitudeType"/>
 *             &lt;element name="Latitude" type="{http://www.netex.org.uk/netex}LatitudeType"/>
 *             &lt;element name="Altitude" type="{http://www.netex.org.uk/netex}AltitudeType" minOccurs="0"/>
 *           &lt;/sequence>
 *           &lt;element ref="{http://www.opengis.net/gml/3.2}pos" minOccurs="0"/>
 *         &lt;/sequence>
 *         &lt;element name="Precision" type="{http://www.netex.org.uk/netex}DistanceType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}normalizedString" />
 *       &lt;attribute name="srsName" type="{http://www.netex.org.uk/netex}SrsNameType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LocationStructure", propOrder = {
    "longitude",
    "latitude",
    "altitude",
    "pos",
    "precision"
})
@Entity
@Table(name = "location")
public class LocationStructure {

    private static final int SRID = 4326;


    @XmlElement(name = "Altitude")
    @Transient
    protected BigDecimal altitude;

    @XmlElement(namespace = "http://www.opengis.net/gml/3.2")
    @Transient
    protected DirectPositionType pos;

    @XmlElement(name = "Precision")
    @Transient
    protected BigDecimal precision;

    @Id
    @GeneratedValue
    // TODO: Use String as type for 'id'. Got this using String: org.h2.jdbc.JdbcSQLException: Hexadecimal string contains non-hex character.
   // @GenericGenerator(name = "uuid", strategy = "uuid2")
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    @XmlTransient
    protected long id;

    @XmlAttribute(name = "srsName")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    @Transient
    protected String srsName;

    @XmlTransient
    @Type(type="org.hibernate.spatial.GeometryType")
    private Point geometryPoint = null;

    /**
     * TODO: Do not require geometry factory here.
     * See also GeometryFactory
     */
    @XmlTransient
    private static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);

    public LocationStructure(Point geometryPoint) {
        this.geometryPoint = geometryPoint;
    }

    public LocationStructure() {
    }


    /**
     * Gets the value of the longitude property.
     * TODO: Find a more elegant solution to geometry point
     * Allow this class to be closer to a java bean with minimal logic.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    @XmlElement(name = "Longitude")
    public BigDecimal getLongitude() {
        if(geometryPoint != null) {
            return new BigDecimal(String.valueOf(geometryPoint.getX()));
        }
        return null;
    }

    /**
     * Sets the value of the longitude property.
     * TODO: Not use the geometry factory in this class.
     * 
     * @param longitude
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    @XmlElement(name = "Longitude")
    public void setLongitude(BigDecimal longitude) {
        double latitude;
        if(geometryPoint != null) {
            latitude = geometryPoint.getY();
        } else {
            latitude = 0;
        }
        geometryPoint = geometryFactory.createPoint(new Coordinate(longitude.doubleValue(), latitude));
    }

    /**
     * Gets the value of the latitude property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    @XmlElement(name = "Latitude")
    public BigDecimal getLatitude() {
        if (geometryPoint != null) {
            return new BigDecimal(String.valueOf(geometryPoint.getY()));
        }
        return null;
    }

    /**
     * Sets the value of the latitude property.
     * 
     * @param latitude
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    @XmlElement(name = "Latitude")
    public void setLatitude(BigDecimal latitude) {
        double longitude;
        if(geometryPoint != null) {
            longitude = geometryPoint.getX();
        } else {
            longitude = 0;
        }
        geometryPoint = geometryFactory.createPoint(new Coordinate(longitude, latitude.doubleValue()));
    }

    /**
     * Gets the value of the altitude property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getAltitude() {
        return altitude;
    }

    /**
     * Sets the value of the altitude property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setAltitude(BigDecimal value) {
        this.altitude = value;
    }

    /**
     * Gets the value of the pos property.
     * 
     * @return
     *     possible object is
     *     {@link DirectPositionType }
     *     
     */
    public DirectPositionType getPos() {
        return pos;
    }

    /**
     * Sets the value of the pos property.
     * 
     * @param value
     *     allowed object is
     *     {@link DirectPositionType }
     *     
     */
    public void setPos(DirectPositionType value) {
        this.pos = value;
    }

    /**
     * Gets the value of the precision property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getPrecision() {
        return precision;
    }

    /**
     * Sets the value of the precision property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setPrecision(BigDecimal value) {
        this.precision = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(long value) {
        this.id = value;
    }

    /**
     * Gets the value of the srsName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSrsName() {
        return srsName;
    }

    /**
     * Sets the value of the srsName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSrsName(String value) {
        this.srsName = value;
    }

    public Point getGeometryPoint() {
        return geometryPoint;
    }

    public void setGeometryPoint(Point geometryPoint) {
        this.geometryPoint = geometryPoint;
    }
}
