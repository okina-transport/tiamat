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

public class Direction_ValueStructure
        extends TypeOfValue_VersionStructure {

    protected ExternalObjectRefStructure externalDirectionRef;
    protected DirectionTypeEnumeration directionType;
    protected DirectionRefStructure oppositeDIrectionRef;

    public ExternalObjectRefStructure getExternalDirectionRef() {
        return externalDirectionRef;
    }

    public void setExternalDirectionRef(ExternalObjectRefStructure value) {
        this.externalDirectionRef = value;
    }

    public DirectionTypeEnumeration getDirectionType() {
        return directionType;
    }

    public void setDirectionType(DirectionTypeEnumeration value) {
        this.directionType = value;
    }

    public DirectionRefStructure getOppositeDIrectionRef() {
        return oppositeDIrectionRef;
    }

    public void setOppositeDIrectionRef(DirectionRefStructure value) {
        this.oppositeDIrectionRef = value;
    }

}
