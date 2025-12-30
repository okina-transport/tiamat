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

package org.rutebanken.tiamat.repository;

import org.rutebanken.tiamat.model.InstalledEquipment_VersionStructure;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.Set;

@Repository
@Transactional
public class ParkingInstalledEquipmentsRepositoryImpl implements ParkingInstalledEquipmentsRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public String findFirstByKeyValues(String key, Set<String> values) {
        return null;
    }

    @Override
    public InstalledEquipment_VersionStructure findInstalledEquipmentByNetexId(String netexId) {
        String queryString = "SELECT * FROM installed_equipment_version_structure WHERE netex_id = :name";
        Query query = entityManager.createNativeQuery(queryString, InstalledEquipment_VersionStructure.class);
        query.setParameter("name", netexId);
        return (InstalledEquipment_VersionStructure) query.getSingleResult();
    }
}
