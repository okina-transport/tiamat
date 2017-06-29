package org.rutebanken.tiamat.repository;

import com.vividsolutions.jts.geom.Envelope;
import org.rutebanken.tiamat.exporter.params.ParkingSearch;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface ParkingRepositoryCustom extends DataManagedObjectStructureRepository<Parking>  {

    String findFirstByKeyValues(String key, Set<String> value);

    Iterator<Parking> scrollParkings(Set<Long> stopPlaceIds);

    int countResult(ParkingSearch parkingSearch);

    int countResult(Set<Long> stopPlaceIds);

    String findNearbyParking(Envelope boundingBox, String value, ParkingTypeEnumeration parkingType);

    Iterator<Parking> scrollParkings();

    Iterator<Parking> scrollParkings(ParkingSearch parkingSearch);

    /**
     * Find parkings that belong to StopPlace
     * @param netexStopPlaceId
     * @return list of parkings referencing to stopPlace
     */
    List<String> findByStopPlaceNetexId(String netexStopPlaceId);
}

