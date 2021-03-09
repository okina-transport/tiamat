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
import org.locationtech.jts.geom.Coordinate;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.validation.NeTExValidator;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.model.AccessibilityAssessment;
import org.rutebanken.tiamat.model.AccessibilityLimitation;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.GroupOfStopPlaces;
import org.rutebanken.tiamat.model.LimitationStatusEnumeration;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopPlaceReference;
import org.rutebanken.tiamat.model.TariffZone;
import org.rutebanken.tiamat.model.TariffZoneRef;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.model.TopographicPlaceRefStructure;
import org.rutebanken.tiamat.model.TopographicPlaceTypeEnumeration;
import org.rutebanken.tiamat.model.ValidBetween;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.netex.validation.NetexXmlReferenceValidator;
import org.rutebanken.tiamat.repository.ProviderRepository;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryTestHelper;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.rutebanken.tiamat.versioning.save.GroupOfStopPlacesSaverService;
import org.rutebanken.tiamat.versioning.save.TariffZoneSaverService;
import org.rutebanken.tiamat.versioning.save.TopographicPlaceVersionedSaverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static javax.xml.bind.JAXBContext.newInstance;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test streaming publication delivery with h2 database
 */
@Transactional
public class StreamingPublicationDeliveryIntegrationTest extends TiamatIntegrationTest {

    @Qualifier("syncStreamingPublicationDelivery")
    @Autowired
    private StreamingPublicationDelivery streamingPublicationDelivery;

    @Autowired
    private TariffZoneSaverService tariffZoneSaverService;

    @Autowired
    private GroupOfStopPlacesSaverService groupOfStopPlacesSaverService;

    @Autowired
    private TopographicPlaceVersionedSaverService topographicPlaceVersionedSaverService;

    @Autowired
    private PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;

    @Autowired
    private PublicationDeliveryHelper publicationDeliveryHelper;

    @Autowired
    private PublicationDeliveryTestHelper publicationDeliveryTestHelper;

    @Autowired
    private ProviderRepository providerRepository;

    private NetexXmlReferenceValidator netexXmlReferenceValidator = new NetexXmlReferenceValidator(true);


    /**
     * Export more than default page size to check that default paging is overriden
     * @throws InterruptedException
     * @throws IOException
     * @throws XMLStreamException
     * @throws SAXException
     * @throws JAXBException
     */
    @Test
    public void exportMoreThanDefaultPageSize() throws InterruptedException, IOException, XMLStreamException, SAXException, JAXBException {

        final int numberOfStopPlaces = StopPlaceSearch.DEFAULT_PAGE_SIZE + 1;
        for(int i = 0; i < numberOfStopPlaces; i++) {
            StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("stop place numbber " + i));
            stopPlace.setVersion(1L);
            stopPlaceRepository.save(stopPlace);
        }
        stopPlaceRepository.flush();


        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setStopPlaceSearch(
                       new StopPlaceSearch())
                .build();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        streamingPublicationDelivery.stream(exportParams, byteArrayOutputStream, true, null);

        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryTestHelper.fromString(byteArrayOutputStream.toString());
        List<org.rutebanken.netex.model.StopPlace> stopPlaces = publicationDeliveryTestHelper.extractStopPlaces(publicationDeliveryStructure);
        assertThat(stopPlaces).hasSize(numberOfStopPlaces);
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

        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setStopPlaceSearch(
                        StopPlaceSearch.newStopPlaceSearchBuilder()
                                .setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE)
                                .build())
                .setTopographicPlaceExportMode(ExportParams.ExportMode.ALL)
                .setTariffZoneExportMode(ExportParams.ExportMode.RELEVANT)
                .build();

        streamingPublicationDelivery.stream(exportParams, byteArrayOutputStream, null);

        String xml = byteArrayOutputStream.toString();

        System.out.println(xml);

        validate(xml);
        netexXmlReferenceValidator.validateNetexReferences(new ByteArrayInputStream(xml.getBytes()), "publicationDelivery");


    }

    /**
     * Set export modes to none, to see that export netex is valid
     */
    @Test
    public void handleExportModeSetToNone() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        TopographicPlace county = new TopographicPlace(new EmbeddableMultilingualString("county"));
        county.setTopographicPlaceType(TopographicPlaceTypeEnumeration.COUNTY);
        county = topographicPlaceVersionedSaverService.saveNewVersion(county);

        TopographicPlace municipality = new TopographicPlace(new EmbeddableMultilingualString("Some municipality"));
        municipality.setTopographicPlaceType(TopographicPlaceTypeEnumeration.MUNICIPALITY);
        municipality.setParentTopographicPlaceRef(new TopographicPlaceRefStructure(county.getNetexId(), String.valueOf(county.getVersion()  )));
        municipality = topographicPlaceVersionedSaverService.saveNewVersion(municipality);

        TariffZone tariffZone = new TariffZone();
        tariffZone.setVersion(1L);
        tariffZone = tariffZoneRepository.save(tariffZone);

        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("stop place"));
        stopPlace.setTopographicPlace(municipality);
        stopPlace.getTariffZones().add(new TariffZoneRef(tariffZone));
        stopPlaceRepository.save(stopPlace);

        stopPlaceRepository.flush();

        GroupOfStopPlaces groupOfStopPlaces = new GroupOfStopPlaces(new EmbeddableMultilingualString("group"));
        groupOfStopPlaces.getMembers().add(new StopPlaceReference(stopPlace.getNetexId()));
        groupOfStopPlacesSaverService.saveNewVersion(groupOfStopPlaces);

        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setStopPlaceSearch(
                        StopPlaceSearch.newStopPlaceSearchBuilder()
                                .setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE)
                                .build())
                .setTopographicPlaceExportMode(ExportParams.ExportMode.NONE)
                .setTariffZoneExportMode(ExportParams.ExportMode.NONE)
                .setGroupOfStopPlacesExportMode(ExportParams.ExportMode.NONE)
                .build();

        streamingPublicationDelivery.stream(exportParams, byteArrayOutputStream, null);

        String xml = byteArrayOutputStream.toString();
        System.out.println(xml);

        netexXmlReferenceValidator.validateNetexReferences(new ByteArrayInputStream(xml.getBytes()), "publicationDelivery");
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
        tariffZoneV2 = tariffZoneRepository.save(tariffZoneV2);

        TariffZone tariffZoneV3 = new TariffZone();
        tariffZoneV3.setNetexId(tariffZoneId);
        tariffZoneV3.setVersion(3L);
        tariffZoneV3 = tariffZoneRepository.save(tariffZoneV3);

        StopPlace stopPlace1 = new StopPlace(new EmbeddableMultilingualString("stop place in publication delivery"));
        stopPlace1.getTariffZones().add(new TariffZoneRef(tariffZoneId)); // Without version, implicity v3
        stopPlace1 = stopPlaceVersionedSaverService.saveNewVersion(stopPlace1);
        final String stopPlace1NetexId = stopPlace1.getNetexId();

        StopPlace stopPlace2 = new StopPlace(new EmbeddableMultilingualString("another stop place in publication delivery"));
        // StopPlace 2 refers to tariffzone version 1. This must be included in the publication delivery, allthough v2 and v3 exists
        stopPlace2.getTariffZones().add(new TariffZoneRef(tariffZoneV1));
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

        stopPlaceRepository.flush();
        tariffZoneRepository.flush();

        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setStopPlaceSearch(
                        StopPlaceSearch.newStopPlaceSearchBuilder()
                                .setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE)
                                .build())
                .setTopographicPlaceExportMode(ExportParams.ExportMode.RELEVANT)
                .setTariffZoneExportMode(ExportParams.ExportMode.RELEVANT)
                .setGroupOfStopPlacesExportMode(ExportParams.ExportMode.RELEVANT)
                .build();

        streamingPublicationDelivery.stream(exportParams, byteArrayOutputStream, null);

        String xml = byteArrayOutputStream.toString();

        System.out.println(xml);

        // The unmarshaller will validate as well. But only if validateAgainstSchema is true
        validate(xml);
        // Validate using own implementation of netex xml reference validator
        netexXmlReferenceValidator.validateNetexReferences(new ByteArrayInputStream(xml.getBytes()), "publicationDelivery");

        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryUnmarshaller.unmarshal(new ByteArrayInputStream(xml.getBytes()));

        org.rutebanken.netex.model.SiteFrame siteFrame = publicationDeliveryHelper.findSiteFrame(publicationDeliveryStructure);
        List<org.rutebanken.netex.model.StopPlace> stops = siteFrame.getStopPlaces().getStopPlace();

        assertThat(stops)
                .hasSize(2)
                .as("stops expected")
                .extracting(org.rutebanken.netex.model.StopPlace::getId)
                .containsOnly(stopPlace1.getNetexId(), stopPlace2.getNetexId());


        // Make sure both stops have references to tariff zones and with correct version
        org.rutebanken.netex.model.StopPlace actualStopPlace1 = stops.stream().filter(sp -> sp.getId().equals(stopPlace1NetexId)).findFirst().get();

        assertThat(actualStopPlace1.getTariffZones())
                .as("actual stop place 1 tariff zones")
                .isNotNull();

        org.rutebanken.netex.model.TariffZoneRef actualTariffZoneRefStopPlace1 = actualStopPlace1.getTariffZones().getTariffZoneRef().get(0);

        // Stop place 1 refers to tariff zone v2 implicity beacuse the reference does not contain version value.
        assertThat(actualTariffZoneRefStopPlace1.getRef())
                .as("actual stop place 1 tariff zone ref")
                .isEqualTo(tariffZoneId);

        // Check stop place 2

        org.rutebanken.netex.model.StopPlace actualStopPlace2 = stops.stream().filter(sp -> sp.getId().equals(stopPlace2NetexId)).findFirst().get();
        org.rutebanken.netex.model.TariffZoneRef actualTariffZoneRefStopPlace2 = actualStopPlace2.getTariffZones().getTariffZoneRef().get(0);

        assertThat(actualTariffZoneRefStopPlace2.getRef())
                .as("actual stop place 2 tariff zone ref")
                .isEqualTo(tariffZoneId);

        assertThat(actualTariffZoneRefStopPlace2.getVersion())
                .as("actual tariff zone ref for stop place 2 should point to version 1 of tariff zone")
                .isEqualTo(String.valueOf(tariffZoneV1.getVersion()));

        // Check topographic places

        assertThat(siteFrame.getTopographicPlaces()).isNotNull();
        assertThat(siteFrame.getTopographicPlaces().getTopographicPlace())
                .as("site fra topopgraphic places")
                .isNotNull()
                .hasSize(1)
                .extracting(org.rutebanken.netex.model.TopographicPlace::getId)
                .containsOnly(topographicPlace.getNetexId());

        // Check tariff zones

        assertThat(siteFrame.getTariffZones())
                .as("site fra tariff zones")
                .isNotNull();


        List<org.rutebanken.netex.model.TariffZone> tarifZones = siteFrame.getTariffZones().getTariffZone_()
                                                                    .stream()
                                                                    .map(jaxbElement -> (org.rutebanken.netex.model.TariffZone) jaxbElement.getValue())
                                                                    .collect(Collectors.toList());


        assertThat(tarifZones)
                .extracting(tariffZone -> tariffZone.getId() + "-" + tariffZone.getVersion())
                .as("Both tariff zones exists in publication delivery. But not the one not being reffered to (v2)")
                .containsOnly(tariffZoneId + "-" + 1, tariffZoneId + "-" + 3);



    }

    /**
     * Reproduce ROR-277, missing current stop place, when there is a future version.
     */
    @Test
    public void keepCurrentVersionOfStopPlaceWhenFutureVersionExist() throws Exception {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        StopPlace stopPlacev1 = new StopPlace(new EmbeddableMultilingualString("name"));
        stopPlacev1 = stopPlaceVersionedSaverService.saveNewVersion(stopPlacev1);
        StopPlace stopPlacev2 = versionCreator.createCopy(stopPlacev1, StopPlace.class);

        stopPlacev2.setValidBetween(new ValidBetween(Instant.now().plus(10, ChronoUnit.DAYS)));

        stopPlaceVersionedSaverService.saveNewVersion(stopPlacev1, stopPlacev2);

        stopPlaceRepository.flush();

        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setStopPlaceSearch(
                        StopPlaceSearch.newStopPlaceSearchBuilder()
                                .setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE)
                                .build())
                .setTopographicPlaceExportMode(ExportParams.ExportMode.NONE)
                .setTariffZoneExportMode(ExportParams.ExportMode.NONE)
                .build();

        streamingPublicationDelivery.stream(exportParams, byteArrayOutputStream, null);

        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryUnmarshaller.unmarshal(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

        List<org.rutebanken.netex.model.StopPlace> stopPlaces = publicationDeliveryTestHelper.extractStopPlaces(publicationDeliveryStructure);
        assertThat(stopPlaces).hasSize(2);
    }

    private void validate(String xml) throws JAXBException, IOException, SAXException {
        JAXBContext publicationDeliveryContext = newInstance(PublicationDeliveryStructure.class);
        Unmarshaller unmarshaller = publicationDeliveryContext.createUnmarshaller();

        NeTExValidator neTExValidator =  NeTExValidator.getNeTExValidator();
        unmarshaller.setSchema(neTExValidator.getSchema());
        unmarshaller.unmarshal(new StringReader(xml));
    }


    /**
     * Test export IDFM
     * @throws InterruptedException
     * @throws IOException
     * @throws XMLStreamException
     * @throws SAXException
     * @throws JAXBException
     */
//    @Test
//    public void exportIDFM() throws Exception {
//
//        Provider provider = new Provider();
//        provider.setId(1L);
//        provider.setCode("1");
//        provider.setName("SQYBUS");
//
//        provider = providerRepository.save(provider);
//        createStopPlace(provider, "boaarle", "Arletty", 48.769338, 2.061942, "50090356");
//        createStopPlace(provider, "boaarle2","Arletty",48.803673, 2.011310, "50089971");
////        createStopPlace(provider, "boabonn","Méliès - Croix Bonnet",48.801103, 2.006726,"50089967");
////        createStopPlace(provider, "boabonn2","Méliès - Croix Bonnet",48.801312, 2.006654, "50090348");
////        createStopPlace(provider, "boaclai2","René Clair",48.801586,2.004456, "");
////        createStopPlace(provider, "boaclair","René Clair",48.801417,2.004304, "");
////        createStopPlace(provider, "boacroi","Croix Blanche",48.798463,2.017417, "50089973");
////        createStopPlace(provider, "boacroi2","Croix Blanche",48.798425,2.016724, "50090021");
////        createStopPlace(provider, "boatati","Jacques Tati",48.803735,2.004744, "");
//
//
//        ExportParams exportParams = ExportParams.newExportParamsBuilder()
//                .setStopPlaceSearch(
//                        StopPlaceSearch.newStopPlaceSearchBuilder().setVersionValidity(ExportParams.VersionValidity.ALL).build())
//                .build();
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//
//
//
//        streamingPublicationDelivery.stream(exportParams, byteArrayOutputStream, true, provider);
//
//        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryTestHelper.fromString(byteArrayOutputStream.toString());
////        List<org.rutebanken.netex.model.StopPlace> stopPlaces = publicationDeliveryTestHelper.extractStopPlaces(publicationDeliveryStructure);
////        assertThat(stopPlaces).hasSize(numberOfStopPlaces);
//
//        assertThat(byteArrayOutputStream.size()).isGreaterThan(0);
//    }
//
//    private void createStopPlace(Provider provider, String privateCode, String originalName, double y, double x, String zdep) {
//        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString(privateCode));
//        Quay quay = getQuay(privateCode, originalName, x, y, zdep);
//        HashSet<Quay> quaysList = new HashSet<>();
//        quaysList.add(quay);
//        stopPlace.setQuays(quaysList);
//        stopPlace.setVersion(1L);
//        stopPlace.setProvider(provider);
//        stopPlace.setStopPlaceType(StopTypeEnumeration.ONSTREET_BUS);
//        stopPlaceRepository.save(stopPlace);
//    }

    private Quay getQuay(String privateCode, String originalName, double x, double y, String zdep) {
        Quay quay = new Quay();
        quay.setCentroid(geometryFactory.createPoint(new Coordinate(x,y)));
        quay.getOriginalZDEP().add(zdep);
        quay.getOriginalNames().add(originalName);

        // Bon fonctionnement de la gestion des codes à vérifier et à traiter
//        PrivateCodeStructure privateCodeStructure = new PrivateCodeStructure();
//        privateCodeStructure.setValue(privateCode);
//        quay.setPrivateCode(privateCodeStructure);

        String publicCode = "testcode";
        quay.setPublicCode(publicCode);

        AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
        accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.UNKNOWN);
        AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
        // comment or uncomment to test
        //accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setAudibleSignalsAvailable(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setVisualSignsAvailable(LimitationStatusEnumeration.UNKNOWN);
        List<AccessibilityLimitation> accessibilityLimitationList = new ArrayList<AccessibilityLimitation>();
        accessibilityLimitationList.add(accessibilityLimitation);
        accessibilityAssessment.setLimitations(accessibilityLimitationList);
        quay.setAccessibilityAssessment(accessibilityAssessment);

        quay.setZipCode("75000");

        return quay;
    }

}
