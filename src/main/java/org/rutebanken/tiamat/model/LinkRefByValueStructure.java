/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.model;

import javax.xml.datatype.XMLGregorianCalendar;


public class LinkRefByValueStructure {

    protected String nameOfClass;
    protected String fromPointRef;
    protected String toPointRef;
    protected String nameOfPointRefClass;
    protected String typeOfLinkRef;
    protected XMLGregorianCalendar created;
    protected XMLGregorianCalendar changed;
    protected String version;

    public String getNameOfClass() {
        return nameOfClass;
    }

    public void setNameOfClass(String value) {
        this.nameOfClass = value;
    }

    public String getFromPointRef() {
        return fromPointRef;
    }

    public void setFromPointRef(String value) {
        this.fromPointRef = value;
    }

    public String getToPointRef() {
        return toPointRef;
    }

    public void setToPointRef(String value) {
        this.toPointRef = value;
    }

    public String getNameOfPointRefClass() {
        return nameOfPointRefClass;
    }

    public void setNameOfPointRefClass(String value) {
        this.nameOfPointRefClass = value;
    }

    public String getTypeOfLinkRef() {
        return typeOfLinkRef;
    }

    public void setTypeOfLinkRef(String value) {
        this.typeOfLinkRef = value;
    }

    public XMLGregorianCalendar getCreated() {
        return created;
    }

    public void setCreated(XMLGregorianCalendar value) {
        this.created = value;
    }

    public XMLGregorianCalendar getChanged() {
        return changed;
    }

    public void setChanged(XMLGregorianCalendar value) {
        this.changed = value;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String value) {
        this.version = value;
    }

}
