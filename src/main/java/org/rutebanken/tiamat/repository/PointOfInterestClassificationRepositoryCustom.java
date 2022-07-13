package org.rutebanken.tiamat.repository;



import org.rutebanken.tiamat.model.PointOfInterestClassification;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface PointOfInterestClassificationRepositoryCustom extends DataManagedObjectStructureRepository<PointOfInterestClassification>{

    int countResult();
    Iterator<PointOfInterestClassification> scrollPointOfInterestClassifications();

    List<PointOfInterestClassification> getPOIClassificationInitializedForExport(Set<Long> poiIds);

    Set<Long> getNextBatchToProcess(Long exportJobId);

    void deleteProcessedIds(Long exportJobId, Set<Long> processedPoi);

}
