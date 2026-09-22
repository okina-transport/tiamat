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

package org.rutebanken.tiamat.importer.mdm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rutebanken.tiamat.client.mdm.MdmClient;
import org.rutebanken.tiamat.client.mdm.OkinaIdentifier;
import org.rutebanken.tiamat.config.TiamatProperties;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MdmService#getProvider}, resolved indirectly through
 * {@link MdmService#getExistingStopPlaceMdmIds}, which builds the MDM
 * identifier's dataset from the stop place provider.
 */
@ExtendWith(MockitoExtension.class)
class MdmServiceTest {

    @InjectMocks
    private MdmService mdmService;

    @Mock
    private MdmClient mdmClient;

    @Mock
    private TiamatProperties tiamatProperties;

    @Captor
    private ArgumentCaptor<OkinaIdentifier> okinaIdentifierCaptor;

    @Test
    void getProvider_stopPlaceHasProvider_usesItUppercased() throws TiamatBusinessException {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setNetexId("MOBIITI:StopPlace:1");
        stopPlace.setProvider("prov1");

        mdmService.getExistingStopPlaceMdmIds(stopPlace);

        verify(mdmClient).getStopPlaceIdentifiersByOriginalId(okinaIdentifierCaptor.capture());
        assertThat(okinaIdentifierCaptor.getValue().getDataset()).isEqualTo("PROV1");
        assertThat(stopPlace.getProvider()).isEqualTo("prov1");
    }

    @Test
    void getProvider_noProviderButImportedIdKeyValue_extractsDatasetFromImportedId() throws TiamatBusinessException {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setNetexId("MOBIITI:StopPlace:1");
        stopPlace.getOrCreateValues("imported-id").add("PROV2:StopPlace:123");

        mdmService.getExistingStopPlaceMdmIds(stopPlace);

        verify(mdmClient).getStopPlaceIdentifiersByOriginalId(okinaIdentifierCaptor.capture());
        assertThat(okinaIdentifierCaptor.getValue().getDataset()).isEqualTo("PROV2");
        assertThat(stopPlace.getProvider()).isEqualTo("prov2");
    }

    @Test
    void getProvider_noProviderAndNoImportedId_fallsBackToDefaultProvider() throws TiamatBusinessException {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setNetexId("MOBIITI:StopPlace:1");

        mdmService.getExistingStopPlaceMdmIds(stopPlace);

        verify(mdmClient).getStopPlaceIdentifiersByOriginalId(okinaIdentifierCaptor.capture());
        assertThat(okinaIdentifierCaptor.getValue().getDataset()).isEqualTo("TECHNIQUE");
        assertThat(stopPlace.getProvider()).isEqualTo("technique");
    }
}