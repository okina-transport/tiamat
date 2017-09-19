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

public class Country_VersionStructure
        extends Place {

    protected PrivateCodeStructure uicCode;
    protected MultilingualStringEntity countryName;
    protected AlternativeNames_RelStructure alternativeNames;

    public PrivateCodeStructure getUicCode() {
        return uicCode;
    }

    public void setUicCode(PrivateCodeStructure value) {
        this.uicCode = value;
    }

    public MultilingualStringEntity getCountryName() {
        return countryName;
    }

    public void setCountryName(MultilingualStringEntity value) {
        this.countryName = value;
    }

    public AlternativeNames_RelStructure getAlternativeNames() {
        return alternativeNames;
    }

    public void setAlternativeNames(AlternativeNames_RelStructure value) {
        this.alternativeNames = value;
    }

}
