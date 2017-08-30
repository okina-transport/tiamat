package org.rutebanken.tiamat.versioning.util;

import org.rutebanken.tiamat.model.DataManagedObjectStructure;
import org.rutebanken.tiamat.model.VersionOfObjectRefStructure;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Component
public class ReferenceVersionUpdater {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceVersionUpdater.class);

    @Autowired
    private ReferenceResolver referenceResolver;

    public <T extends VersionOfObjectRefStructure> Set<T> updateReferencesToNewestVersion(Set<T> listOfRefs, Class<T> clazz) {

        if(listOfRefs == null) {
            return null;
        }

        return listOfRefs.stream()
                .map(ref -> {
                    logger.debug("Updating reference to newest version {}", ref);
                    ref.setVersion(null);
                    DataManagedObjectStructure dataManagedObjectStructure = referenceResolver.resolve(ref);
                    try {
                        T newRef = clazz.newInstance();
                        newRef.setRef(dataManagedObjectStructure.getNetexId());
                        newRef.setVersion(String.valueOf(dataManagedObjectStructure.getVersion()));
                        return newRef;
                    } catch (InstantiationException|IllegalAccessException e) {
                        throw new RuntimeException("Cannot create new instance", e);
                    }
                })
                .collect(toSet());
    }

}
