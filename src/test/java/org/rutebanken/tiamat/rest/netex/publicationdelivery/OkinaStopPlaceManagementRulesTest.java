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


import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ImportType;
import org.rutebanken.tiamat.netex.NetexUtils;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OkinaStopPlaceManagementRulesTest extends TiamatIntegrationTest {

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
     * If a stop place or quay is already existing in database , it should NOT be updated.
     * (This is done to avoid data loss, in case user made modifications from abzu.
     *  User must use Abzu to change point name or type)
      */
    @Test
    public void checkThatStopAndQuayNamesAreNotUpdated() throws Exception {

        String initialStopName = "stop name1";
        String initialQuayName = "quay name1";

        String stopPlaceId1 = "XYZ:StopPlace:1";


        StopPlace stopPlace1 = new StopPlace()
                .withId(stopPlaceId1)
                .withVersion("1")
                .withName(new MultilingualString().withValue(initialStopName))
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("59.914353"))
                                .withLongitude(new BigDecimal("10.806387"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("XYZ:Quay:87654")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue(stopPlaceId1).withRef(stopPlaceId1))
                                .withName(new MultilingualString().withValue(initialQuayName).withLang("fr"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace1);
        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        List<StopPlace> changedStopPlaces = publicationDeliveryTestHelper.extractStopPlaces(response, false);
        assertEquals(1, changedStopPlaces.size());
        StopPlace resultSp = changedStopPlaces.get(0);
        assertEquals(initialStopName, resultSp.getName().getValue(),"created stop place should have initial name");
        List<Quay> quays = NetexUtils.getQuaysFromStopPlace(resultSp);
        assertEquals(1, quays.size());
        Quay createdQuay = quays.get(0);
        assertEquals(initialQuayName, createdQuay.getName().getValue(), "created quay should have initial name");


        ///2nd import : same stop place/quay with different names
        // Expected : name has NOT been updated

        StopPlace stopPlace2 = new StopPlace()
                .withId(stopPlaceId1)
                .withVersion("1")
                .withName(new MultilingualString().withValue("new name for SP"))
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("59.914353"))
                                .withLongitude(new BigDecimal("10.806387"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("XYZ:Quay:87654")
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue(stopPlaceId1).withRef(stopPlaceId1))
                                .withName(new MultilingualString().withValue("new name for quay").withLang("fr"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));

        PublicationDeliveryStructure publicationDelivery2 = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace2);
        PublicationDeliveryStructure response2 = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery2);

        List<StopPlace> changedStopPlaces2 = publicationDeliveryTestHelper.extractStopPlaces(response2, false);
        assertEquals(1, changedStopPlaces2.size());
        StopPlace resultSp2 = changedStopPlaces2.get(0);
        assertEquals(initialStopName, resultSp2.getName().getValue(),"created stop place should have initial name");
        List<Quay> quays2 = NetexUtils.getQuaysFromStopPlace(resultSp2);
        assertEquals(1, quays2.size());
        Quay createdQuay2 = quays2.get(0);
        assertEquals(initialQuayName, createdQuay2.getName().getValue(),"created quay should have initial name");



    }
}