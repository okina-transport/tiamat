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
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a list of references to an  NeTEx Object.
 * 
 * <p>Java class for objectRefs_RelStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="objectRefs_RelStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}oneToManyRelationshipStructure">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.netex.org.uk/netex}VersionOfObjectRef" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "objectRefs_RelStructure", propOrder = {
    "versionOfObjectRef"
})
public class ObjectRefs_RelStructure
    extends OneToManyRelationshipStructure
{

    @XmlElementRef(name = "VersionOfObjectRef", namespace = "http://www.netex.org.uk/netex", type = JAXBElement.class)
    protected List<JAXBElement<? extends VersionOfObjectRefStructure>> versionOfObjectRef;

    public List<JAXBElement<? extends VersionOfObjectRefStructure>> getVersionOfObjectRef() {
        if (versionOfObjectRef == null) {
            versionOfObjectRef = new ArrayList<JAXBElement<? extends VersionOfObjectRefStructure>>();
        }
        return this.versionOfObjectRef;
    }

}
