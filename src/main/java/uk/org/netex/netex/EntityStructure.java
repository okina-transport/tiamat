//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.06 at 10:37:32 AM CET 
//


package uk.org.netex.netex;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;


/**
 * Type for ENTITY.
 * 
 * <p>Java class for EntityStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EntityStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="nameOfClass" type="{http://www.netex.org.uk/netex}NameOfClass" />
 *       &lt;attribute name="id" type="{http://www.netex.org.uk/netex}ObjectIdType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EntityStructure")
@XmlSeeAlso({
    CodespaceStructure.class,
    EntityInVersionStructure.class
})
@MappedSuperclass
public abstract class EntityStructure implements Serializable{

 /*   @XmlAttribute(name = "nameOfClass")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public String nameOfClass;
*/
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String id;

    /**
     * Gets the value of the nameOfClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
  /*  public String getNameOfClass() {
        return nameOfClass;
    }*/

    /**
     * Sets the value of the nameOfClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
  /*  public void setNameOfClass(String nameOfClass) {
        this.nameOfClass = nameOfClass;
    }*/

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
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
    public void setId(String id) {
        this.id = id;
    }

}
