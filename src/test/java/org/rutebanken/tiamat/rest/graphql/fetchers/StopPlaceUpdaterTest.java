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

package org.rutebanken.tiamat.rest.graphql.fetchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.changelog.LoggingService;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.lock.MutateLock;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.rest.graphql.mappers.StopPlaceMapper;
import org.rutebanken.tiamat.service.Renamer;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Regression tests for the NullPointerException fixed in
 * {@code doesExistsInSameStoplace} when creating a new StopPlace
 * (existingStopPlace == null) whose incoming quays carry an imported-id.
 */
class StopPlaceUpdaterTest {

    private StopPlaceUpdater stopPlaceUpdater;

    @BeforeEach
    void setUp() {
        stopPlaceUpdater = new StopPlaceUpdater(
                mock(StopPlaceVersionedSaverService.class),
                mock(StopPlaceRepository.class),
                mock(StopPlaceMapper.class),
                mock(MutateLock.class),
                mock(VersionCreator.class),
                mock(QuayRepository.class),
                mock(Renamer.class),
                mock(MdmService.class),
                mock(LoggingService.class),
                mock(UsernameFetcher.class));
    }

    @Test
    void doesExistsInSameStoplace_existingStopPlaceNull_returnsFalse() throws Exception {
        Quay currentQuay = new Quay();
        currentQuay.setNetexId("MOBIITI:Quay:1");

        boolean result = invokeDoesExistsInSameStoplace("PROV1:Quay:1", currentQuay, null);

        assertThat(result).isFalse();
    }

    @Test
    void doesExistsInSameStoplace_originalIdBelongsToSameQuayInExistingStopPlace_returnsTrue() throws Exception {
        Quay currentQuay = new Quay();
        currentQuay.setNetexId("MOBIITI:Quay:1");
        currentQuay.getOriginalIds().add("PROV1:Quay:1");

        StopPlace existingStopPlace = new StopPlace();
        existingStopPlace.getQuays().add(currentQuay);

        boolean result = invokeDoesExistsInSameStoplace("PROV1:Quay:1", currentQuay, existingStopPlace);

        assertThat(result).isTrue();
    }

    @Test
    void doesExistsInSameStoplace_originalIdBelongsToDifferentQuayInExistingStopPlace_returnsFalse() throws Exception {
        Quay otherQuay = new Quay();
        otherQuay.setNetexId("MOBIITI:Quay:1");
        otherQuay.getOriginalIds().add("PROV1:Quay:1");

        Quay currentQuay = new Quay();
        currentQuay.setNetexId("MOBIITI:Quay:2");

        StopPlace existingStopPlace = new StopPlace();
        existingStopPlace.getQuays().add(otherQuay);

        boolean result = invokeDoesExistsInSameStoplace("PROV1:Quay:1", currentQuay, existingStopPlace);

        assertThat(result).isFalse();
    }

    @Test
    void doesExistsInSameStoplace_originalIdNotFoundInExistingStopPlace_returnsFalse() throws Exception {
        Quay currentQuay = new Quay();
        currentQuay.setNetexId("MOBIITI:Quay:2");

        StopPlace existingStopPlace = new StopPlace();

        boolean result = invokeDoesExistsInSameStoplace("PROV1:Quay:1", currentQuay, existingStopPlace);

        assertThat(result).isFalse();
    }

    private boolean invokeDoesExistsInSameStoplace(String originalId, Quay currentQuay, StopPlace existingStopPlace) throws Exception {
        Method method = StopPlaceUpdater.class.getDeclaredMethod("doesExistsInSameStoplace", String.class, Quay.class, StopPlace.class);
        method.setAccessible(true);
        return (boolean) method.invoke(stopPlaceUpdater, originalId, currentQuay, existingStopPlace);
    }
}