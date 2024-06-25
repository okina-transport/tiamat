package org.rutebanken.tiamat.repository;


import org.locationtech.jts.geom.Envelope;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.PointOfInterestClassification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface PointOfInterestRepositoryCustom extends DataManagedObjectStructureRepository<PointOfInterest> {
    int countResult();

    Set<Long> scrollPointsOfInterest();

    void clearAllPois();

    void clearPOIForClassification(String classificationName);

    void clearPOIExceptClassification(String classificationName);

    List<PointOfInterest> findAllAndInitialize();

    void initExportJobTable(Long exportJobId);

    int countPOIInExport(Long exportJobId);

    List<PointOfInterest> getPOIInitializedForExport(Set<Long> poiIds);

    Set<Long> getNextBatchToProcess(Long exportJobId);

    void deleteProcessedIds(Long exportJobId, Set<Long> processedPoi);

    PointOfInterest findFirstByNetexIdOrderByVersionDescAndInitialize(String netexId);

    Page<PointOfInterest> findNearbyPOI(Envelope boundingBox, String name, String ignorePointOfInterestId, Pageable pageable);

    String findNearbyPOI(Envelope envelope, String name);

    Page<PointOfInterest> findByName(String query, Pageable pageable);

    Page<PointOfInterest> findByClassifications(List<String> classifications, Pageable pageable);

    List<PointOfInterest> getAllPOIWithoutPostcode();

}
