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

package org.rutebanken.tiamat.exporter;

import org.junit.Test;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.TariffZone;
import org.rutebanken.tiamat.repository.TariffZoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TariffZonesFromStopsExporterTest extends TiamatIntegrationTest {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Autowired
    private TariffZoneRepository tariffZoneRepository;

    @Autowired
    private TariffZonesFromStopsExporter tariffZonesFromStopsExporter;

    private QName qname = new QName("http://www.netex.org.uk/netex", "TariffZone_");

    @Test
    public void avoidDuplicateTariffZones() {

        TariffZone tariffZone = new TariffZone();
        tariffZone.setNetexId("VKT:TariffZone:201");
        tariffZone.setVersion(1L);
        tariffZoneRepository.save(tariffZone);

        // Two stops with reference to the same tariffzone
        StopPlace netexStopPlace = new StopPlace();
        netexStopPlace.setId("NSR:StopPlace:1");
        netexStopPlace.withTariffZones(new TariffZoneRefs_RelStructure().withTariffZoneRef_(netexObjectFactory.createTariffZoneRef(new TariffZoneRef().withRef(tariffZone.getNetexId()).withVersion("1"))));

        StopPlace netexStopPlace2 = new StopPlace();
        netexStopPlace2.setId("NSR:StopPlace:2");
        netexStopPlace2.withTariffZones(new TariffZoneRefs_RelStructure().withTariffZoneRef_(netexObjectFactory.createTariffZoneRef(new TariffZoneRef().withRef(tariffZone.getNetexId()).withVersion("1"))));

        SiteFrame siteFrame = new SiteFrame();
        tariffZonesFromStopsExporter.resolveTariffZones(Arrays.asList(netexStopPlace, netexStopPlace2), siteFrame);


        List<org.rutebanken.netex.model.TariffZone> tariffZones = siteFrame.getTariffZones().getTariffZone_()
                .stream()
                .map(jaxbElement -> (org.rutebanken.netex.model.TariffZone) jaxbElement.getValue())
                .collect(Collectors.toList());


        assertThat(tariffZones).as("Number of tariffzones returned").hasSize(1);

    }

    @Test
    public void handleUnresolvableTariffZoneRef() {

        TariffZone tariffZone = new TariffZone();
        tariffZone.setNetexId("VKT:TariffZone:201");
        tariffZone.setVersion(1L);
        tariffZoneRepository.save(tariffZone);

        StopPlace netexStopPlace = new StopPlace();
        netexStopPlace.setId("NSR:StopPlace:1");
        netexStopPlace.withTariffZones(new TariffZoneRefs_RelStructure().withTariffZoneRef_(netexObjectFactory.createTariffZoneRef(new TariffZoneRef().withRef("NSR:TariffZone:1"))));

        SiteFrame siteFrame = new SiteFrame();
        tariffZonesFromStopsExporter.resolveTariffZones(Arrays.asList(netexStopPlace), siteFrame);

        assertThat(siteFrame.getTariffZones()).as("Number of tariffzones returned").isNull();

    }

    @Test
    public void keepExistingTariffZones() {

        TariffZone tariffZone = new TariffZone();
        tariffZone.setNetexId("VKT:TariffZone:201");
        tariffZone.setVersion(1L);
        tariffZoneRepository.save(tariffZone);

        org.rutebanken.netex.model.TariffZone alreadyAddedTariffZone = new org.rutebanken.netex.model.TariffZone()
                .withId("VKT:TariffZone:123")
                .withVersion("2");

        StopPlace netexStopPlace = new StopPlace();
        netexStopPlace.setId("NSR:StopPlace:1");
        netexStopPlace.withTariffZones(new TariffZoneRefs_RelStructure().withTariffZoneRef_(netexObjectFactory.createTariffZoneRef(new TariffZoneRef().withRef(tariffZone.getNetexId()).withVersion("1"))));

        SiteFrame siteFrame = new SiteFrame();
        siteFrame.withTariffZones(new TariffZonesInFrame_RelStructure().withTariffZone_(netexObjectFactory.createTariffZone(alreadyAddedTariffZone)));
        tariffZonesFromStopsExporter.resolveTariffZones(Arrays.asList(netexStopPlace), siteFrame);


        List<org.rutebanken.netex.model.TariffZone> tarifZones = siteFrame.getTariffZones().getTariffZone_()
                .stream()
                .map(jaxbElement -> (org.rutebanken.netex.model.TariffZone) jaxbElement.getValue())
                .collect(Collectors.toList());

        assertThat(tarifZones).as("Number of tariffzones returned").hasSize(2);

    }

}