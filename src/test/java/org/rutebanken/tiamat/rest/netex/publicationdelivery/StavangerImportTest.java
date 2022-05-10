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

import org.junit.Test;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Import tests for different cases in Stavanger.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class StavangerImportTest  extends TiamatIntegrationTest {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Autowired
    private PublicationDeliveryTestHelper publicationDeliveryTestHelper;


    /**
     * Import stop place with hpl. and number in the name.
     * The number should be moved to the Quay name.
     * Stop place does not have centroid. This causes tests to not interfer with each other.
     */
    @Test
    public void importStavangerWithHplNumbering() throws Exception {

        StopPlace stopPlace = new StopPlace()
                .withId("KOL:StopArea:987654")
                .withVersion("1")
                .withName(new MultilingualString().withValue("Stavanger hpl. 12").withLang("no"))
                .withStopPlaceType(StopTypeEnumeration.ONSTREET_BUS)
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                        .withVersion("1")
                                        .withId("KOL:StopArea:87654")
                                        .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                        .withName(new MultilingualString().withValue("Stavanger hpl. 12").withLang("no"))
                                        .withSiteRef(new SiteRefStructure().withValue("KOL:StopArea:987654").withRef("KOL:StopArea:987654"))
                                        .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                                .withLatitude(new BigDecimal("58.966910"))
                                                .withLongitude(new BigDecimal("5.732949")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);

        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(response);

        assertThat(actualStopPlace.getName().getValue()).isEqualTo("Stavanger");

        List<Quay> actualQuays = publicationDeliveryTestHelper.extractQuays(actualStopPlace);
        assertThat(actualQuays).isNotNull().as("quays should not be null");
        assertThat(actualQuays.get(0).getPublicCode()).describedAs("Quay name should not be null").isNotNull();
        assertThat(actualQuays.get(0).getPublicCode()).isEqualTo("12");
    }

    /**
     * Import stop place with spor and number in the name.
     * The number should be moved to the Quay name.
     */
    @Test
    public void importStopWithSporAndNumbering() throws Exception {

        final String originalStopPlaceName = "Stavanger spor 2";

        StopPlace stopPlace = new StopPlace()
                .withId("KOL:StopArea:11032650")
                .withVersion("1")
                .withName(new MultilingualString().withValue(originalStopPlaceName).withLang("no"))
                .withStopPlaceType(StopTypeEnumeration.ONSTREET_BUS)
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay()
                                .withVersion("1")
                                .withId("KOL:StopArea:1103265001")
                                .withName(new MultilingualString().withValue(originalStopPlaceName).withLang("no"))
                                .withSiteRef(new SiteRefStructure().withValue("KOL:StopArea:11032650").withRef("KOL:StopArea:11032650"))
                                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                .withCentroid(new SimplePoint_VersionStructure().withLocation(new LocationStructure()
                                        .withLatitude(new BigDecimal("58.966910"))
                                        .withLongitude(new BigDecimal("5.732949")))))));

        PublicationDeliveryStructure publicationDelivery = publicationDeliveryTestHelper.createPublicationDeliveryWithStopPlace(stopPlace);

        PublicationDeliveryStructure response = publicationDeliveryTestHelper.postAndReturnPublicationDelivery(publicationDelivery);

        StopPlace actualStopPlace = publicationDeliveryTestHelper.findFirstStopPlace(response);

        assertThat(actualStopPlace.getName().getValue()).describedAs("Stop place name").isEqualTo("Stavanger");

        List<Quay> actualQuays = publicationDeliveryTestHelper.extractQuays(actualStopPlace);
        assertThat(actualQuays).isNotNull().as("quays should not be null");
        assertThat(actualQuays.get(0).getPublicCode()).describedAs("Quay name should not be null").isNotNull();
        assertThat(actualQuays.get(0).getPublicCode()).isEqualTo("2");
    }
}
