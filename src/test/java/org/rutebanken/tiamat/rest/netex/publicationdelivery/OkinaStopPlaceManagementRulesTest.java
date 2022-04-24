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
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PrivateCodeStructure;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.rutebanken.netex.model.SiteRefStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopTypeEnumeration;
import org.rutebanken.netex.model.ValidBetween;
import org.rutebanken.netex.model.VehicleModeEnumeration;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ImportType;
import org.rutebanken.tiamat.netex.NetexUtils;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OkinaStopPlaceManagementRulesTest extends TiamatIntegrationTest {

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
                .withTransportMode(VehicleModeEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("59.914353"))
                                .withLongitude(new BigDecimal("10.806387"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(new Quay()
                                .withVersion("1")
                                .withId("XYZ:Quay:87654")
                                .withTransportMode(VehicleModeEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue(stopPlaceId1).withRef(stopPlaceId1))
                                .withName(new MultilingualString().withValue(initialQuayName).withLang("fr"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949"))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace1);
        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        List<StopPlace> changedStopPlaces = publicationDeliveryTestHelper.extractStopPlaces(response, false);
        Assert.assertEquals(1, changedStopPlaces.size());
        StopPlace resultSp = changedStopPlaces.get(0);
        Assert.assertEquals("created stop place should have initial name",initialStopName, resultSp.getName().getValue());
        List<Quay> quays = NetexUtils.getQuaysFromStopPlace(resultSp);
        Assert.assertEquals(1, quays.size());
        Quay createdQuay = quays.get(0);
        Assert.assertEquals("created quay should have initial name",initialQuayName, createdQuay.getName().getValue());


        ///2nd import : same stop place/quay with different names
        // Expected : name has NOT been updated

        StopPlace stopPlace2 = new StopPlace()
                .withId(stopPlaceId1)
                .withVersion("1")
                .withName(new MultilingualString().withValue("new name for SP"))
                .withTransportMode(VehicleModeEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withCentroid(new SimplePoint_VersionStructure()
                        .withLocation(new LocationStructure()
                                .withLatitude(new BigDecimal("59.914353"))
                                .withLongitude(new BigDecimal("10.806387"))))
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(new Quay()
                                .withVersion("1")
                                .withId("XYZ:Quay:87654")
                                .withTransportMode(VehicleModeEnumeration.BUS)
                                .withSiteRef(new SiteRefStructure().withValue(stopPlaceId1).withRef(stopPlaceId1))
                                .withName(new MultilingualString().withValue("new name for quay").withLang("fr"))
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949"))))));

        PublicationDeliveryStructure publicationDelivery2 = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace2);
        PublicationDeliveryStructure response2 = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery2);

        List<StopPlace> changedStopPlaces2 = publicationDeliveryTestHelper.extractStopPlaces(response2, false);
        Assert.assertEquals(1, changedStopPlaces2.size());
        StopPlace resultSp2 = changedStopPlaces2.get(0);
        Assert.assertEquals("created stop place should have initial name",initialStopName, resultSp2.getName().getValue());
        List<Quay> quays2 = NetexUtils.getQuaysFromStopPlace(resultSp2);
        Assert.assertEquals(1, quays2.size());
        Quay createdQuay2 = quays2.get(0);
        Assert.assertEquals("created quay should have initial name",initialQuayName, createdQuay2.getName().getValue());



    }
}