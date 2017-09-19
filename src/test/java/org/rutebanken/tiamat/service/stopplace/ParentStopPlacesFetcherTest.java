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

package org.rutebanken.tiamat.service.stopplace;

import org.junit.Test;
import org.rutebanken.tiamat.model.EntityInVersionStructure;
import org.rutebanken.tiamat.model.SiteRefStructure;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.stopplace.ParentStopPlacesFetcher;

import java.util.Arrays;
import java.util.List;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParentStopPlacesFetcherTest {

    private StopPlaceRepository stopPlaceRepository = mock(StopPlaceRepository.class);

    private ParentStopPlacesFetcher parentStopPlacesFetcher = new ParentStopPlacesFetcher(stopPlaceRepository);

    @Test
    public void resolveParents() throws Exception {

        int counter = 0;
        StopPlace parent = createAndMockStopPlaceWithNetexIdAndVersion(++counter);
        parent.setParentStopPlace(true);
        StopPlace parentSecondVersion = new StopPlace();
        parentSecondVersion.setParentStopPlace(true);
        parentSecondVersion.setNetexId(parent.getNetexId());
        parentSecondVersion.setVersion(2L);

        StopPlace child1 = createAndMockStopPlaceWithNetexIdAndVersion(++counter);
        child1.setParentStopPlace(false);
        addParentRef(child1, parent);
        StopPlace child2 = createAndMockStopPlaceWithNetexIdAndVersion(++counter);
        child2.setParentStopPlace(false);
        addParentRef(child2, parent);

        List<StopPlace> result = parentStopPlacesFetcher.resolveParents(Arrays.asList(parent, parentSecondVersion, child1, child2), false);

        assertThat(result).extracting(this::concatenateNetexIdVersion)
                .as("parent first version should be kept")
                .contains(concatenateNetexIdVersion(parent));
        assertThat(result).extracting(this::concatenateNetexIdVersion)
                .as("parent second version should be kept")
                .contains(concatenateNetexIdVersion(parentSecondVersion));
        assertThat(result).extracting(stopPlace -> stopPlace.getNetexId()).doesNotContain(child1.getNetexId());
        assertThat(result).extracting(stopPlace -> stopPlace.getNetexId()).doesNotContain(child2.getNetexId());
    }

    @Test
    public void resolveParentsKeepChilds() throws Exception {

        int counter = 10;
        StopPlace parent = createAndMockStopPlaceWithNetexIdAndVersion(++counter);
        parent.setParentStopPlace(true);

        StopPlace parentSecondVersion = new StopPlace();
        parentSecondVersion.setParentStopPlace(true);
        parentSecondVersion.setNetexId(parent.getNetexId());
        parentSecondVersion.setVersion(2L);

        StopPlace child1 = createAndMockStopPlaceWithNetexIdAndVersion(++counter);
        child1.setParentStopPlace(false);
        addParentRef(child1, parent);

        StopPlace child2 = createAndMockStopPlaceWithNetexIdAndVersion(++counter);
        child2.setParentStopPlace(false);
        addParentRef(child2, parent);

        List<StopPlace> result = parentStopPlacesFetcher.resolveParents(Arrays.asList(parent, parentSecondVersion, child1, child2), true);

        assertThat(result).extracting(this::concatenateNetexIdVersion)
                .as("parent first version should be kept")
                .contains(concatenateNetexIdVersion(parent));
        assertThat(result).extracting(this::concatenateNetexIdVersion)
                .as("parent second version should be kept")
                .contains(concatenateNetexIdVersion(parentSecondVersion));
        assertThat(result).extracting(stopPlace -> stopPlace.getNetexId()).contains(child1.getNetexId());
        assertThat(result).extracting(stopPlace -> stopPlace.getNetexId()).contains(child2.getNetexId());
    }

    private void addParentRef(StopPlace child, StopPlace parent) {
        child.setParentSiteRef(new SiteRefStructure(parent.getNetexId(), String.valueOf(parent.getVersion())));
    }

    private String concatenateNetexIdVersion(EntityInVersionStructure entity) {
        return entity.getVersion()+entity.getNetexId();
    }

    private StopPlace createAndMockStopPlaceWithNetexIdAndVersion(int counter) {

        StopPlace stopPlace = new StopPlace();
        stopPlace.setNetexId("XYZ:StopPlace:"+counter);
        stopPlace.setVersion(1L);
        when(stopPlaceRepository.findFirstByNetexIdAndVersion(stopPlace.getNetexId(), stopPlace.getVersion())).thenReturn(stopPlace);
        return stopPlace;
    }

}