package org.rutebanken.tiamat.versioning;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.rutebanken.tiamat.model.AccessibilityAssessment;
import org.rutebanken.tiamat.model.AccessibilityLimitation;
import org.rutebanken.tiamat.model.EntityInVersionStructure;
import org.rutebanken.tiamat.model.SiteElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AccessibilityAssessmentVersionResolver {

    private static final Logger logger = LoggerFactory.getLogger(AccessibilityAssessmentVersionResolver.class);

    @PersistenceContext
    private EntityManager entityManager;

    public void resolveVersions(SiteElement siteElement) {
        if (siteElement == null || siteElement.getAccessibilityAssessment() == null) {
            return;
        }
        AccessibilityAssessment accessibilityAssessment = siteElement.getAccessibilityAssessment();
        resolveVersion(accessibilityAssessment, "accessibility_assessment");

        if (accessibilityAssessment.getLimitations() != null) {
            for (AccessibilityLimitation limitation : accessibilityAssessment.getLimitations()) {
                resolveVersion(limitation, "accessibility_limitation");
            }
        }
    }

    private void resolveVersion(EntityInVersionStructure entity, String tableName) {
        if (entity == null || entity.getId() != null || entity.getNetexId() == null) {
            return;
        }

        Number maxVersion = (Number) entityManager
                .createNativeQuery("SELECT max(version) FROM " + tableName + " WHERE netex_id = :netexId")
                .setParameter("netexId", entity.getNetexId())
                .getSingleResult();

        if (maxVersion != null && entity.getVersion() <= maxVersion.longValue()) {
            long newVersion = maxVersion.longValue() + 1;
            logger.info("{} {} version {} already exists in database, using version {}",
                    entity.getClass().getSimpleName(), entity.getNetexId(), entity.getVersion(), newVersion);
            entity.setVersion(newVersion);
        }
    }
}
