package org.rutebanken.tiamat.repository;


import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.PointOfInterestClassification;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface PointOfInterestRepositoryCustom extends DataManagedObjectStructureRepository<PointOfInterest> {
    int countResult();

    Iterator<PointOfInterest> scrollPointsOfInterest();

    void clearAllPois();

    void clearPOIForClassification(String classificationName);

    void clearPOIExceptClassification(String classificationName);

    List<PointOfInterest> findAllAndInitialize();

    void initExportJobTable(Long exportJobId);

    int countPOIInExport(Long exportJobId);

    List<PointOfInterest> getPOIInitializedForExport(Set<Long> poiIds);

    Set<Long> getNextBatchToProcess(Long exportJobId);

    void deleteProcessedIds(Long exportJobId, Set<Long> processedPoi);

}
