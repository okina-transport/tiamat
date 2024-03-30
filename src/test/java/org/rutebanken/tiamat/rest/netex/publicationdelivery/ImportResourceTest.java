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

package org.rutebanken.tiamat.rest.netex.publicationdelivery;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ImportType;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.xml.sax.SAXException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ImportResourceTest extends TiamatIntegrationTest {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Autowired
    private ImportResource importResource;

    @Autowired
    private PublicationDeliveryTestHelper publicationDeliveryTestHelper;

    @Autowired
    private PublicationDeliveryHelper publicationDeliveryHelper;

    private LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);


    private ImportParams createStandardParamsForImport(){
        ImportParams importParams = new ImportParams();
        importParams.importType = ImportType.MATCH;
        importParams.providerCode ="PROV1";
        return importParams;
    }

    /**
     * When sending a stop place with the same ID twice, the same stop place must be returned.
     * When importing multiple stop places and those exists, make sure no Lazy Initialization Exception is thrown.
     */
    @Test
    public void publicationDeliveriesWithDuplicateStopPlace() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("RUT:StopPlace:123123")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withName(new MultilingualString().withValue("SP1"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("9"))
                                .withLongitude(new BigDecimal("71"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("RUT:StopArea:87654")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("RUT:StopPlace:123123").withRef("RUT:StopPlace:123123"))
                                .withName(new MultilingualString().withValue("q1").withLang("no"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));

        StopPlace stopPlace2 = new StopPlace()
                .withId("RUT:StopPlace:123123")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withName(new MultilingualString().withValue("SP2"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("10"))
                                .withLongitude(new BigDecimal("72"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("RUT:StopArea:87654")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("RUT:StopPlace:123123").withRef("RUT:StopPlace:123123"))
                                .withName(new MultilingualString().withValue("q1").withLang("no"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));


        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);
        publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        PublicationDeliveryStructure publicationDelivery2 = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace2);
        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery2);


        List<StopPlace> result = publicationDeliveryTestHelper.extractStopPlaces(response, false);

        assertThat(result).as("Expecting one stop place in return, as there is no need to return duplicates").hasSize(1);
    }

    @Test
    @Ignore //disable test as stop place types are different, they should not be merged
    public void publicationDeliveriesWithBusStationStopAndOnStreetBus() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("RUT:StopPlace:123123")
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withName(new MultilingualString().withValue("somewhere"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("9"))
                                .withLongitude(new BigDecimal("71"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("RUT:StopArea:1425")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("RUT:StopPlace:123123").withRef("RUT:StopPlace:123123"))
                                .withName(new MultilingualString().withValue("q1").withLang("no"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));

        StopPlace stopPlace2 = new StopPlace()
                .withId("RUT:StopPlace:987654321")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.ONSTREET_BUS)
                .withName(new MultilingualString().withValue("somewhere"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("9"))
                                .withLongitude(new BigDecimal("71"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("RUT:StopArea:87654")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("XYZ:StopPlace:3231").withRef("XYZ:StopPlace:3231"))
                                .withName(new MultilingualString().withValue("q1").withLang("no"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));



        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);
        publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        PublicationDeliveryStructure publicationDelivery2 = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace2);
        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery2);


        List<StopPlace> result = publicationDeliveryTestHelper.extractStopPlaces(response, false);

        assertThat(result).as("Expecting one stop place in return, as stops imported has onstreet bus and bus station as type").hasSize(1);
        publicationDeliveryTestHelper.hasOriginalId("RUT:StopPlace:123123", result.get(0));
        publicationDeliveryTestHelper.hasOriginalId("RUT:StopPlace:987654321", result.get(0));
    }

    @Test
    @Ignore
    public void ignoreStopPlaceTypes() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:StopPlace:321")
                .withVersion("3")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withName(new MultilingualString().withValue("SP21").withLang("no"))
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION);


        ImportParams importParams = new ImportParams();
        importParams.ignoreStopTypes = Sets.newHashSet(org.rutebanken.tiamat.model.StopTypeEnumeration.BUS_STATION);
        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);
        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery, importParams);
        List<StopPlace> result = publicationDeliveryTestHelper.extractStopPlaces(response, false);

        assertThat(result).isEmpty();
    }

    @Test
    @Ignore
    public void allowOnlyStopPlaceTypes() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:StopPlace:3231")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withName(new MultilingualString().withValue("SP3231").withLang("no"))
                .withVersion("2")
                .withStopPlaceType(StopTypeEnumeration.METRO_STATION);

        StopPlace other = new StopPlace()
                .withId("XYZ:StopPlace:9988")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withName(new MultilingualString().withValue("SP9988").withLang("no"))
                .withVersion("2")
                .withStopPlaceType(StopTypeEnumeration.AIRPORT);

        ImportParams importParams = new ImportParams();
        importParams.allowOnlyStopTypes = Sets.newHashSet(org.rutebanken.tiamat.model.StopTypeEnumeration.METRO_STATION);
        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace, other);
        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery, importParams);
        List<StopPlace> result = publicationDeliveryTestHelper.extractStopPlaces(response, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStopPlaceType().equals(StopTypeEnumeration.METRO_STATION));
    }

    @Test
    public void forceStopPlaceType() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:StopPlace:3231")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withName(new MultilingualString().withValue("somewhere"))
                .withVersion("2")
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("RUT:StopArea:87654")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("XYZ:StopPlace:3231").withRef("XYZ:StopPlace:3231"))
                                .withName(new MultilingualString().withValue("q1").withLang("no"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));

        ImportParams importParams = createStandardParamsForImport();
        importParams.forceStopType = org.rutebanken.tiamat.model.StopTypeEnumeration.BUS_STATION;

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);
        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery, importParams);
        List<StopPlace> result = publicationDeliveryTestHelper.extractStopPlaces(response, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStopPlaceType()).isEqualTo(StopTypeEnumeration.BUS_STATION);
    }

    @Test
    public void allowOtherWhenMatchingExistingStopPlacesWithImportTypeMATCH() throws Exception {

        StopPlace stopPlaceToBeMatched = new StopPlace()
                .withId("RUT:StopPlace:987978")
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withName(new MultilingualString().withValue("somewhere"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("9"))
                                .withLongitude(new BigDecimal("71"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("RUT:StopArea:87654")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("RUT:StopPlace:123").withRef("RUT:StopPlace:123"))
                                .withName(new MultilingualString().withValue("q1").withLang("no"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));

        StopPlace incomingStopPlace = new StopPlace()
                .withId("RUT:StopPlace:123546789")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withName(new MultilingualString().withValue("somewhere"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("9"))
                                .withLongitude(new BigDecimal("71"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("RUT:StopArea:87654")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("RUT:StopPlace:123546789").withRef("RUT:StopPlace:123546789"))
                                .withName(new MultilingualString().withValue("q1").withLang("no"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));


        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlaceToBeMatched);
        publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        PublicationDeliveryStructure publicationDelivery2 = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(incomingStopPlace);
        ImportParams importParams = createStandardParamsForImport();
        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery2, importParams);


        List<StopPlace> result = publicationDeliveryTestHelper.extractStopPlaces(response, false);

        assertThat(result).as("Expecting one stop place in return, as stops imported has onstreet bus and bus station as type").hasSize(1);
        publicationDeliveryTestHelper.hasOriginalId(stopPlaceToBeMatched.getId(), result.get(0));
        publicationDeliveryTestHelper.hasOriginalId(incomingStopPlace.getId(), result.get(0));
    }



    /**
     * Real life example: Two stops with different IDs should be merged into one, and their quays should be added.
     *
     * @throws Exception
     */
    //No longer used because stop place merge has been de activated
    // @Test
    public void publicationDeliveryWithDuplicateStopPlaceWithDifferentId() throws Exception {

        String name = "Varnaveien bensin";

        StopPlace stopPlace = new StopPlace()
                .withName(new MultilingualString().withValue(name))
                .withId("OST:StopArea:01360680")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.ONSTREET_BUS)
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("59.4172358106178"))
                                .withLongitude(new BigDecimal("10.66847409589632"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withId("OST:StopArea:0136068001")
                                .withVersion("1")
                                .withSiteRef(new SiteRefStructure().withValue("OST:StopArea:01360680").withRef("OST:StopArea:01360680"))
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withName(new MultilingualString().withValue(name))
                                .withCentroid(new SimplePoint_VersionStructure()
                                        .withLocation(new LocationStructure()
                                                .withLongitude(new BigDecimal("10.6684740958963200085918288095854222774505615234375"))
                                                .withLatitude(new BigDecimal("59.41723581061779668743838556110858917236328125")))))));


        StopPlace stopPlace2 = new StopPlace()
                .withName(new MultilingualString().withValue(name))
                .withId("OST:StopArea:01040720")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.ONSTREET_BUS)
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("59.41727956639375"))
                                .withLongitude(new BigDecimal("10.66866436373097"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withId("OST:StopArea:0104072001")
                                .withVersion("1")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("OST:StopArea:01040720").withRef("OST:StopArea:01040720"))
                                .withName(new MultilingualString().withValue(name))
                                .withCentroid(new SimplePoint_VersionStructure()
                                        .withLocation(new LocationStructure()
                                                .withLongitude(new BigDecimal("10.6686643637309706122096031322143971920013427734375"))
                                                .withLatitude(new BigDecimal("59.41727956639375207714692805893719196319580078125")))))));


        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace, stopPlace2);

        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        List<StopPlace> result = publicationDeliveryTestHelper.extractStopPlaces(response, false);

        assertThat(result).as("Expecting one stop place in return, as there is no need to return the same matching stop place twice").hasSize(1);
        String importedIds = result.get(0).getKeyList().getKeyValue()
                .stream()
                .filter(kv -> "imported-id".equals(kv.getKey()))
                .map(KeyValueStructure::getValue)
                .findFirst()
                .get();
        assertThat(importedIds).contains(stopPlace.getId());
        assertThat(importedIds).contains(stopPlace2.getId());
        assertThat(result.get(0).getQuays().getQuayRefOrQuay()).hasSize(2);
    }

    @Test
    public void publicationDeliveryWithStopPlaceAndQuay() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("NSR:StopPlace:123123")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withName(new MultilingualString().withValue("SP1").withLang("no"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("9"))
                                .withLongitude(new BigDecimal("71"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withId("XYZ:Quay:4")
                                .withVersion("1")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("NSR:StopPlace:123123").withRef("NSR:StopPlace:123123"))
                                .withPrivateCode(new PrivateCodeStructure().withValue("B02").withType("type"))
                                .withName(new MultilingualString().withValue("quay"))
                                .withCentroid(new SimplePoint_VersionStructure()
                                        .withLocation(new LocationStructure()
                                                .withLatitude(new BigDecimal("9.1"))
                                                .withLongitude(new BigDecimal("71.2")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);

        PublicationDeliveryStructure firstResponse = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(firstResponse);

        assertThat(actualStopPlace.getQuays()).isNotNull().as("quays should not be null");

        Quay quay = actualStopPlace.getQuays()
                .getQuayRefOrQuay().stream()
                .filter(object -> object.getValue() instanceof Quay)
                .map(object -> ((Quay) object.getValue()))
                .findFirst()
                .get();


        assertThat(quay.getName().getValue()).isEqualTo("quay");
        assertThat(quay.getId()).isNotNull();
        assertThat(quay.getPrivateCode().getValue()).isEqualTo("B02");
        assertThat(quay.getPrivateCode().getType()).isEqualTo("type");

    }

    /**
     * When importing a stop place witch is a direct match with import type MERGE. No changes should be made to the stop place.
     * <p>
     * https://rutebanken.atlassian.net/browse/NRP-1587
     */
    @Test
    public void initialImportThenMergeShouldNotMergeNearbyQuays() throws Exception {

        // Quays with different original ID
        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:StopPlace:123123")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withName(new MultilingualString().withValue("fromStopPlace").withLang("no"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("9"))
                                .withLongitude(new BigDecimal("71"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withId("XYZ:Quay:4")
                                .withVersion("1")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("XYZ:StopPlace:123123").withRef("XYZ:StopPlace:123123"))
                                .withCentroid(new SimplePoint_VersionStructure()
                                        .withLocation(new LocationStructure()
                                                .withLatitude(new BigDecimal("9.1"))
                                                .withLongitude(new BigDecimal("71.2"))))))
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withId("XYZ:Quay:5")
                                .withVersion("1")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("XYZ:StopPlace:123123").withRef("XYZ:StopPlace:123123"))
                                .withCentroid(new SimplePoint_VersionStructure()
                                        .withLocation(new LocationStructure()
                                                .withLatitude(new BigDecimal("9.1"))
                                                .withLongitude(new BigDecimal("71.2")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);

        ImportParams importParams = createStandardParamsForImport();
        publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery, importParams);

        importParams.importType = ImportType.MERGE;
        PublicationDeliveryStructure mergeResponse = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery, importParams);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(mergeResponse);

        assertThat(actualStopPlace.getQuays()).isNotNull().as("quays should not be null");

        List<Quay> quays = publicationDeliveryTestHelper.extractQuays(actualStopPlace);

        assertThat(quays).hasSize(2);

        quays.forEach(quay -> {
            Set<String> importedIds = publicationDeliveryHelper.getImportedIds(quay);
            assertThat(importedIds).hasSize(1);
        });

    }

    /**
     * https://rutebanken.atlassian.net/browse/NRP-830
     */
    @Test
    public void handleChangesToQuaysWithoutSavingDuplicates() throws Exception {

        /**
         * StopPlace{name=Fredheimveien (no),
         *      quays=[Quay{name=Fredheimveien (no), centroid=POINT (11.142676854561447 59.83314448493502), keyValues={imported-id=Value{id=0, items=[RUT:StopArea:0229012201]}}},
         *        Quay{name=Fredheimveien (no), centroid=POINT (11.142897636770531 59.83297022041692), keyValues={imported-id=Value{id=0, items=[RUT:StopArea:0229012202]}}}],
         *    centroid=POINT (11.142676854561447 59.83314448493502),
         *    keyValues={imported-id=Value{id=0, items=[RUT:StopArea:02290122]}}}
         */
        MultilingualString name = new MultilingualString().withValue("Fredheimveien").withLang("no");


        StopPlace stopPlace1 = new StopPlace()
                .withId("RUT:StopArea:02290122")
                .withVersion("1")
                .withName(name)
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("59.83314448493502"))
                                .withLongitude(new BigDecimal("11.142676854561447"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                        .withId("RUT:StopArea:0229012201")
                                        .withVersion("1")
                                        .withName(name)
                                        .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                        .withSiteRef(new SiteRefStructure().withValue("RUT:StopArea:02290122").withRef("RUT:StopArea:02290122"))
                                        .withCentroid(new SimplePoint_VersionStructure()
                                                .withLocation(new LocationStructure()
                                                        .withLatitude(new BigDecimal("59.83314448493502"))
                                                        .withLongitude(new BigDecimal("11.142676854561447"))))),
                                netexObjectFactory.createQuay(new Quay()
                                        .withId("RUT:StopArea:0229012202")
                                        .withVersion("1")
                                        .withName(name)
                                        .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                        .withSiteRef(new SiteRefStructure().withValue("RUT:StopArea:02290122").withRef("RUT:StopArea:02290122"))
                                        .withCentroid(new SimplePoint_VersionStructure()
                                                .withLocation(new LocationStructure()
                                                        .withLatitude(new BigDecimal("59.83297022041692"))
                                                        .withLongitude(new BigDecimal("11.142897636770531")))))
                        ));

        /**
         * StopPlace{name=Fredheimveien (no),
         *      quays=[Quay{name=Fredheimveien (no), centroid=POINT (11.142902250197631 59.83304200609072), keyValues={imported-id=Value{id=0, items=[RUT:StopArea:0229012201]}}},
         *          Quay{name=Fredheimveien (no), centroid=POINT (11.14317535486387 59.832848923825956), keyValues={imported-id=Value{id=0, items=[RUT:StopArea:0229012202]}}}],
         *
         *  centroid=POINT (11.142902250197631 59.83304200609072),
         *  keyValues={imported-id=Value{id=0, items=[RUT:StopArea:02290122]}}}
         *
         */
        StopPlace stopPlace2 = new StopPlace()
                .withId("RUT:StopArea:02290122")
                .withVersion("1")
                .withName(name)
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("59.83304200609072"))
                                .withLongitude(new BigDecimal("11.142902250197631"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(
                                netexObjectFactory.createQuay(new Quay()
                                        .withId("BRA:StopArea:0229012201")
                                        .withVersion("1")
                                        .withName(name)
                                        .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                        .withSiteRef(new SiteRefStructure().withValue("RUT:StopArea:02290122").withRef("RUT:StopArea:02290122"))
                                        .withCentroid(new SimplePoint_VersionStructure()
                                                .withLocation(new LocationStructure()
                                                        .withLatitude(new BigDecimal("59.83304200609072"))
                                                        .withLongitude(new BigDecimal("11.142902250197631"))))),
                                netexObjectFactory.createQuay(new Quay()
                                        .withId("BRA:StopArea:0229012202")
                                        .withVersion("1")
                                        .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                        .withName(name)
                                        .withSiteRef(new SiteRefStructure().withValue("RUT:StopArea:02290122").withRef("RUT:StopArea:02290122"))
                                        .withCentroid(new SimplePoint_VersionStructure()
                                                .withLocation(new LocationStructure()
                                                        .withLatitude(new BigDecimal("59.832848923825956"))
                                                        .withLongitude(new BigDecimal("11.14317535486387")))))
                        ));

        List<PublicationDeliveryStructure> publicationDeliveryStructures = new ArrayList<>();

        publicationDeliveryStructures.add(publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace1));
        publicationDeliveryStructures.add(publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace2));

        for (PublicationDeliveryStructure pubde : publicationDeliveryStructures) {
            PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(pubde);
            StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(response);
            assertThat(actualStopPlace.getQuays().getQuayRefOrQuay()).hasSize(2);
            List<Quay> quays = publicationDeliveryTestHelper.extractQuays(actualStopPlace);

            long matches = quays
                    .stream()
                    .map(quay -> quay.getKeyList())
                    .flatMap(keyList -> keyList.getKeyValue().stream())
                    .map(keyValue -> keyValue.getValue())
                    .map(value -> value.split(","))
                    .flatMap(values -> Stream.of(values))
                    .filter(value -> value.equals("RUT:StopArea:0229012202") || value.equals("RUT:StopArea:0229012201"))
                    .count();
            assertThat(matches)
                    .as("Expecting quay to contain two matching orignal IDs in key val")
                    .isEqualTo(2);

//            assertThat(quays)
//                    .extracting(Quay::getKeyList)
//                    .extracting(KeyListStructure::getKeyValue)
//                    .extracting(KeyValueStructure::getValue)
//                    .contains("RUT:StopArea:0229012202");
        }
    }

    /**
     * Import stop place StopPlace{name=Skaret (no), quays=
     * [Quay{name=Skaret (no), centroid=POINT (7.328336965528884 62.799557598196465), keyValues={imported-id=Value{id=0, items=[MOR:StopArea:1548612801]}}},
     * Quay{name=Skaret (no), keyValues={imported-id=Value{id=0, items=[MOR:StopArea:1548575301]}}}],
     * keyValues={imported-id=Value{id=0, items=[MOR:StopArea:15485753]}}}
     */
    @Test
    public void importStopWithoutCoordinatesWithQuays1() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("MOR:StopArea:15485753")
                .withVersion("1")
                .withName(new MultilingualString().withValue("Skaret").withLang("no"))
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                        .withVersion("1")
                                        .withId("MOR:StopArea:1548612801")
                                        .withName(new MultilingualString().withValue("Skaret").withLang("no"))
                                        .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                        .withSiteRef(new SiteRefStructure().withValue("MOR:StopArea:15485753").withRef("MOR:StopArea:15485753"))
                                        .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                                .withLatitude(new BigDecimal("62.799557598196465"))
                                                .withLongitude(new BigDecimal("7.328336965528884"))))),
                                netexObjectFactory.createQuay(new Quay()
                                        .withId("MOR:StopArea:1548575301")
                                        .withVersion("1")
                                        .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                        .withSiteRef(new SiteRefStructure().withValue("MOR:StopArea:15485753").withRef("MOR:StopArea:15485753"))
                                        .withName(new MultilingualString().withValue("Skaret").withLang("no")))));



        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);

        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        // Exception should not have been thrown
        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(response);

        List<Quay> actualQuays = publicationDeliveryTestHelper.extractQuays(actualStopPlace);
        assertThat(actualQuays).isNotNull().as("quays should not be null");
    }

    @Test
    public void createdAndChangedTimestampsMustBeSetOnStopPlace() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:StopPlace:4")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withName(new MultilingualString().withValue("new"))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("XYZ:Quay:5")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withName(new MultilingualString().withValue("new quay"))
                                .withSiteRef(new SiteRefStructure().withValue("XYZ:StopPlace:4").withRef("XYZ:StopPlace:4"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("62.799557598196465"))
                                        .withLongitude(new BigDecimal("7.328336965528884")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);

        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(response);
        assertThat(actualStopPlace.getCreated()).as("The imported stop place's created date must not be null").isNotNull();

        //Created field in quays are no longer initialized
        //List<Quay> actualQuays = publicationDeliveryTestHelper.extractQuays(actualStopPlace);
       // assertThat(actualQuays.get(0).getCreated()).as("The imported quay's created date must not be null").isNotNull();
    }

    @Test
    public void validityMustBeSetOnImportedStop() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:StopPlace:123")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.ONSTREET_BUS)
                .withVersion("1")
                .withName(new MultilingualString().withValue("new"))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("RUT:StopArea:87654")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("RUT:StopPlace:123").withRef("RUT:StopPlace:123"))
                                .withName(new MultilingualString().withValue("q1").withLang("no"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);

        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(response);

        List<ValidBetween> actualValidBetween = actualStopPlace.getValidBetween();

        assertThat(actualValidBetween)
                .as("Stop Place should have actualValidBetween set")
                .isNotNull()
                .isNotEmpty()
                .hasSize(1);

        ValidBetween validBetween = actualValidBetween.get(0);
        assertThat(validBetween.getFromDate())
                .as("From date should be set")
                .isNotNull();

    }

    @Test
    public void updateStopPlaceShouldHaveItsDateChanged() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:StopPlace:123")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withVersion("1")
                .withName(new MultilingualString().withValue("new"))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("RUT:StopArea:87654")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("XYZ:StopPlace:123").withRef("XYZ:StopPlace:123"))
                                .withName(new MultilingualString().withValue("q1").withLang("no"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));

        PublicationDeliveryStructure firstPublicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);
        PublicationDeliveryStructure firstResponse = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(firstPublicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(firstResponse);
        LocalDateTime changedDate = actualStopPlace.getChanged();

        // Add a Quay to the stop place so that it will be updated.
        stopPlace.withQuays(
                new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("XYZ:StopPlace:123").withRef("XYZ:StopPlace:123"))
                                .withId("XYZ:Quay:321")
                                .withName(new MultilingualString().withValue("new quay"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("62.799557598196465"))
                                        .withLongitude(new BigDecimal("7.328336965528884")))))));

        PublicationDeliveryStructure secondPublicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);
        PublicationDeliveryStructure secondResponse = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(secondPublicationDelivery);

        StopPlace changedStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(secondResponse);
        assertThat(changedDate).as("The changed date for stop should not be the same as the first time it was imported")
                .isNotEqualTo(changedStopPlace.getChanged());
    }


    @Test
    public void importStopPlaceWithoutCoordinates() throws Exception {

        String chouetteId = "OPP:StopArea:123";

        StopPlace stopPlace = new StopPlace()
                .withId(chouetteId)
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withName(new MultilingualString().withValue("SP1"))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withName(new MultilingualString().withValue("quay"))
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("OPP:StopArea:123").withRef("OPP:StopArea:123k"))
                                .withId("XYZ:Quay:1")
                                .withVersion("1"))));

        PublicationDeliveryStructure firstPublicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);
        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(firstPublicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(response);

        assertThat(actualStopPlace).isNotNull();

    }

    @Test
    public void matchStopPlaceWithoutCoordinates() throws Exception {

        String chouetteId = "HED:StopArea:321321";

        StopPlace stopPlace = new StopPlace()
                .withId(chouetteId)
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withName(new MultilingualString().withValue("fromStopPlace").withLang("no"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("9"))
                                .withLongitude(new BigDecimal("71"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId(chouetteId + 1)
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("HED:StopArea:321321").withRef("HED:StopArea:321321"))
                                .withName(new MultilingualString().withValue("quay"))
                                .withCentroid(new SimplePoint_VersionStructure()
                                        .withLocation(new LocationStructure()
                                                .withLatitude(new BigDecimal("9.1"))
                                                .withLongitude(new BigDecimal("71.2")))))));

        PublicationDeliveryStructure firstPublicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);
        PublicationDeliveryStructure firstResponse = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(firstPublicationDelivery);
        StopPlace firstStopPlaceReturned = publicationDeliveryTestHelper.findFirstStopPlace(firstResponse);
        // Same ID, but no coordinates
        StopPlace stopPlaceWithoutCoordinates = new StopPlace()
                .withId(chouetteId)
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withVersion("1")
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withId("XYZ:Quay:1")
                                .withVersion("1")
                                .withSiteRef(new SiteRefStructure().withValue(chouetteId).withRef(chouetteId))
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withName(new MultilingualString().withValue("quay")))));

        PublicationDeliveryStructure secondPublicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlaceWithoutCoordinates);
        PublicationDeliveryStructure secondResponse = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(secondPublicationDelivery);

        StopPlace secondStopPlaceReturned = publicationDeliveryTestHelper.findFirstStopPlace(secondResponse);
        assertThat(secondStopPlaceReturned.getId()).isEqualTo(firstStopPlaceReturned.getId())
                .as("Expecting IDs to be the same, because the chouette ID is the same");

    }

    @Test
    public void importPublicationDeliveryAndExpectMappedIdInReturn() throws Exception {

        String originalQuayId = "XYZ:Quay:321321";

        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:StopPlace:123123")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withName(new MultilingualString().withValue("SP1").withLang("no"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("9"))
                                .withLongitude(new BigDecimal("71"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withId(originalQuayId)
                                .withVersion("1")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("XYZ:StopPlace:123123").withRef("XYZ:StopPlace:123123"))
                                .withName(new MultilingualString().withValue("quay"))
                                .withCentroid(new SimplePoint_VersionStructure()
                                        .withLocation(new LocationStructure()
                                                .withLatitude(new BigDecimal("9.1"))
                                                .withLongitude(new BigDecimal("71.2")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);
        PublicationDeliveryStructure firstResponse = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(firstResponse);

        publicationDeliveryTestHelper.hasOriginalId(stopPlace.getId(), actualStopPlace);

        Quay quay = actualStopPlace.getQuays()
                .getQuayRefOrQuay()
                .stream()
                .peek(object -> System.out.println(object))
                .filter(object -> object.getValue() instanceof Quay)
                .map(object -> ((Quay) object.getValue()))
                .peek(q -> System.out.println(q))
                .findFirst().get();

        publicationDeliveryTestHelper.hasOriginalId(originalQuayId, quay);
    }

    @Test
    @Ignore //no remove is done on names in mobiiti
    public void importPublicationDeliveryAndExpectCertainWordsToBeRemovedFromNames() throws Exception {
        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:stoparea:1")
                .withVersion("1")
                .withName(new MultilingualString().withValue("Steinerskolen Moss (Buss)"))
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("9"))
                                .withLongitude(new BigDecimal("71"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withId("XYZ:boardingpos:2")
                                .withVersion("1")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("RUT:StopPlace:1").withRef("RUT:StopPlace:1"))
                                .withName(new MultilingualString().withValue("Steinerskolen [tog]"))
                                .withCentroid(new SimplePoint_VersionStructure()
                                        .withLocation(new LocationStructure()
                                                .withLatitude(new BigDecimal("9.1"))
                                                .withLongitude(new BigDecimal("71.2")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);
        PublicationDeliveryStructure firstResponse = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(firstResponse);

        Quay quay = actualStopPlace.getQuays()
                .getQuayRefOrQuay()
                .stream()
                .peek(object -> System.out.println(object))
                .filter(object -> object.getValue() instanceof Quay)
                .map(object -> ((Quay) object.getValue()))
                .peek(q -> System.out.println(q))
                .findFirst().get();

        assertThat(actualStopPlace.getName().getValue()).isEqualTo("Steinerskolen Moss");
        assertThat(quay.getName().getValue()).isEqualTo("Steinerskolen");

    }

    @Test
    @Ignore // no remove is done on child now, even if it is same as parent
    public void expectQuayNameToBeRemovedIfSameAsParentStopPlaceName() throws Exception {
        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:stoparea:2")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withName(new MultilingualString().withValue("Fleskeby sentrum"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("9"))
                                .withLongitude(new BigDecimal("71"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withId("XYZ:boardingpos:2")
                                .withVersion("1")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("XYZ:stoparea:2").withRef("XYZ:stoparea:2"))
                                .withName(new MultilingualString().withValue("Fleskeby sentrum"))
                                .withCentroid(new SimplePoint_VersionStructure()
                                        .withLocation(new LocationStructure()
                                                .withLatitude(new BigDecimal("9.1"))
                                                .withLongitude(new BigDecimal("71.2")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);
        PublicationDeliveryStructure firstResponse = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(firstResponse);

        Quay quay = actualStopPlace.getQuays()
                .getQuayRefOrQuay()
                .stream()
                .peek(object -> System.out.println(object))
                .filter(object -> object.getValue() instanceof Quay)
                .map(object -> ((Quay) object.getValue()))
                .peek(q -> System.out.println(q))
                .findFirst().get();

        assertThat(actualStopPlace.getName().getValue()).isEqualTo("Fleskeby sentrum");
        assertThat(quay.getName()).isNull();
    }

    @Test
    public void computeStopPlaceCentroid() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:StopPlace:9")
                .withVersion("1")
                .withName(new MultilingualString().withValue("SP9").withLang("no"))
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("1"))
                                .withLongitude(new BigDecimal("2"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                        .withId("XYZ:Quay:9")
                                        .withVersion("1")
                                        .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                        .withSiteRef(new SiteRefStructure().withValue("XYZ:StopPlace:9").withRef("XYZ:StopPlace:9"))
                                        .withName(new MultilingualString().withValue("quay number one"))
                                        .withCentroid(new SimplePoint_VersionStructure()
                                                .withId("12")
                                                .withVersion("1")
                                                .withLocation(new LocationStructure()
                                                        .withLatitude(new BigDecimal("10"))
                                                        .withLongitude(new BigDecimal("20"))))),
                                netexObjectFactory.createQuay(new Quay()
                                        .withId("XYZ:Quay:133")
                                        .withVersion("1")
                                        .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                        .withSiteRef(new SiteRefStructure().withValue("XYZ:StopPlace:9").withRef("XYZ:StopPlace:9"))
                                        .withName(new MultilingualString().withValue("quay number two"))
                                        .withCentroid(new SimplePoint_VersionStructure()
                                                .withId("30")
                                                .withVersion("1")
                                                .withLocation(new LocationStructure()
                                                        .withLatitude(new BigDecimal("10.0002"))
                                                        .withLongitude(new BigDecimal("20.0002")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);

        PublicationDeliveryStructure firstResponse = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(firstResponse);

        assertThat(actualStopPlace.getCentroid().getLocation().getLongitude().doubleValue()).isEqualTo(20.0001);
        assertThat(actualStopPlace.getCentroid().getLocation().getLatitude().doubleValue()).isEqualTo(10.0001);
    }

    @Test
    public void maxNumberOfDigitsInCoordinatesShouldBeSix() throws Exception {

        final int maxdigits = 6;

        StopPlace stopPlace = new StopPlace()
                .withId("XYZ:StopPlace:91")
                .withVersion("1")
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withName(new MultilingualString().withValue("fromStopPlace").withLang("no"))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("10.123456789123456789123456789"))
                                .withLongitude(new BigDecimal("20.123456789123456789123456789"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withId("XYZ:Quay:91")
                                .withVersion("1")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("XYZ:StopPlace:91").withRef("XYZ:StopPlace:91"))
                                .withName(new MultilingualString().withValue("quay number one"))
                                .withCentroid(new SimplePoint_VersionStructure()
                                        .withVersion("1")
                                        .withLocation(new LocationStructure()
                                                .withLatitude(new BigDecimal("10.123456789123456789123456789"))
                                                .withLongitude(new BigDecimal("20.123456789123456789123456789")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);

        PublicationDeliveryStructure firstResponse = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(firstResponse);

        BigDecimal longitude = actualStopPlace.getCentroid().getLocation().getLongitude();
        BigDecimal latitude = actualStopPlace.getCentroid().getLocation().getLatitude();

        assertThat(String.valueOf(longitude).split("\\.")[1].length()).as("longitude decimals length").isLessThanOrEqualTo(maxdigits);
        assertThat(String.valueOf(latitude).split("\\.")[1].length()).as("latitude decimals length").isLessThanOrEqualTo(maxdigits);

    }

    @Test
    public void importPublicationDeliveryAndVerifyStatusCode200() throws Exception {

        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<PublicationDelivery version=\"1.0\" xmlns=\"http://www.netex.org.uk/netex\"\n" +
                "                     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "                     xsi:schemaLocation=\"http://www.netex.org.uk/netex ../../xsd/NeTEx_publication.xsd\">\n" +
                "    <PublicationTimestamp>2016-05-18T15:00:00.0Z</PublicationTimestamp>\n" +
                "    <ParticipantRef>NHR</ParticipantRef>\n" +
                "    <dataObjects>\n" +
                "        <SiteFrame version=\"01\" id=\"nhr:sf:1\">\n" +
                "            <FrameDefaults>\n" +
                "               <DefaultLocale>\n" +
                "                   <TimeZone>Europe/Oslo</TimeZone>\n" +
                "                   <DefaultLanguage>no</DefaultLanguage>\n" +
                "               </DefaultLocale>\n" +
                "            </FrameDefaults>\n" +
                "            <stopPlaces>\n" +
                "                <StopPlace version=\"01\" created=\"2016-04-21T09:00:00.0Z\" id=\"nhr:sp:1\">\n" +
                "                    <ValidBetween>\n" +
                "                        <FromDate>2017-05-11T10:20:27.394+02:00</FromDate>\n" +
                "                    </ValidBetween>" +
                "                    <Name lang=\"no-NO\">Krokstien</Name>\n" +
                "                    <Centroid>\n" +
                "                        <Location srsName=\"EPSG:4326\">\n" +
                "                            <Longitude>10.8577903</Longitude>\n" +
                "                            <Latitude>59.910579</Latitude>\n" +
                "                        </Location>\n" +
                "                    </Centroid>\n" +
                "                    <TransportMode>bus</TransportMode>\n" +
                "                    <StopPlaceType>onstreetBus</StopPlaceType>\n" +
                "                    <quays>\n" +
                "                        <Quay version=\"01\" created=\"2016-04-21T09:01:00.0Z\" id=\"nhr:Quay:1\">\n" +
                "               <Name lang=\"no\">q1</Name>\n" +
                "                            <Centroid>\n" +
                "                                <Location srsName=\"EPSG:4326\">\n" +
                "                                    <Longitude>10.8577903</Longitude>\n" +
                "                                    <Latitude>59.910579</Latitude>\n" +
                "                                </Location>\n" +
                "                            </Centroid>\n" +
                "                            <Covered>outdoors</Covered>\n" +
                "                            <Lighting>wellLit</Lighting>\n" +
                "    <SiteRef ref=\"nhr:sp:\"/> \n"+
                "                    <TransportMode>bus</TransportMode>\n" +
                "                            <QuayType>busStop</QuayType>\n" +
                "                        </Quay>\n" +
                "                    </quays>\n" +
                "                </StopPlace>\n" +
                "            </stopPlaces>\n" +
                "        </SiteFrame>\n" +
                "    </dataObjects>\n" +
                "</PublicationDelivery>";


        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        ImportParams importParams = createStandardParamsForImport();

        Response response = importResource.importPublicationDelivery(stream, importParams);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void importStopPlaceWithMultipleValidBetweenPeriods() throws Exception {
        LocalDateTime firstValidFrom = now.plusSeconds(1);
        LocalDateTime secondValidFrom = now.plusSeconds(2);
        StopPlace stopPlace1 = new StopPlace()
                .withId("XYZ:Stopplace:1")
                .withVersion("1")
                .withName(new MultilingualString().withValue("New stop1"))
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withValidBetween(new ValidBetween().withFromDate(firstValidFrom).withToDate(secondValidFrom), new ValidBetween().withFromDate(secondValidFrom))
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("59.914353"))
                                .withLongitude(new BigDecimal("10.806387"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("RUT:StopArea:87654")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue("XYZ:Stopplace:1").withRef("XYZ:Stopplace:1"))
                                .withName(new MultilingualString().withValue("q1").withLang("no"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace1);
        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        List<StopPlace> changedStopPlaces = publicationDeliveryTestHelper.extractStopPlaces(response, false);
        Assert.assertEquals(1, changedStopPlaces.size());
        StopPlace stopPlace = changedStopPlaces.get(0);

        List<ValidBetween> actualValidBetween = stopPlace.getValidBetween();

        assertThat(actualValidBetween)
                .as("Stop Place should have actualValidBetween set")
                .isNotNull()
                .isNotEmpty()
                .hasSize(1);

        //imported objects have "to" field set to null
        assertNull(actualValidBetween.get(0).getToDate());
    }

    /**
     * Partially copied from https://github.com/rutebanken/netex-norway-examples/blob/master/examples/stops/BasicStopPlace_example.xml
     */
    @Test
    @Ignore
    public void importBasicStopPlace() throws JAXBException, IOException, SAXException, TiamatBusinessException {

        String xml = "<PublicationDelivery\n" +
                " version=\"any\"\n" +
                " xmlns=\"http://www.netex.org.uk/netex\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://www.netex.org.uk/netex ../../xsd/NeTEx_publication.xsd\">\n" +
                " <!-- Når denne dataleveransen ble generert -->\n" +
                " <PublicationTimestamp>2016-05-18T15:00:00.0Z</PublicationTimestamp>\n" +
                " <ParticipantRef>NHR</ParticipantRef>\n" +
                " <dataObjects>\n" +
                "  <SiteFrame version=\"any\" id=\"nhr:sf:1\">\n" +
                "   <FrameDefaults>\n" +
                "     <DefaultLocale>\n" +
                "       <TimeZone>Europe/Oslo</TimeZone>\n" +
                "       <DefaultLanguage>no</DefaultLanguage>\n" +
                "     </DefaultLocale>\n" +
                "   </FrameDefaults>\n" +
                "   <stopPlaces>\n" +
                "    <!--===Stop=== -->\n" +
                "    <!-- Merk: Holdeplass-ID vil komme fra Holdeplassregisteret -->\n" +
                "    <StopPlace version=\"1\" created=\"2016-04-21T09:00:00.0Z\" id=\"nhr:sp:2\">\n" +
                "     <Name lang=\"no-NO\">Krokstien</Name>\n" +
                "         <TransportMode>bus</TransportMode>\n" +
                "    </StopPlace>\n" +
                "   </stopPlaces>\n" +
                "  </SiteFrame>\n" +
                " </dataObjects>\n" +
                "</PublicationDelivery>\n" +
                "\n";

        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        ImportParams importParams = createStandardParamsForImport();
        Response response = importResource.importPublicationDelivery(stream,importParams);
        assertThat(response.getStatus()).isEqualTo(200);

        StreamingOutput streamingOutput = (StreamingOutput) response.getEntity();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        streamingOutput.write(byteArrayOutputStream);
        System.out.println(byteArrayOutputStream.toString());
    }

    @Test
    public void importNSBStopPlace() throws JAXBException, IOException, SAXException, TiamatBusinessException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<PublicationDelivery xmlns=\"http://www.netex.org.uk/netex\">\n" +
                "   <PublicationTimestamp>2017-04-18T12:57:27.796+02:00</PublicationTimestamp>\n" +
                "   <ParticipantRef>NSB</ParticipantRef>\n" +
                "   <Description>NSB Grails stasjoner til NeTex</Description>\n" +
                "   <dataObjects>\n" +
                "      <SiteFrame id=\"NSB:SiteFrame:1\" version=\"1\">\n" +
                "         <codespaces>\n" +
                "            <Codespace id=\"nsb\">\n" +
                "               <Xmlns>NSB</Xmlns>\n" +
                "               <XmlnsUrl>http://www.rutebanken.org/ns/nsb</XmlnsUrl>\n" +
                "            </Codespace>\n" +
                "         </codespaces>\n" +
                "         <FrameDefaults>\n" +
                "           <DefaultLocale>\n" +
                "               <TimeZone>Europe/Oslo</TimeZone>\n" +
                "               <DefaultLanguage>no</DefaultLanguage>\n" +
                "           </DefaultLocale>\n" +
                "         </FrameDefaults>\n" +
                "         <stopPlaces>\n" +
                "   \n" +
                "   \n" +
                "            <StopPlace id=\"NSB:StopPlace:007602146\" version=\"1\">\n" +
                "               <keyList>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>grailsId</Key>\n" +
                "                     <Value>3</Value>\n" +
                "                  </KeyValue>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>lisaId</Key>\n" +
                "                     <Value>2146</Value>\n" +
                "                  </KeyValue>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>jbvCode</Key>\n" +
                "                     <Value>ADL</Value>\n" +
                "                  </KeyValue>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>iffCode</Key>\n" +
                "                     <Value>7602146</Value>\n" +
                "                  </KeyValue>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>uicCode</Key>\n" +
                "                     <Value>7602146</Value>\n" +
                "                  </KeyValue>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>imported-id</Key>\n" +
                "                     <Value>NRI:StopPlace:761037602</Value>\n" +
                "                  </KeyValue>\n" +
                "               </keyList>\n" +
                "               <Name lang=\"no\">Arendal</Name>\n" +
                "               <Centroid>\n" +
                "                  <Location srsName=\"EPSG:4326\"><!--Match on NRI quays--><Longitude>8.769146</Longitude>\n" +
                "                     <Latitude>58.465256</Latitude>\n" +
                "                  </Location>\n" +
                "               </Centroid>\n" +
                "               <Url>http://www.jernbaneverket.no/no/Jernbanen/Stasjonssok/-A-/Arendal/</Url>\n" +
                "               <PostalAddress id=\"NSB:PostalAddress:3\" version=\"1\">\n" +
                "                  <AddressLine1>Møllebakken 15</AddressLine1>\n" +
                "                  <AddressLine2> 4841 Arendal</AddressLine2>\n" +
                "               </PostalAddress>\n" +
                "               <AccessibilityAssessment id=\"NSB:AccessibilityAssessment:3\" version=\"1\">\n" +
                "                  <MobilityImpairedAccess>true</MobilityImpairedAccess>\n" +
                "                  <limitations>\n" +
                "                     <AccessibilityLimitation>\n" +
                "                        <WheelchairAccess>true</WheelchairAccess>\n" +
                "                        <StepFreeAccess>true</StepFreeAccess>\n" +
                "                     </AccessibilityLimitation>\n" +
                "                  </limitations>\n" +
                "               </AccessibilityAssessment>\n" +
                "               <placeEquipments>\n" +
                "                  <WaitingRoomEquipment id=\"NSB:WaitingRoomEquipment:3\" version=\"1\"/>\n" +
                "                  <SanitaryEquipment id=\"NSB:SanitaryEquipment:3\" version=\"1\">\n" +
                "                     <Gender>both</Gender>\n" +
                "                     <SanitaryFacilityList>toilet wheelchairAccessToilet</SanitaryFacilityList>\n" +
                "                  </SanitaryEquipment>\n" +
                "                  <TicketingEquipment id=\"NSB:TicketingEquipment:3\" version=\"1\">\n" +
                "                     <NumberOfMachines>1</NumberOfMachines>\n" +
                "                  </TicketingEquipment>\n" +
                "               </placeEquipments>\n" +
                "               <localServices>\n" +
                "                  <LeftLuggageService id=\"NSB:LeftLuggageService:3\" version=\"1\">\n" +
                "                     <SelfServiceLockers>true</SelfServiceLockers>\n" +
                "                  </LeftLuggageService>\n" +
                "                  <TicketingService id=\"NSB:TicketingService:3\" version=\"1\">\n" +
                "                     <TicketCounterService>true</TicketCounterService>\n" +
                "                  </TicketingService>\n" +
                "               </localServices>\n" +
                "         <TransportMode>bus</TransportMode>\n" +
                "               <StopPlaceType>busStation</StopPlaceType>\n" +
                "               <Weighting>interchangeAllowed</Weighting>\n" +
                "               <quays>\n" +
                "                  <Quay id=\"NSB:Quay:0076021461\" version=\"1\">\n" +
                "                     <keyList>\n" +
                "                        <KeyValue>\n" +
                "                           <Key>grails-platformId</Key>\n" +
                "                           <Value>825930</Value>\n" +
                "                        </KeyValue>\n" +
                "                        <KeyValue>\n" +
                "                           <Key>uicCode</Key>\n" +
                "                           <Value>7602146</Value>\n" +
                "                        </KeyValue>\n" +
                "                     </keyList>\n" +
                "               <Name lang=\"no\">q1</Name>\n" +
                "                     <Centroid>\n" +
                "                        <Location srsName=\"EPSG:4326\"><!--Match on NRI quays--><Longitude>8.769146</Longitude>\n" +
                "                           <Latitude>58.465256</Latitude>\n" +
                "                        </Location>\n" +
                "                     </Centroid>\n" +
                "    <SiteRef ref=\"NSB:StopPlace:007602146\"/> \n"+
                "         <TransportMode>bus</TransportMode>\n" +
                "                     <PublicCode>1</PublicCode>\n" +
                "                  </Quay>\n" +
                "               </quays>\n" +
                "            </StopPlace>\n" +
                "           </stopPlaces>" +
                "       </SiteFrame>" +
                "   </dataObjects>" +
                "</PublicationDelivery>";


        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        ImportParams importParams = createStandardParamsForImport();
        Response response = importResource.importPublicationDelivery(stream, importParams);
        assertThat(response.getStatus()).isEqualTo(200);

        StreamingOutput streamingOutput = (StreamingOutput) response.getEntity();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        streamingOutput.write(byteArrayOutputStream);
        System.out.println(byteArrayOutputStream.toString());
    }

    @Test
    public void importNSBStopPlaceWithTicketValidatorEquipment() throws JAXBException, IOException, SAXException, TiamatBusinessException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<PublicationDelivery xmlns=\"http://www.netex.org.uk/netex\">\n" +
                "   <PublicationTimestamp>2017-04-18T12:57:27.796+02:00</PublicationTimestamp>\n" +
                "   <ParticipantRef>NSB</ParticipantRef>\n" +
                "   <Description>NSB Grails stasjoner til NeTex</Description>\n" +
                "   <dataObjects>\n" +
                "      <SiteFrame id=\"NSB:SiteFrame:1\" version=\"1\">\n" +
                "         <codespaces>\n" +
                "            <Codespace id=\"nsb\">\n" +
                "               <Xmlns>NSB</Xmlns>\n" +
                "               <XmlnsUrl>http://www.rutebanken.org/ns/nsb</XmlnsUrl>\n" +
                "            </Codespace>\n" +
                "         </codespaces>\n" +
                "         <FrameDefaults>\n" +
                "           <DefaultLocale>\n" +
                "             <TimeZone>CET</TimeZone>\n" +
                "              <DefaultLanguage>no</DefaultLanguage>\n" +
                "           </DefaultLocale>\n" +
                "         </FrameDefaults>\n" +
                "         <stopPlaces>\n" +
                "            <StopPlace id=\"NSB:StopPlace:007602146\" version=\"1\">\n" +
                "               <keyList>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>grailsId</Key>\n" +
                "                     <Value>3</Value>\n" +
                "                  </KeyValue>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>lisaId</Key>\n" +
                "                     <Value>2146</Value>\n" +
                "                  </KeyValue>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>jbvCode</Key>\n" +
                "                     <Value>ADL</Value>\n" +
                "                  </KeyValue>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>iffCode</Key>\n" +
                "                     <Value>7602146</Value>\n" +
                "                  </KeyValue>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>uicCode</Key>\n" +
                "                     <Value>7602146</Value>\n" +
                "                  </KeyValue>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>imported-id</Key>\n" +
                "                     <Value>NRI:StopPlace:761037602</Value>\n" +
                "                  </KeyValue>\n" +
                "               </keyList>\n" +
                "               <Name lang=\"no\">Arendal</Name>\n" +
                "               <Centroid>\n" +
                "                  <Location srsName=\"EPSG:4326\"><!--Match on NRI quays--><Longitude>8.769146</Longitude>\n" +
                "                     <Latitude>58.465256</Latitude>\n" +
                "                  </Location>\n" +
                "               </Centroid>\n" +
                "               <placeEquipments>\n" +
                "                  <TicketValidatorEquipment id=\"NSB:TicketValidatorEquipment:88\" version=\"1\">\n" +
                "                     <TicketValidatorType>contactLess</TicketValidatorType>\n" +
                "                  </TicketValidatorEquipment>\n" +
                "               </placeEquipments>\n" +
                "         <TransportMode>bus</TransportMode>\n" +
                "               <StopPlaceType>railStation</StopPlaceType>\n" +
                "               <Weighting>interchangeAllowed</Weighting>\n" +
                "               <quays>\n" +
                "                  <Quay id=\"NSB:Quay:0076021461\" version=\"1\">\n" +
                "                     <keyList>\n" +
                "                        <KeyValue>\n" +
                "                           <Key>grails-platformId</Key>\n" +
                "                           <Value>825930</Value>\n" +
                "                        </KeyValue>\n" +
                "                        <KeyValue>\n" +
                "                           <Key>uicCode</Key>\n" +
                "                           <Value>7602146</Value>\n" +
                "                        </KeyValue>\n" +
                "                     </keyList>\n" +
                "               <Name lang=\"no\">Q1</Name>\n" +
                "                     <Centroid>\n" +
                "                        <Location srsName=\"EPSG:4326\"><!--Match on NRI quays--><Longitude>8.769146</Longitude>\n" +
                "                           <Latitude>58.465256</Latitude>\n" +
                "                        </Location>\n" +
                "                     </Centroid>\n" +
                "    <SiteRef ref=\"NSR:StopPlace:2\"/> \n"+
                "         <TransportMode>bus</TransportMode>\n" +
                "                     <PublicCode>1</PublicCode>\n" +
                "                  </Quay>\n" +
                "               </quays>\n" +
                "            </StopPlace>\n" +
                "           </stopPlaces>" +
                "       </SiteFrame>" +
                "   </dataObjects>" +
                "</PublicationDelivery>";


        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        ImportParams importParams = createStandardParamsForImport();

        Response response = importResource.importPublicationDelivery(stream, importParams);
        assertThat(response.getStatus()).isEqualTo(200);

        StreamingOutput streamingOutput = (StreamingOutput) response.getEntity();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        streamingOutput.write(byteArrayOutputStream);
        System.out.println(byteArrayOutputStream.toString());
    }

    @Test
    public void importBrakarStopPlaceWithGeneralSignEquipment() throws JAXBException, IOException, SAXException, TiamatBusinessException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<PublicationDelivery xmlns=\"http://www.netex.org.uk/netex\">\n" +
                "   <PublicationTimestamp>2017-04-18T12:57:27.796+02:00</PublicationTimestamp>\n" +
                "   <ParticipantRef>NSB</ParticipantRef>\n" +
                "   <Description>NSB Grails stasjoner til NeTex</Description>\n" +
                "   <dataObjects>\n" +
                "      <SiteFrame id=\"NSB:SiteFrame:1\" version=\"1\">\n" +
                "         <codespaces>\n" +
                "            <Codespace id=\"nsb\">\n" +
                "               <Xmlns>NSB</Xmlns>\n" +
                "               <XmlnsUrl>http://www.rutebanken.org/ns/nsb</XmlnsUrl>\n" +
                "            </Codespace>\n" +
                "         </codespaces>\n" +
                "        <FrameDefaults>\n" +
                "           <DefaultLocale>\n" +
                "              <TimeZone>Europe/Oslo</TimeZone>\n" +
                "               <DefaultLanguage>no</DefaultLanguage>\n" +
                "           </DefaultLocale>\n" +
                "         </FrameDefaults>\n" +
                "         <stopPlaces>\n" +
                "           <StopPlace id=\"BRA:StopPlace:06021002\" version=\"1\">\n" +
                "               <keyList>\n" +
                "                  <KeyValue>\n" +
                "                     <Key>imported-id</Key>\n" +
                "                     <Value>NRI:StopPlace:761023202</Value>\n" +
                "                  </KeyValue>\n" +
                "               </keyList>\n" +
                "               <Name lang=\"nb\">Bragernes torg</Name>\n" +
                "               <Centroid>\n" +
                "                  <Location srsName=\"EPSG:4326\">\n" +
                "                     <Longitude>10.203912</Longitude>\n" +
                "                     <Latitude>59.743416</Latitude>\n" +
                "                  </Location>\n" +
                "               </Centroid>\n" +
                "                    <TransportMode>bus</TransportMode>\n" +
                "               <StopPlaceType>busStation</StopPlaceType>\n" +
                "               <quays>\n" +
                "                  <Quay id=\"BRA:Quay:0602100201\" version=\"1\">\n" +
                "                     <keyList>\n" +
                "                        <KeyValue>\n" +
                "                           <Key>imported-id</Key>\n" +
                "                           <Value>NRI:Quay:762023206</Value>\n" +
                "                        </KeyValue>\n" +
                "                     </keyList>\n" +
                "               <Name lang=\"no\">q0602100201</Name>\n" +
                "                     <Centroid>\n" +
                "                        <Location srsName=\"EPSG:4326\">\n" +
                "                           <Longitude>10.203526</Longitude>\n" +
                "                           <Latitude>59.7432</Latitude>\n" +
                "                        </Location>\n" +
                "                     </Centroid>\n" +
                "                     <RoadAddress id=\"BRA:RoadAddress:0602100201\" version=\"1\">\n" +
                "                        <RoadName>v/Barista</RoadName>\n" +
                "                     </RoadAddress>\n" +
                "                     <AccessibilityAssessment id=\"BRA:AccessibilityAssessment:0602100201\" version=\"1\">\n" +
                "                        <MobilityImpairedAccess>true</MobilityImpairedAccess>\n" +
                "                        <limitations>\n" +
                "                           <AccessibilityLimitation>\n" +
                "                              <WheelchairAccess>true</WheelchairAccess>\n" +
                "                              <StepFreeAccess>true</StepFreeAccess>\n" +
                "                           </AccessibilityLimitation>\n" +
                "                        </limitations>\n" +
                "                     </AccessibilityAssessment>\n" +
                "                     <Lighting>wellLit</Lighting>\n" +
                "    <SiteRef ref=\"BRA:StopPlace:06021002\"/> \n"+
                "                     <placeEquipments>\n" +
                "                        <ShelterEquipment id=\"BRA:ShelterEquipment:0602100201\" version=\"1\">\n" +
                "                           <Enclosed>true</Enclosed>\n" +
                "                        </ShelterEquipment>\n" +
                "                        <GeneralSign id=\"BRA:GeneralSign:0602100201\" version=\"1\">\n" +
                "                           <PrivateCode>512</PrivateCode>\n" +
                "                           <SignContentType>transportMode</SignContentType>\n" +
                "                        </GeneralSign>\n" +
                "                        <!--TODO markere som papiroppslag--><GeneralSign id=\"BRA:HeadSign:0602100201\" version=\"1\">\n" +
                "                           <Content lang=\"nb\">Timetable</Content>\n" +
                "                        </GeneralSign>\n" +
                "                     </placeEquipments>\n" +
                "                    <TransportMode>bus</TransportMode>\n" +
                "                     <PublicCode>A</PublicCode>\n" +
                "                  </Quay>\n" +
                "                  <Quay id=\"BRA:Quay:0602100202\" version=\"1\">\n" +
                "                     <keyList>\n" +
                "                        <KeyValue>\n" +
                "                           <Key>imported-id</Key>\n" +
                "                           <Value>NRI:Quay:762023204</Value>\n" +
                "                        </KeyValue>\n" +
                "                     </keyList>\n" +
                "               <Name lang=\"no\">q0602100202</Name>\n" +
                "                     <Centroid>\n" +
                "                        <Location srsName=\"EPSG:4326\">\n" +
                "                           <Longitude>10.203558</Longitude>\n" +
                "                           <Latitude>59.74361</Latitude>\n" +
                "                        </Location>\n" +
                "                     </Centroid>\n" +
                "                     <RoadAddress id=\"BRA:RoadAddress:0602100202\" version=\"1\">\n" +
                "                        <RoadName>Øvre Storgate</RoadName>\n" +
                "                     </RoadAddress>\n" +
                "                     <AccessibilityAssessment id=\"BRA:AccessibilityAssessment:0602100202\" version=\"1\">\n" +
                "                        <MobilityImpairedAccess>true</MobilityImpairedAccess>\n" +
                "                        <limitations>\n" +
                "                           <AccessibilityLimitation>\n" +
                "                              <WheelchairAccess>true</WheelchairAccess>\n" +
                "                              <StepFreeAccess>true</StepFreeAccess>\n" +
                "                           </AccessibilityLimitation>\n" +
                "                        </limitations>\n" +
                "                     </AccessibilityAssessment>\n" +
                "                     <Lighting>wellLit</Lighting>\n" +
                "    <SiteRef ref=\"BRA:StopPlace:06021002\"/> \n"+
                "                     <placeEquipments>\n" +
                "                        <GeneralSign id=\"BRA:GeneralSign:0602100202\" version=\"1\">\n" +
                "                           <PrivateCode>512</PrivateCode>\n" +
                "                           <SignContentType>transportMode</SignContentType>\n" +
                "                        </GeneralSign>\n" +
                "                        <!--TODO markere som digital skjerm--><GeneralSign id=\"BRA:HeadSign:0602100202\" version=\"1\">\n" +
                "                           <Content lang=\"nb\">RealtimeMonitor</Content>\n" +
                "                        </GeneralSign>\n" +
                "                     </placeEquipments>\n" +
                "                    <TransportMode>bus</TransportMode>\n" +
                "                     <PublicCode>B</PublicCode>\n" +
                "                  </Quay>\n" +
                "                  <Quay id=\"BRA:Quay:0602100203\" version=\"1\">\n" +
                "                     <keyList>\n" +
                "                        <KeyValue>\n" +
                "                           <Key>imported-id</Key>\n" +
                "                           <Value>NRI:Quay:762023205</Value>\n" +
                "                        </KeyValue>\n" +
                "                     </keyList>\n" +
                "               <Name lang=\"no\">q0+02100203</Name>\n" +
                "                     <Centroid>\n" +
                "                        <Location srsName=\"EPSG:4326\">\n" +
                "                           <Longitude>10.203966</Longitude>\n" +
                "                           <Latitude>59.74358</Latitude>\n" +
                "                        </Location>\n" +
                "                     </Centroid>\n" +
                "                     <RoadAddress id=\"BRA:RoadAddress:0602100203\" version=\"1\">\n" +
                "                        <RoadName>mot torget</RoadName>\n" +
                "                     </RoadAddress>\n" +
                "                     <AccessibilityAssessment id=\"BRA:AccessibilityAssessment:0602100203\" version=\"1\">\n" +
                "                        <MobilityImpairedAccess>true</MobilityImpairedAccess>\n" +
                "                        <limitations>\n" +
                "                           <AccessibilityLimitation>\n" +
                "                              <WheelchairAccess>true</WheelchairAccess>\n" +
                "                              <StepFreeAccess>true</StepFreeAccess>\n" +
                "                           </AccessibilityLimitation>\n" +
                "                        </limitations>\n" +
                "                     </AccessibilityAssessment>\n" +
                "                     <Lighting>wellLit</Lighting>\n" +
                "    <SiteRef ref=\"BRA:StopPlace:06021002\"/> \n"+
                "                     <placeEquipments>\n" +
                "                        <ShelterEquipment id=\"BRA:ShelterEquipment:0602100203\" version=\"1\">\n" +
                "                           <Enclosed>true</Enclosed>\n" +
                "                        </ShelterEquipment>\n" +
                "                        <GeneralSign id=\"BRA:GeneralSign:0602100203\" version=\"1\">\n" +
                "                           <PrivateCode>512</PrivateCode>\n" +
                "                           <SignContentType>transportMode</SignContentType>\n" +
                "                        </GeneralSign>\n" +
                "                        <!--TODO markere som digital skjerm--><GeneralSign id=\"BRA:HeadSign:0602100203\" version=\"1\">\n" +
                "                           <Content lang=\"nb\">RealtimeMonitor</Content>\n" +
                "                        </GeneralSign>\n" +
                "                     </placeEquipments>\n" +
                "                    <TransportMode>bus</TransportMode>\n" +
                "                     <PublicCode>C</PublicCode>\n" +
                "                  </Quay>\n" +
                "                  <Quay id=\"BRA:Quay:0602100204\" version=\"1\">\n" +
                "                     <keyList>\n" +
                "                        <KeyValue>\n" +
                "                           <Key>imported-id</Key>\n" +
                "                           <Value>NRI:Quay:762023203</Value>\n" +
                "                        </KeyValue>\n" +
                "                     </keyList>\n" +
                "               <Name lang=\"no\">q0602100204</Name>\n" +
                "                     <Centroid>\n" +
                "                        <Location srsName=\"EPSG:4326\">\n" +
                "                           <Longitude>10.204596</Longitude>\n" +
                "                           <Latitude>59.743267</Latitude>\n" +
                "                        </Location>\n" +
                "                     </Centroid>\n" +
                "                     <RoadAddress id=\"BRA:RoadAddress:0602100204\" version=\"1\">\n" +
                "                        <RoadName>v/Børsen</RoadName>\n" +
                "                     </RoadAddress>\n" +
                "                     <AccessibilityAssessment id=\"BRA:AccessibilityAssessment:0602100204\" version=\"1\">\n" +
                "                        <MobilityImpairedAccess>true</MobilityImpairedAccess>\n" +
                "                        <limitations>\n" +
                "                           <AccessibilityLimitation>\n" +
                "                              <WheelchairAccess>true</WheelchairAccess>\n" +
                "                              <StepFreeAccess>true</StepFreeAccess>\n" +
                "                           </AccessibilityLimitation>\n" +
                "                        </limitations>\n" +
                "                     </AccessibilityAssessment>\n" +
                "                     <Lighting>wellLit</Lighting>\n" +
                "    <SiteRef ref=\"BRA:StopPlace:06021002\"/> \n"+
                "                     <placeEquipments>\n" +
                "                        <ShelterEquipment id=\"BRA:ShelterEquipment:0602100204\" version=\"1\">\n" +
                "                           <Enclosed>true</Enclosed>\n" +
                "                        </ShelterEquipment>\n" +
                "                        <GeneralSign id=\"BRA:GeneralSign:0602100204\" version=\"1\">\n" +
                "                           <PrivateCode>512</PrivateCode>\n" +
                "                           <SignContentType>transportMode</SignContentType>\n" +
                "                        </GeneralSign>\n" +
                "                        <!--TODO markere som papiroppslag--><GeneralSign id=\"BRA:HeadSign:0602100204\" version=\"1\">\n" +
                "                           <Content lang=\"nb\">Timetable</Content>\n" +
                "                        </GeneralSign>\n" +
                "                     </placeEquipments>\n" +
                "                    <TransportMode>bus</TransportMode>\n" +
                "                     <PublicCode>D</PublicCode>\n" +
                "                  </Quay>\n" +
                "               </quays>\n" +
                "            </StopPlace>\n" +
                "           </stopPlaces>" +
                "       </SiteFrame>" +
                "   </dataObjects>" +
                "</PublicationDelivery>";


        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        ImportParams importParams = createStandardParamsForImport();

        Response response = importResource.importPublicationDelivery(stream,importParams);
        assertThat(response.getStatus()).isEqualTo(200);

        StreamingOutput streamingOutput = (StreamingOutput) response.getEntity();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        streamingOutput.write(byteArrayOutputStream);
        System.out.println(byteArrayOutputStream.toString());
    }
}