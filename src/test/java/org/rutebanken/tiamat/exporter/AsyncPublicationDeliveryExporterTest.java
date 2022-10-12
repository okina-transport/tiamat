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

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.rutebanken.netex.model.GeneralFrame;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.validation.NeTExValidator;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.GroupOfStopPlaces;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopPlaceReference;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.TariffZone;
import org.rutebanken.tiamat.model.TariffZoneRef;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.model.TopographicPlaceRefStructure;
import org.rutebanken.tiamat.model.TopographicPlaceTypeEnumeration;
import org.rutebanken.tiamat.model.ValidBetween;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.model.VehicleModeEnumeration;
import org.rutebanken.tiamat.model.job.ExportJob;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.netex.validation.NetexXmlReferenceValidator;
import org.rutebanken.tiamat.repository.ExportJobRepository;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryTestHelper;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.rutebanken.tiamat.versioning.save.TopographicPlaceVersionedSaverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.xml.bind.JAXBContext.newInstance;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AsyncPublicationDeliveryExporterTest extends TiamatIntegrationTest {

    @Qualifier("syncStreamingPublicationDelivery")
    @Autowired
    private StreamingPublicationDelivery streamingPublicationDelivery;

    @Autowired
    private AsyncPublicationDeliveryExporter asyncPublicationDeliveryExporter;

    @Autowired
    private ExportJobRepository exportJobRepository;

    @Autowired
    private PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;

    @Autowired
    private PublicationDeliveryTestHelper publicationDeliveryTestHelper;

    @Autowired
    private TopographicPlaceVersionedSaverService topographicPlaceVersionedSaverService;

    private NetexXmlReferenceValidator netexXmlReferenceValidator = new NetexXmlReferenceValidator(true);

    @Autowired
    private PublicationDeliveryHelper publicationDeliveryHelper;

    @Test
    public void test() throws InterruptedException, JAXBException, IOException, SAXException {

        asyncPublicationDeliveryExporter.providerRepository = providerRepository;
        stopPlaceRepository.deleteAll();

        final int numberOfStopPlaces = StopPlaceSearch.DEFAULT_PAGE_SIZE;
        for (int i = 0; i < numberOfStopPlaces; i++) {
            StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("stop place numbber " + i));
            stopPlace.setVersion(1L);
            stopPlace.setProvider("test");
            stopPlace.setStopPlaceType(StopTypeEnumeration.ONSTREET_BUS);


            Quay quay = new Quay();
            quay.setNetexId("NSR:Quay:" + i);
            quay.setName(new EmbeddableMultilingualString("Quay_" + i));
            quay.setPublicCode("quay" + i);
            quay.setCentroid(geometryFactory.createPoint(new Coordinate(48, 2)));
            quay.setZipCode("75000");

            stopPlace.getQuays().add(quay);

            stopPlaceRepository.save(stopPlace);

        }
        stopPlaceRepository.flush();


        Provider provider = providerRepository.getProviders().iterator().next();
        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setStopPlaceSearch(
                        StopPlaceSearch
                                .newStopPlaceSearchBuilder()
                                .setVersionValidity(ExportParams.VersionValidity.ALL)
                                .build())
                .setProviderId(provider.getId())
                .build();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ExportJob exportJob = asyncPublicationDeliveryExporter.startExportJob(exportParams);


        streamingPublicationDelivery.stream(byteArrayOutputStream, provider, LocalDateTime.now(), exportJob.getId());
        asyncPublicationDeliveryExporter.streamingPublicationDelivery = streamingPublicationDelivery;


        JobStatus startStatus = exportJob.getStatus();

        assertThat(exportJob.getId()).isGreaterThan(0L);

        long start = System.currentTimeMillis();
        long timeout = 60000;
        while (true) {
            Optional<ExportJob> actualExportJob = exportJobRepository.findById(exportJob.getId());
            if (actualExportJob.get().getStatus().equals(startStatus)) {

                long time = System.currentTimeMillis() - start;
                if (time > timeout) {
                    fail("Waited more than " + timeout + " millis for job status to change. Process duration:" + time);
                }
                Thread.sleep(1000);
                continue;
            }

            if (actualExportJob.get().getStatus().equals(JobStatus.FAILED)) {
                fail("Job status is failed");
            } else if (actualExportJob.get().getStatus().equals(JobStatus.FINISHED)) {
                System.out.println("Job finished");
                break;
            }
        }
    }

    @Test
    public void testName() {
        // GIVEN

        // WHEN
        String sqybus = asyncPublicationDeliveryExporter.createFileNameWithoutExtention("41", "SQYBUS", LocalDateTime.now(ZoneOffset.UTC));

        // THEN
        Assert.assertTrue(sqybus.length() > 0);
    }

    @Test
    public void keepCurrentVersionOfStopPlaceWhenFutureVersionExist() throws Exception {
        asyncPublicationDeliveryExporter.providerRepository = providerRepository;
        Provider provider = providerRepository.getProviders().iterator().next();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        StopPlace stopPlacev1 = new StopPlace(new EmbeddableMultilingualString("name"));
        stopPlacev1.setProvider(provider.getChouetteInfo().getReferential());
        stopPlacev1 = stopPlaceVersionedSaverService.saveNewVersion(stopPlacev1);
        StopPlace stopPlacev2 = versionCreator.createCopy(stopPlacev1, StopPlace.class);

        stopPlacev2.setValidBetween(new ValidBetween(Instant.now().plus(10, ChronoUnit.DAYS)));

        stopPlaceVersionedSaverService.saveNewVersion(stopPlacev1, stopPlacev2);

        stopPlaceRepository.flush();

        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setStopPlaceSearch(
                        StopPlaceSearch.newStopPlaceSearchBuilder()
                                .setVersionValidity(ExportParams.VersionValidity.ALL)
                                .build())
                .setProviderId(provider.getId())
                .build();

        ExportJob exportJob = asyncPublicationDeliveryExporter.startExportJob(exportParams);

        streamingPublicationDelivery.stream(byteArrayOutputStream, provider, LocalDateTime.now(), exportJob.getId());
        asyncPublicationDeliveryExporter.streamingPublicationDelivery = streamingPublicationDelivery;

        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryUnmarshaller.unmarshal(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

        List<org.rutebanken.netex.model.StopPlace> stopPlaces = publicationDeliveryTestHelper.extractStopPlaces(publicationDeliveryStructure, true);
        assertThat(stopPlaces).hasSize(1);
    }


    @Test
    public void streamStopPlacesAndRelatedEntitiesIntoPublicationDelivery() throws Exception {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        TopographicPlace topographicPlace = new TopographicPlace(new EmbeddableMultilingualString("Some municipality"));
        topographicPlace.setTopographicPlaceType(TopographicPlaceTypeEnumeration.MUNICIPALITY);
        topographicPlace = topographicPlaceVersionedSaverService.saveNewVersion(topographicPlace);

        String tariffZoneId = "CRI:TariffZone:1";

        TariffZone tariffZoneV1 = new TariffZone();
        tariffZoneV1.setNetexId(tariffZoneId);
        tariffZoneV1.setVersion(1L);
        tariffZoneV1 = tariffZoneRepository.save(tariffZoneV1);


        TariffZone tariffZoneV2 = new TariffZone();
        tariffZoneV2.setNetexId(tariffZoneId);
        tariffZoneV2.setVersion(2L);
        tariffZoneRepository.save(tariffZoneV2);

        TariffZone tariffZoneV3 = new TariffZone();
        tariffZoneV3.setNetexId(tariffZoneId);
        tariffZoneV3.setVersion(3L);
        tariffZoneRepository.save(tariffZoneV3);

        String quayNetexId = "CRI:Quay:1";
        Quay quay1 = new Quay();
        quay1.setNetexId(quayNetexId);
        quay1.setVersion(1L);
        quay1.setTransportMode(org.rutebanken.tiamat.model.VehicleModeEnumeration.BUS);

        StopPlace stopPlace1 = new StopPlace(new EmbeddableMultilingualString("stop place in publication delivery"));
        stopPlace1.getTariffZones().add(new TariffZoneRef(tariffZoneV3));
        stopPlace1.getQuays().add(quay1);
        stopPlace1 = stopPlaceVersionedSaverService.saveNewVersion(stopPlace1);
        final String stopPlace1NetexId = stopPlace1.getNetexId();

        StopPlace stopPlace2 = new StopPlace(new EmbeddableMultilingualString("another stop place in publication delivery"));
        stopPlace2.getTariffZones().add(new TariffZoneRef(tariffZoneV3));
        stopPlace2 = stopPlaceVersionedSaverService.saveNewVersion(stopPlace2);
        final String stopPlace2NetexId = stopPlace2.getNetexId();

        GroupOfStopPlaces groupOfStopPlaces1 = new GroupOfStopPlaces(new EmbeddableMultilingualString("group of stop places"));
        groupOfStopPlaces1.getMembers().add(new StopPlaceReference(stopPlace1.getNetexId()));

        groupOfStopPlacesSaverService.saveNewVersion(groupOfStopPlaces1);

        GroupOfStopPlaces groupOfStopPlaces2 = new GroupOfStopPlaces(new EmbeddableMultilingualString("group of stop places number two"));
        groupOfStopPlaces2.getMembers().add(new StopPlaceReference(stopPlace1.getNetexId()));

        groupOfStopPlacesSaverService.saveNewVersion(groupOfStopPlaces2);

        groupOfStopPlacesRepository.flush();

        // Allows setting topographic place without lookup.
        // To have the lookup work, topographic place polygon must exist
        stopPlace1.setTopographicPlace(topographicPlace);
        stopPlaceRepository.save(stopPlace1);
        quay1.setSiteRef(new org.rutebanken.tiamat.model.SiteRefStructure(stopPlace1.getNetexId()));

        stopPlaceRepository.flush();
        tariffZoneRepository.flush();
        quayRepository.flush();

        asyncPublicationDeliveryExporter.providerRepository = providerRepository;
        Provider provider = providerRepository.getProviders().iterator().next();

        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setStopPlaceSearch(
                        StopPlaceSearch.newStopPlaceSearchBuilder()
                                .setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE)
                                .build())
                .setTopographicPlaceExportMode(ExportParams.ExportMode.RELEVANT)
                .setTariffZoneExportMode(ExportParams.ExportMode.RELEVANT)
                .setGroupOfStopPlacesExportMode(ExportParams.ExportMode.RELEVANT)
                .setProviderId(provider.getId())
                .build();

        ExportJob exportJob = asyncPublicationDeliveryExporter.startExportJob(exportParams);

        streamingPublicationDelivery.stream(byteArrayOutputStream, null, LocalDateTime.now(), exportJob.getId());

        String xml = byteArrayOutputStream.toString();

        System.out.println(xml);

        // The unmarshaller will validate as well. But only if validateAgainstSchema is true
        validate(xml);
        // Validate using own implementation of netex xml reference validator
        netexXmlReferenceValidator.validateNetexReferences(new ByteArrayInputStream(xml.getBytes()), "publicationDelivery");

        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryUnmarshaller.unmarshal(new ByteArrayInputStream(xml.getBytes()));

        GeneralFrame netexGeneralFrame= publicationDeliveryHelper.findGeneralFrame(publicationDeliveryStructure);

        //Make sure that the stop places are in the members list
        List<JAXBElement> jaxStopPlaces = netexGeneralFrame.getMembers()
                .getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity()
                .stream()
                .filter(jaxbElement -> jaxbElement.getValue() instanceof org.rutebanken.netex.model.StopPlace)
                .collect(Collectors.toList());

        List<org.rutebanken.netex.model.StopPlace> stopPlaces = jaxStopPlaces.stream().map(jaxStopPlace -> (org.rutebanken.netex.model.StopPlace)jaxStopPlace.getValue()).collect(Collectors.toList());

        assertThat(stopPlaces)
                .hasSize(2)
                .as("stops expected")
                .extracting(org.rutebanken.netex.model.StopPlace::getId)
                .containsOnly(stopPlace1.getNetexId(), stopPlace2.getNetexId());

        //Make sure that we have just the quay Reference in the Stop Place 1
        org.rutebanken.netex.model.StopPlace actualStopPlace1 = stopPlaces.stream().filter(sp -> sp.getId().equals(stopPlace1NetexId)).findFirst().get();

        Optional<JAXBElement<?>> optionalQuayRef = actualStopPlace1.getQuays().getQuayRefOrQuay().stream().findFirst();
        if(optionalQuayRef.isPresent()){
            QuayRefStructure quayRef = (QuayRefStructure) optionalQuayRef.get().getValue();
            assertThat(quayRef.getRef()).isEqualTo(quayNetexId);
        }

        //Make sure that the quays is in the members
        List<JAXBElement> jaxQuays = netexGeneralFrame.getMembers()
                .getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity()
                .stream()
                .filter(jaxbElement -> jaxbElement.getValue() instanceof org.rutebanken.netex.model.Quay)
                .collect(Collectors.toList());

        List<org.rutebanken.netex.model.Quay> quays = jaxQuays.stream().map(jaxQuay -> (org.rutebanken.netex.model.Quay)jaxQuay.getValue()).collect(Collectors.toList());

        assertThat(quays).hasSize(1);
        assertThat(quays.get(0).getId()).isEqualTo(quayNetexId);


        // Make sure both stops have references to tariff zones and with correct version
        assertThat(actualStopPlace1.getTariffZones())
                .as("actual stop place 1 tariff zones")
                .isNotNull();

        org.rutebanken.netex.model.TariffZoneRef actualTariffZoneRefStopPlace1 = (org.rutebanken.netex.model.TariffZoneRef) actualStopPlace1.getTariffZones().getTariffZoneRef_().get(0).getValue();

        // Stop place 1 refers to tariff zone v3 implicity beacuse the reference does not contain version value.
        assertThat(actualTariffZoneRefStopPlace1.getRef())
                .as("actual stop place 1 tariff zone ref")
                .isEqualTo(tariffZoneId);

        // Check stop place 2

        org.rutebanken.netex.model.StopPlace actualStopPlace2 = stopPlaces.stream().filter(sp -> sp.getId().equals(stopPlace2NetexId)).findFirst().get();
        org.rutebanken.netex.model.TariffZoneRef actualTariffZoneRefStopPlace2 = (org.rutebanken.netex.model.TariffZoneRef) actualStopPlace2.getTariffZones().getTariffZoneRef_().get(0).getValue();

        assertThat(actualTariffZoneRefStopPlace2.getRef())
                .as("actual stop place 2 tariff zone ref")
                .isEqualTo(tariffZoneId);

        assertThat(actualTariffZoneRefStopPlace2.getVersion())
                .as("actual tariff zone ref for stop place 2 should point to version 3 of tariff zone")
                .isEqualTo(String.valueOf(tariffZoneV3.getVersion()));

        // Check topographic places
        List<JAXBElement>jaxTopographicPlaces = netexGeneralFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity()
                .stream()
                .filter(jaxbElement -> jaxbElement.getValue() instanceof org.rutebanken.netex.model.TopographicPlace)
                .collect(Collectors.toList());
        List<org.rutebanken.netex.model.TopographicPlace> topographicPlaces = jaxTopographicPlaces.stream().map(jaxTopographicPlace -> (org.rutebanken.netex.model.TopographicPlace)jaxTopographicPlace.getValue()).collect(Collectors.toList());

        assertThat(topographicPlaces).isNotNull();
        assertThat(topographicPlaces)
                .as("site fra topopgraphic places")
                .isNotNull()
                .hasSize(1)
                .extracting(org.rutebanken.netex.model.TopographicPlace::getId)
                .containsOnly(topographicPlace.getNetexId());

        // Check tariff zones
        List<JAXBElement> jaxTariffZones= netexGeneralFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity()
                .stream()
                .filter(jaxbElement -> jaxbElement.getValue() instanceof org.rutebanken.netex.model.TariffZone)
                .collect(Collectors.toList());
        List <org.rutebanken.netex.model.TariffZone> tariffZones = jaxTariffZones.stream().map(tariffZone -> (org.rutebanken.netex.model.TariffZone)tariffZone.getValue()).collect(Collectors.toList());
        assertThat(tariffZones)
                .as("site fra tariff zones")
                .isNotNull();

        assertThat(tariffZones)
                .extracting(tariffZone -> tariffZone.getId() + "-" + tariffZone.getVersion())
                .as("Only one tariff zones exists in publication delivery. (v3)")
                .containsOnly(tariffZoneId + "-" + 3);
    }


    @Test
    public void avoidDuplicateTopographicPlaceWhenExportModeAll() throws Exception {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        TopographicPlace county = new TopographicPlace(new EmbeddableMultilingualString("county"));
        county.setTopographicPlaceType(TopographicPlaceTypeEnumeration.COUNTY);
        county = topographicPlaceVersionedSaverService.saveNewVersion(county);

        TopographicPlace municipality = new TopographicPlace(new EmbeddableMultilingualString("Some municipality"));
        municipality.setTopographicPlaceType(TopographicPlaceTypeEnumeration.MUNICIPALITY);
        municipality.setParentTopographicPlaceRef(new TopographicPlaceRefStructure(county.getNetexId(), String.valueOf(county.getVersion()  )));
        municipality = topographicPlaceVersionedSaverService.saveNewVersion(municipality);

        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("stop place"));
        stopPlace.setTopographicPlace(municipality);
        stopPlaceRepository.save(stopPlace);

        stopPlaceRepository.flush();

        asyncPublicationDeliveryExporter.providerRepository = providerRepository;
        Provider provider = providerRepository.getProviders().iterator().next();

        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setStopPlaceSearch(
                        StopPlaceSearch.newStopPlaceSearchBuilder()
                                .setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE)
                                .build())
                .setTopographicPlaceExportMode(ExportParams.ExportMode.ALL)
                .setTariffZoneExportMode(ExportParams.ExportMode.RELEVANT)
                .setProviderId(provider.getId())
                .build();

        ExportJob exportJob = asyncPublicationDeliveryExporter.startExportJob(exportParams);

        streamingPublicationDelivery.stream(byteArrayOutputStream, provider, LocalDateTime.now(), exportJob.getId());

        String xml = byteArrayOutputStream.toString();

        System.out.println(xml);

        validate(xml);
        netexXmlReferenceValidator.validateNetexReferences(new ByteArrayInputStream(xml.getBytes()), "publicationDelivery");


    }

    private void validate(String xml) throws JAXBException, IOException, SAXException {
        JAXBContext publicationDeliveryContext = newInstance(PublicationDeliveryStructure.class);
        Unmarshaller unmarshaller = publicationDeliveryContext.createUnmarshaller();

        NeTExValidator neTExValidator =  NeTExValidator.getNeTExValidator();
        unmarshaller.setSchema(neTExValidator.getSchema());
        unmarshaller.unmarshal(new StringReader(xml));
    }
}
