/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.importer.merging;

import org.rutebanken.tiamat.model.PointOfInterest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

/**
 * When importing site frames with the matching stops concurrently, not thread safe.
 */
@Component
@Transactional
public class TransactionalMergingPointOfInterestssImporter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalMergingPointOfInterestssImporter.class);

    private final MergingPointOfInterestImporter mergingPointOfInterestImporter;

    @Autowired
    public TransactionalMergingPointOfInterestssImporter(MergingPointOfInterestImporter mergingPointOfInterestImporter) {
        this.mergingPointOfInterestImporter = mergingPointOfInterestImporter;
    }


    public Collection<org.rutebanken.netex.model.PointOfInterest> importPointOfInterests(List<PointOfInterest> pointOfInterests, AtomicInteger created) {
        List<org.rutebanken.netex.model.PointOfInterest> createdPointOfInterests = pointOfInterests
                .stream()
                .filter(Objects::nonNull)
                .map(pointOfInterest -> {
                    org.rutebanken.netex.model.PointOfInterest importedPointOfInterest = null;
                    try {
                        importedPointOfInterest = mergingPointOfInterestImporter.importPointOfInterest(pointOfInterest);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not import stop place " + pointOfInterest, e);
                    }
                    created.incrementAndGet();
                    return importedPointOfInterest;
                })
                .filter(Objects::nonNull)
                .collect(toList());

        return distinctByIdAndHighestVersion(createdPointOfInterests);
    }

    /**
     * In order to get a distinct list over stop places, and the newest version if duplicates.
     *
     * @param pointOfInterests
     * @return unique list with stop places based on ID
     */
    public Collection<org.rutebanken.netex.model.PointOfInterest> distinctByIdAndHighestVersion(List<org.rutebanken.netex.model.PointOfInterest> pointOfInterests) {
        Map<String, org.rutebanken.netex.model.PointOfInterest> uniquePointOfInterests = new HashMap<>();
        for (org.rutebanken.netex.model.PointOfInterest pointOfInterest : pointOfInterests) {
            if (uniquePointOfInterests.containsKey(pointOfInterest.getId())) {
                org.rutebanken.netex.model.PointOfInterest existingPointOfInterest = uniquePointOfInterests.get(pointOfInterest.getId());
                long existingPointOfInterestVersion = tryParseLong(existingPointOfInterest.getVersion());
                long pointOfInterestVersion = tryParseLong(pointOfInterest.getVersion());
                if (existingPointOfInterestVersion < pointOfInterestVersion) {
                    logger.info("Returning newest version of stop place with ID {}: {}", pointOfInterest.getId(), pointOfInterest.getVersion());
                    uniquePointOfInterests.put(pointOfInterest.getId(), pointOfInterest);
                }
            } else {
                uniquePointOfInterests.put(pointOfInterest.getId(), pointOfInterest);
            }
        }
        return uniquePointOfInterests.values();
    }

    private long tryParseLong(String version) {
        try {
            return Long.parseLong(version);
        } catch (NumberFormatException | NullPointerException e) {
            return 0L;
        }
    }
}
