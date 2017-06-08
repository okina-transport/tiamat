package org.rutebanken.tiamat.importer.modifier.name;


import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.GroupOfEntities_VersionStructure;
import org.rutebanken.tiamat.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Move certain parts of stop place and quay name to description field.
 */
@Component
public class NameToDescriptionMover {

    private static final Logger logger = LoggerFactory.getLogger(NameToDescriptionMover.class);
    private static final Pattern pattern = Pattern.compile("\\((.{3,})\\)");

    public StopPlace updateDescriptionFromName(StopPlace stopPlace) {

        updateEntityDescriptionFromName(stopPlace);
        if(stopPlace.getQuays() != null) {
            stopPlace.getQuays().forEach(quay -> updateEntityDescriptionFromName(quay));
        }
        return stopPlace;
    }

    public void updateEntityDescriptionFromName(GroupOfEntities_VersionStructure entity) {
        if(entity.getName() != null && entity.getName().getValue() != null) {
            String name = entity.getName().getValue();

            Matcher matcher = pattern.matcher(name);

            while (matcher.find()) {
                logger.debug("Matching {}", name);
                if (matcher.groupCount() > 0) {
                    String description = matcher.group(1).trim();
                    logger.debug("Extracted description {}", description);
                    entity.setDescription(new EmbeddableMultilingualString(description));
                }
            }

            entity.getName().setValue(matcher.replaceAll("").trim());
        }
    }
}