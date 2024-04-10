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

import org.rutebanken.tiamat.model.DataManagedObjectStructure;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.rutebanken.tiamat.versioning.save.PointOfInterestVersionedSaverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

@Component
@Transactional
public class ParallelInitialPointOfInterestImporter {

    @Autowired
    private PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService;

    @Autowired
    private ReferenceResolver referenceResolver;

    @Autowired
    private NetexMapper netexMapper;

    public List<org.rutebanken.netex.model.PointOfInterest> importPointOfInterests(List<PointOfInterest> tiamatPointOfInterests, AtomicInteger created) {

        return tiamatPointOfInterests.parallelStream()
                .filter(pointOfInterest -> pointOfInterest != null)
                .map(pointOfInterest -> {
                    if (pointOfInterest.getParentSiteRef() != null) {
                        DataManagedObjectStructure referencedStopPlace = referenceResolver.resolve(pointOfInterest.getParentSiteRef());
                        pointOfInterest.getParentSiteRef().setRef(referencedStopPlace.getNetexId());
                    }
                    return pointOfInterest;
                })
                .map(pointOfInterest -> pointOfInterestVersionedSaverService.saveNewVersion(pointOfInterest))
                .peek(pointOfInterest -> created.incrementAndGet())
                .map(pointOfInterest -> netexMapper.mapToNetexModel(pointOfInterest))
                .collect(toList());
    }

}
