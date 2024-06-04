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

package org.rutebanken.tiamat.importer.initial;

import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.versioning.save.QuayVersionedSaverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

@Component
@Transactional
public class ParallelInitialQuayImporter {

    @Autowired
    private QuayVersionedSaverService quayVersionedSaverService;

    @Autowired
    private NetexMapper netexMapper;

    public List<org.rutebanken.netex.model.Quay> importQuays(List<Quay> tiamatQuays, AtomicInteger quaysCreated) {

        return tiamatQuays.parallelStream()
                .filter(parking -> parking != null)
                .map(parking -> quayVersionedSaverService.saveNewVersion(parking))
                .peek(parking -> quaysCreated.incrementAndGet())
                .map(parking -> netexMapper.mapToNetexModel(parking))
                .collect(toList());
    }

}
