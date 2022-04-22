package org.rutebanken.tiamat.repository;


import org.rutebanken.tiamat.model.PointOfInterest;


import java.util.List;

import java.util.Iterator;

public interface PointOfInterestRepositoryCustom  extends DataManagedObjectStructureRepository<PointOfInterest>{
    int countResult();

    Iterator<PointOfInterest> scrollPointsOfInterest();

    void clearAllPois();
    void clearPOIForClassification(String classificationName);
    void clearPOIExceptClassification(String classificationName);
    List<PointOfInterest> findAllAndInitialize() ;
}
