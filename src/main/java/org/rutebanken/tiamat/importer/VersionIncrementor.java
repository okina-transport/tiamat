package org.rutebanken.tiamat.importer;

import org.rutebanken.tiamat.model.EntityInVersionStructure;
import org.rutebanken.tiamat.model.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VersionIncrementor {

    private static final Logger logger = LoggerFactory.getLogger(VersionIncrementor.class);

    public void incrementVersion(EntityInVersionStructure versionedEntity) {
        Long version = versionedEntity.getVersion();

        if(version == -1L) {
            version = 1L;
        } else {
            version ++;
        }

        logger.debug("Setting version {} for {}", version, versionedEntity);
        versionedEntity.setVersion(version);
    }

}
