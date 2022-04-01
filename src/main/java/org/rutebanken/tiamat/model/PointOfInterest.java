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
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public class PointOfInterest extends PointOfInterest_VersionStructure {

    private String zipCode;
    private String address;
    private String city;
    private String postalCode;

    @OneToMany(cascade = CascadeType.MERGE)
    private Set<PointOfInterestClassification> classifications = new HashSet<>();

    private Integer pointOfInterestFacilitySetId;

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }



    public Set<PointOfInterestClassification> getClassifications() {
        return classifications;
    }

    public void setClassifications(Set<PointOfInterestClassification> classifications) {
        this.classifications = classifications;
    }

    public Integer getPointOfInterestFacilitySetId() {
        return pointOfInterestFacilitySetId;
    }

    public void setPointOfInterestFacilitySetId(Integer pointOfInterestFacilitySetId) {
        this.pointOfInterestFacilitySetId = pointOfInterestFacilitySetId;
    }
}
