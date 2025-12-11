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

import org.locationtech.jts.geom.Envelope;
import org.rutebanken.tiamat.dtoassembling.dto.IdMappingDto;
import org.rutebanken.tiamat.dtoassembling.dto.JbvCodeMappingDto;
import org.rutebanken.tiamat.model.Quay;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface QuayRepositoryCustom extends DataManagedObjectStructureRepository<Quay> {

    List<IdMappingDto> findKeyValueMappingsForQuay(Instant validFrom, Instant validTo, int recordPosition, int recordsPerRoundTrip);

    Set<String> findUniqueQuayIds(Instant validFrom, Instant validTo);

    List<JbvCodeMappingDto> findJbvCodeMappingsForQuay();

    String findImportedIdByNetexIdQuay(Long netexId);

    List<String> searchByKeyValue(String key, String value);

    Set<String> findByKeyValues(String key, Set<String> values, boolean exactMatch);

    List<Quay> findAllByImportedId(String importedId);

    List<Quay> findAllLatestVersionByNetexId(List<String> netexIdentifiers);

    Optional<Quay> findActiveQuayForImportedId(String importedId);

    List<JbvCodeMappingDto> findIdMappingsForQuay();

    List<JbvCodeMappingDto> findSelectedIdMappingsForQuay();

    Optional<Quay> findTADQuay(String netexId) ;

    Set<String> findByKeyValues(String key, Set<String> values);

    String findNearbyQuay(Envelope envelope, String name, String publicCode);

    Optional<Quay> findQuayByMdmId(Long mdmId);

}
