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

import org.junit.jupiter.api.Test;
import org.rutebanken.helper.organisation.ReflectionAuthorizationService;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.changelog.LoggingService;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.lock.MutateLock;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Arrays;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class StopPlaceDeleterTest {

    private final StopPlaceQuayDeleterToChouette stopPlaceQuayDeleterToChouette = mock(StopPlaceQuayDeleterToChouette.class);
    private StopPlaceRepository stopPlaceRepository = mock(StopPlaceRepository.class);
    private ReflectionAuthorizationService authorizationService = mock(ReflectionAuthorizationService.class);
    private UsernameFetcher usernameFetcher = mock(UsernameFetcher.class);
    private MdmService mdmServiceMock = mock(MdmService.class);
    private LoggingService loggingService = mock(LoggingService.class);
    private MutateLock mutateLock = new MutateLock(null) {
        @Override
        public <T> T executeInLock(Supplier<T> supplier) {
            return supplier.get();
        }
    };
    private StopPlaceDeleter stopPlaceDeleter = new StopPlaceDeleter(stopPlaceRepository, authorizationService, usernameFetcher, mutateLock, stopPlaceQuayDeleterToChouette, mdmServiceMock, loggingService);

    @Test
    public void doNotDeleteParent() {
        StopPlace parent = new StopPlace();
        parent.setParentStopPlace(true);
        parent.setNetexId("NSR:StopPlace:1");

        when(stopPlaceRepository.findAll(anyList())).thenReturn(Arrays.asList(parent));

        boolean deleted = stopPlaceDeleter.deleteStopPlace(parent.getNetexId());

        assertThat(deleted).isTrue();

        verify(stopPlaceRepository, times(1)).deleteAll(anyList());
    }

    @Test
    public void deleteMonomodalStopPlace() {
        StopPlace monoModalStopPlace = new StopPlace();
        monoModalStopPlace.setNetexId("NSR:StopPlace:");

        when(stopPlaceRepository.findAll(anyList())).thenReturn(Arrays.asList(monoModalStopPlace));
        when(usernameFetcher.getUserNameForAuthenticatedUser()).thenReturn("Rambo");

        boolean deleted = stopPlaceDeleter.deleteStopPlace(monoModalStopPlace.getNetexId());

        assertThat(deleted).isTrue();

        verify(stopPlaceRepository, times(1)).deleteAll(anyList());
        verify(mdmServiceMock, times(1)).deleteStopPlaceBySuperId(anyString());
    }

}