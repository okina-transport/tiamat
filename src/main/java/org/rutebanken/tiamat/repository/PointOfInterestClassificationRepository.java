package org.rutebanken.tiamat.repository;


import org.rutebanken.tiamat.model.PointOfInterestClassification;

import java.util.Optional;


public interface PointOfInterestClassificationRepository extends PointOfInterestClassificationRepositoryCustom, EntityInVersionRepository<PointOfInterestClassification> {

    Optional<String> getClassification(String classificationName, Long parentId);

}
