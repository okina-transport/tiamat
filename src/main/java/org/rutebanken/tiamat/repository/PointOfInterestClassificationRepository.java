package org.rutebanken.tiamat.repository;


import org.rutebanken.tiamat.model.PointOfInterestClassification;


public interface PointOfInterestClassificationRepository extends PointOfInterestClassificationRepositoryCustom, EntityInVersionRepository<PointOfInterestClassification> {

    PointOfInterestClassification getOrCreateClassification(String classificationName);
    PointOfInterestClassification getOrCreateClassification(String classificationName, String parentClassificationName);
}
