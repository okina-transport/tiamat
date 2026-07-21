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
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.rest.dto.DTOClusterMarker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ParkingRepositoryCustom extends DataManagedObjectStructureRepository<Parking>  {

    String findFirstByKeyValues(String key, Set<String> value);

    Iterator<Parking> scrollParkings(Set<Long> stopPlaceIds);

    int countResult(Set<Long> stopPlaceIds);

    int countResult();

    Page<Parking> findNearbyParking(Envelope boundingBox, String name, ParkingTypeEnumeration parkingType, String ignoreParkingId, Pageable pageable);

    String findNearbyParking(Envelope boundingBox, String name, ParkingTypeEnumeration parkingType);

    Set<Long> scrollParkings();

    Page<Parking> findByName(String query, Pageable pageable);

    /**
     * Find parkings that belong to StopPlace
     * @param netexStopPlaceId
     * @return list of parkings referencing to stopPlace
     */
    List<String> findByStopPlaceNetexId(String netexStopPlaceId);

    void initExportJobTable( Long exportJobId);

    List<Parking> getParkingsInitializedForExport(Set<Long> parkingIds) ;

    Optional<Parking> findByIdLocAndOsm(String idLoc, String idOsm);

    List<Parking> findAllParkingsLastVersionAndValid();

    List<DTOClusterMarker> findClusterMarkers();

    Integer findByIdLocForOtherParking(String idLoc, String netexId);

    List<Parking> getAllParkingsWithoutInsee();

    Set<String> findNetexIdsByPlaceEquipmentId(String placeEquipmentNetexId);
}

