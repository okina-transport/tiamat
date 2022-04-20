package org.rutebanken.tiamat.repository;



import org.rutebanken.tiamat.model.PointOfInterestClassification;

import java.util.Iterator;

public interface PointOfInterestClassificationRepositoryCustom extends DataManagedObjectStructureRepository<PointOfInterestClassification>{

    int countResult();
    Iterator<PointOfInterestClassification> scrollPointOfInterestClassifications();

}
