
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

import org.rutebanken.tiamat.model.Quay;
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
public class TransactionalMergingQuaysImporter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalMergingQuaysImporter.class);

    private final MergingQuayImporter mergingQuayImporter;

    @Autowired
    public TransactionalMergingQuaysImporter(MergingQuayImporter mergingQuayImporter) {
        this.mergingQuayImporter = mergingQuayImporter;
    }

    public Collection<org.rutebanken.netex.model.Quay> importQuays(List<Quay> quays) {

        List<org.rutebanken.netex.model.Quay> createdQuays = quays
                .stream()
                .filter(Objects::nonNull)
                .map(quay -> {
                    org.rutebanken.netex.model.Quay importedQuay = null;
                    try {
                        importedQuay = mergingQuayImporter.importQuay(quay);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not import quay " + quay, e);
                    }
                    return importedQuay;
                })
                .filter(Objects::nonNull)
                .collect(toList());

        return distinctByIdAndHighestVersion(createdQuays);
    }

    /**
     * In order to get a distinct list over quays, and the newest version if duplicates.
     *
     * @param quays
     * @return unique list with quays based on ID
     */
    public Collection<org.rutebanken.netex.model.Quay> distinctByIdAndHighestVersion(List<org.rutebanken.netex.model.Quay> quays) {
        Map<String, org.rutebanken.netex.model.Quay> uniqueQuays = new HashMap<>();
        for (org.rutebanken.netex.model.Quay quay : quays) {
            if (uniqueQuays.containsKey(quay.getId())) {
                org.rutebanken.netex.model.Quay existingQuay = uniqueQuays.get(quay.getId());
                long existingQuayVersion = tryParseLong(existingQuay.getVersion());
                long quayVersion = tryParseLong(quay.getVersion());
                if (existingQuayVersion < quayVersion) {
                    logger.info("Returning newest version of quay with ID {}: {}", quay.getId(), quay.getVersion());
                    uniqueQuays.put(quay.getId(), quay);
                }
            } else {
                uniqueQuays.put(quay.getId(), quay);
            }
        }
        return uniqueQuays.values();
    }

    private long tryParseLong(String version) {
        try {
            return Long.parseLong(version);
        } catch (NumberFormatException | NullPointerException e) {
            return 0L;
        }
    }


}
