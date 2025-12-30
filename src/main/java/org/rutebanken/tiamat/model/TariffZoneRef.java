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

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class TariffZoneRef extends ZoneRefStructure {

    @Id
    @GeneratedValue(generator = "sequence_per_table_generator")
    private Long id;

    public TariffZoneRef() {
    }

    public TariffZoneRef(TariffZone tariffZone) {
        this.setRef(tariffZone.getNetexId());
        this.setVersion(String.valueOf(tariffZone.getVersion()));
    }

    public TariffZoneRef(String netexId) {
        this.setRef(netexId);
    }
}
