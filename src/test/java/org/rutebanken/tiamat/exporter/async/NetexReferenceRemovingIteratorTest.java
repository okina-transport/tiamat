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

package org.rutebanken.tiamat.exporter.async;

import org.junit.Test;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class NetexReferenceRemovingIteratorTest {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Test
    public void testReferenceRemoval() {


        StopPlace stopPlace = new StopPlace()
                .withTariffZones(
                        new TariffZoneRefs_RelStructure()
                                .withTariffZoneRef_(
                                        netexObjectFactory.createTariffZoneRef(new TariffZoneRef()
                                                .withRef("ref")
                                                .withVersion("version"))))
                .withTopographicPlaceRef(
                        new TopographicPlaceRefStructure()
                            .withValue("KVE:TopographicPlace:XXX")
                            .withVersion("version"));


        List<StopPlace> stopPlaces = Arrays.asList(stopPlace);


        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setTopographicPlaceExportMode(ExportParams.ExportMode.NONE)
                .setTariffZoneExportMode(ExportParams.ExportMode.NONE)
                .build();

        NetexReferenceRemovingIterator netexReferenceRemovingIterator = new NetexReferenceRemovingIterator(stopPlaces.iterator(), exportParams);


        StopPlace actual = netexReferenceRemovingIterator.next();

        assertThat(actual.getTariffZones().getTariffZoneRef_().get(0).getValue().getVersion()).as("TariffZoneref version").isNull();
        assertThat(actual.getTopographicPlaceRef().getVersion()).as("topographic place ref version").isNull();
    }

    @Test
    public void testNoReferenceRemoval() {


        StopPlace stopPlace = new StopPlace()
                .withTariffZones(
                        new TariffZoneRefs_RelStructure()
                                .withTariffZoneRef_(
                                        netexObjectFactory.createTariffZoneRef(new TariffZoneRef()
                                                .withRef("ref")
                                                .withVersion("version"))))
                .withTopographicPlaceRef(
                        new TopographicPlaceRefStructure()
                                .withValue("KVE:TopographicPlace:XXX")
                                .withVersion("version"));


        List<StopPlace> stopPlaces = Arrays.asList(stopPlace);


        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setTopographicPlaceExportMode(ExportParams.ExportMode.RELEVANT)
                .setTariffZoneExportMode(ExportParams.ExportMode.RELEVANT)
                .build();

        NetexReferenceRemovingIterator netexReferenceRemovingIterator = new NetexReferenceRemovingIterator(stopPlaces.iterator(), exportParams);

        StopPlace actual = netexReferenceRemovingIterator.next();

        assertThat(actual.getTariffZones().getTariffZoneRef_().get(0).getValue().getVersion()).as("TariffZoneref version").isEqualTo("version");
        assertThat(actual.getTopographicPlaceRef().getVersion()).as("topographic place ref version").isEqualTo("version");
    }

}