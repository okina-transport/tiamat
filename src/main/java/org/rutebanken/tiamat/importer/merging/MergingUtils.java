package org.rutebanken.tiamat.importer.merging;

import org.rutebanken.tiamat.importer.KeyValueListAppender;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.versioning.save.AccessibilityVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;

@Component
@Qualifier("mergingUtils")
@Transactional
public class MergingUtils {

    private static final Logger logger = LoggerFactory.getLogger(MergingUtils.class);

    private final KeyValueListAppender keyValueListAppender;
    private final AccessibilityVersionedSaverService accessibilityVersionedSaverService;


    public MergingUtils(KeyValueListAppender keyValueListAppender, AccessibilityVersionedSaverService accessibilityVersionedSaverService) {
        this.keyValueListAppender = keyValueListAppender;
        this.accessibilityVersionedSaverService = accessibilityVersionedSaverService;
    }

    <T> boolean updateProperty(T existingValue, T incomingValue, Consumer<T> setter, String currentField, String netexId) {
        if (((existingValue == null && incomingValue != null) || (existingValue != null && incomingValue != null)) && !Objects.equals(existingValue, incomingValue)) {
            setter.accept(incomingValue);
            logger.info("Updated {} for {}", currentField, netexId);
            return true;
        }
        return false;
    }

    public boolean updateKeyValues(DataManagedObjectStructure existing, DataManagedObjectStructure incoming, String netexId) {
        if ((existing.getKeyValues() == null && incoming.getKeyValues() != null) ||
                (existing.getKeyValues() != null && incoming.getKeyValues() != null &&
                        !existing.getKeyValues().equals(incoming.getKeyValues()))) {

            // Suppression des clés qui ne sont plus présentes dans incoming
            Iterator<Map.Entry<String, Value>> iterator = existing.getKeyValues().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Value> entry = iterator.next();
                if (!incoming.getKeyValues().containsKey(entry.getKey())) {
                    iterator.remove();
                    logger.info("Removed key value for {}", netexId);
                }
            }

            // Ajout/Mise à jour des nouvelles clés-valeurs
            for (Map.Entry<String, Value> entry : incoming.getKeyValues().entrySet()) {
                keyValueListAppender.appendKeyValue(entry.getKey(), incoming, existing);
            }
            logger.info("Updated key values for {}", netexId);
            return true;
        }
        return false;
    }
    
    public boolean updateAccessibilityAccessment(SiteElement existing, SiteElement incoming, String netexId) {
        List<AccessibilityLimitation> accessibilityLimitations = new ArrayList<>();

        if ((existing.getAccessibilityAssessment() == null && incoming.getAccessibilityAssessment() != null) ||
                (existing.getAccessibilityAssessment() != null && incoming.getAccessibilityAssessment() != null &&
                        !existing.getAccessibilityAssessment().equals(incoming.getAccessibilityAssessment()))) {
            existing.setAccessibilityAssessment(accessibilityVersionedSaverService.saveNewVersionAssessment(incoming.getAccessibilityAssessment()));
            logger.info("Updated accessibility assessment for {}", netexId);

            if (!existing.getAccessibilityAssessment().getLimitations().equals(incoming.getAccessibilityAssessment().getLimitations())) {
                existing.getAccessibilityAssessment().getLimitations().clear();
                for (AccessibilityLimitation limitation : incoming.getAccessibilityAssessment().getLimitations()) {
                    accessibilityLimitations.add(accessibilityVersionedSaverService.saveNewVersionLimitation(limitation));
                }
                existing.getAccessibilityAssessment().getLimitations().addAll(accessibilityLimitations);
                logger.info("Updated accessibility limitations for {}", netexId);
            }
            return true;
        }
        return false;
    }

}
