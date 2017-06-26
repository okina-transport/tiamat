package org.rutebanken.tiamat.repository;

import com.vividsolutions.jts.geom.Envelope;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface ParkingRepositoryCustom extends DataManagedObjectStructureRepository<Parking>  {

    String findFirstByKeyValues(String key, Set<String> value);

    String findNearbyParking(Envelope boundingBox, String value, ParkingTypeEnumeration parkingType);

    Iterator<Parking> scrollParkings();

    Iterator<Parking> scrollParkings(List<String> parkingNetexIds);

    /**
     * Find parkings that belong to StopPlace
     * @param netexStopPlaceId
     * @return list of parkings referencing to stopPlace
     */
    List<String> findByStopPlaceNetexId(String netexStopPlaceId);
}

