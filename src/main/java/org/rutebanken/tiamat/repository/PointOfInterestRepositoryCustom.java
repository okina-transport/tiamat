package org.rutebanken.tiamat.repository;


import org.rutebanken.tiamat.model.PointOfInterest;


import java.util.List;

public interface PointOfInterestRepositoryCustom  extends DataManagedObjectStructureRepository<PointOfInterest>{

    void clearAllPois();
    void clearPOIForClassification(String classificationName);
    void clearPOIExceptClassification(String classificationName);
    List<PointOfInterest> findAllAndInitialize() ;
}
