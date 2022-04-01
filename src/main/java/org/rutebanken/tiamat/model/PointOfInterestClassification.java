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


import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class PointOfInterestClassification
        extends PointOfInterestClassification_VersionStructure {


    @ManyToOne(fetch = FetchType.LAZY ,cascade = { CascadeType.PERSIST})
    @JoinColumn(name = "parent_id")
    private PointOfInterestClassification parent;
    private Boolean osm;
    private Boolean active;

    public PointOfInterestClassification getParent() {
        return parent;
    }



    public void setParent(PointOfInterestClassification parent) {
        this.parent = parent;
    }

    public Boolean getOsm() {
        return osm;
    }

    public void setOsm(Boolean osm) {
        this.osm = osm;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}
