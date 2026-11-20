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

package org.rutebanken.tiamat.rest.graphql;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import org.locationtech.jts.geom.Coordinate;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingIdEntity;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.SiteRefStructure;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.AvailabilityCondition;
import org.rutebanken.tiamat.model.DayOfWeekEnumeration;
import org.rutebanken.tiamat.model.DayType;
import org.rutebanken.tiamat.model.Value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

class GraphQLResourceParkingIntegrationTest extends AbstractGraphQLResourceIntegrationTest {

    @Test
    void searchForParkingById() {

        Parking parking = new Parking();
        parking.setCentroid(geometryFactory.createPoint(new Coordinate(10.533212, 59.678080)));
        parking.setParkingType(ParkingTypeEnumeration.PARK_AND_RIDE);
        parking.setParentSiteRef(new SiteRefStructure(stopPlaceRepository.save(new StopPlace()).getNetexId()));

        parkingVersionedSaverService.saveNewVersion(parking);

        String graphQlJsonQuery = "{" +
                "\"query\":\"{" +
                "  parking: " + GraphQLNames.FIND_PARKING + " (id:\\\"" + parking.getNetexId() + "\\\") { " +
                "    id " +
                "    parkingType " +
                "  } " +
                "}\"," +
                "\"variables\":\"\"}";

        executeGraphQL(graphQlJsonQuery)
                .body("data.parking[0].id", equalTo(parking.getNetexId()))
                .body("data.parking[0].parkingType", equalTo(parking.getParkingType().value()));

    }

    @Test
    void searchForParkingByIdAndVersion() {

        Parking parking = new Parking();
        parking.setCentroid(geometryFactory.createPoint(new Coordinate(10.533212, 59.678080)));
        ParkingTypeEnumeration originalParkingType = ParkingTypeEnumeration.PARK_AND_RIDE;
        parking.setParkingType(originalParkingType);
        parking.setParentSiteRef(new SiteRefStructure(stopPlaceRepository.save(new StopPlace()).getNetexId()));
        parking = parkingVersionedSaverService.saveNewVersion(parking);

        String netexId = parking.getNetexId();

        String version1GraphQlJsonQuery = "{" +
                "\"query\":\"{" +
                "  parking: " + GraphQLNames.FIND_PARKING + " (id:\\\"" + netexId + "\\\", version:1 ) { " +
                "    id " +
                "    version " +
                "    parkingType " +
                "  } " +
                "}\"," +
                "\"variables\":\"\"}";

        executeGraphQL(version1GraphQlJsonQuery)
                .body("data.parking[0].id", equalTo(netexId))
                .body("data.parking[0].version", equalTo(""+parking.getVersion()))
                .body("data.parking[0].parkingType", equalTo(parking.getParkingType().value()));


        String updatedParkingTypeValue = ParkingTypeEnumeration.PARKING_ZONE.value();
        String version2GraphQlJsonQuery = "{" +
                "\"query\":\"mutation { " +
                "  parking:" + GraphQLNames.MUTATE_PARKING + " (Parking: {" +
                "        id:\\\"" + netexId + "\\\" " +
                "        parkingType: " + updatedParkingTypeValue +
                "       }) { " +
                "      id " +
                "      version " +
                "      parkingType " +
                "    } " +
                "}\"," +
                "\"variables\":\"\"}";

        executeGraphQL(version2GraphQlJsonQuery)
                .body("data.parking[0].id", equalTo(netexId))
                .body("data.parking[0].version", equalTo("2"))
                .body("data.parking[0].parkingType", equalTo(updatedParkingTypeValue));

    }

    @Test
    void testMutateParkingWithParentSiteRef() {


        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("Brummunddal"));
        stopPlace.setStopPlaceType(StopTypeEnumeration.ONSTREET_BUS);
        stopPlace.setCentroid(geometryFactory.createPoint(new Coordinate(10, 59)));
        stopPlace.setAllAreasWheelchairAccessible(false);

        stopPlace = stopPlaceVersionedSaverService.saveNewVersion(stopPlace);

        String graphQlQuery = "{\n" +
                "\"query\": \"mutation { " +
                "  parking: " + GraphQLNames.MUTATE_PARKING + " (Parking : {" +
                "     name: {" +
                "      value: \\\"Parking name\\\" " +
                "      lang: \\\"no\\\" " +
                "    }" +
                "    geometry: { " +
                "      type:Point " +
                "      coordinates:[[59.0, 10.5]] " +
                "    }" +
                "    totalCapacity:1234, " +
                "    parkingType:parkAndRide, " +
                "    parkingVehicleTypes: [car, pedalCycle]" +
                "    parentSiteRef:\\\"%s\\\"".formatted(stopPlace.getNetexId()) +
                "    parkingLayout:covered " +
                "    principalCapacity:22 " +
                "    overnightParkingPermitted:true, " +
                "    rechargingAvailable:false, " +
                "    secure:false, " +
                "    realTimeOccupancyAvailable:false, " +
                "    parkingReservation:reservationAllowed, " +
                "    bookingUrl:\\\"https://www.rutebanken.org\\\", " +
                "    freeParkingOutOfHours:true, " +
                "    parkingProperties: {, " +
                "      parkingUserTypes:all," +
                "      spaces: [{" +
                "        parkingVehicleType:car," +
                "        parkingStayType:unlimited," +
                "        numberOfSpaces:123" +
                "      }]" +
                "    }" +
                "    parkingAreas: [{" +
                "      label: {value:\\\"Plan 1\\\"}" +
                "      totalCapacity:432" +
                "      parkingProperties: {" +
                "          parkingUserTypes:all, " +
                "          spaces : [{" +
                "              parkingVehicleType:car" +
                "              parkingStayType:shortStay" +
                "              numberOfSpaces:123" +
                "            }]" +
                "      }" +
                "      }]" +
                "}) {" +
                "    id, " +
                "    version, " +
                "    name {value lang}, " +
                "    parentSiteRef, " +
                "    parkingType," +
                "    parkingVehicleTypes," +
                "    parkingLayout," +
                "    principalCapacity," +
                "    totalCapacity," +
                "    overnightParkingPermitted," +
                "    rechargingAvailable," +
                "    secure," +
                "    realTimeOccupancyAvailable," +
                "    parkingReservation," +
                "    bookingUrl," +
                "    freeParkingOutOfHours," +
                "    parkingProperties {" +
                "      parkingUserTypes," +
                "      maximumStay," +
                "      spaces {" +
                "        parkingVehicleType" +
                ",       parkingStayType," +
                "        numberOfSpaces" +
                "      }" +
                "    }" +
                "    parkingAreas {" +
                "      label {value}" +
                "      totalCapacity" +
                "      parkingProperties {" +
                "        parkingUserTypes," +
//                "        maximumStay," +
                "        spaces {," +
                "          parkingVehicleType," +
                "          parkingStayType," +
                "          numberOfSpaces" +
                "        }" +
                "      }" +
                "    }" +
                "    geometry{" +
                "      type," +
                "      coordinates" +
                "    }" +
                "  }" +
                "}\",\"variables\": \"\"}";
        executeGraphQL(graphQlQuery)
                .body("data.parking", notNullValue())
                .rootPath("data.parking[0]")
                    .body("id", notNullValue())
                    .body("version", notNullValue())
                    .body("name.value", notNullValue())
                    .body("name.lang", notNullValue())
                    .body("geometry.type", notNullValue())
                    .body("geometry.coordinates", notNullValue())
                    .body("parentSiteRef", notNullValue())
                    .body("parkingType", notNullValue())
                    .body("parkingVehicleTypes", notNullValue())
                    .body("parkingVehicleTypes", notNullValue())
                    .body("parkingLayout", notNullValue())
                    .body("principalCapacity", notNullValue())
                    .body("totalCapacity", notNullValue())
                    .body("overnightParkingPermitted", notNullValue())
                    .body("rechargingAvailable", notNullValue())
                    .body("secure", notNullValue())
                    .body("realTimeOccupancyAvailable", notNullValue())
                    .body("parkingReservation", notNullValue())
                    .body("bookingUrl", notNullValue())
                    .body("parkingProperties.parkingUserTypes", notNullValue())
                    .body("parkingProperties.spaces", notNullValue())
                    .body("parkingAreas", notNullValue())
                    .body("parkingAreas.label.value", notNullValue())
                    .body("parkingAreas.totalCapacity", notNullValue())
                    .body("parkingAreas.parkingProperties", notNullValue());

    }


    @Test
    void testMutateParkingWithoutParentSiteRef() {

        StopPlace stopPlace = stopPlaceRepository.save(new StopPlace());

        String graphQlQuery = "{\n" +
                "\"query\": \"mutation { " +
                "  parking: " + GraphQLNames.MUTATE_PARKING + " (Parking : {" +

                "     name: {" +
                "      value: \\\"Parking name\\\" " +
                "      lang: \\\"no\\\" " +
                "    }" +
                "    parentSiteRef:\\\"%s\\\"".formatted(stopPlace.getNetexId()) +
                "    geometry: { " +
                "      type:Point " +
                "      coordinates:[[59.0, 10.5]] " +
                "    }" +
                "    totalCapacity:1234, " +
                "    parkingType:parkAndRide, " +
                "    parkingVehicleTypes: [car, pedalCycle]" +
                "    parkingLayout:covered " +
                "    principalCapacity:22 " +
                "    overnightParkingPermitted:true, " +
                "    rechargingAvailable:false, " +
                "    secure:false, " +
                "    realTimeOccupancyAvailable:false, " +
                "    parkingReservation:reservationAllowed, " +
                "    bookingUrl:\\\"https://www.rutebanken.org\\\", " +
                "    freeParkingOutOfHours:true, " +
                "    parkingProperties: {, " +
                "      parkingUserTypes:all," +
                "      spaces: [{" +
                "        parkingVehicleType:car," +
                "        parkingStayType:unlimited," +
                "        numberOfSpaces:123" +
                "      }]" +
                "    }" +
                "    parkingAreas: [{" +
                "      label: {value:\\\"Plan 1\\\"}" +
                "      totalCapacity:432" +
                "      parkingProperties: {" +
                "          parkingUserTypes:all, " +
                "          spaces : [{" +
                "              parkingVehicleType:car" +
                "              parkingStayType:shortStay" +
                "              numberOfSpaces:123" +
                "            }]" +
                "      }" +
                "      }]" +
                "}) {" +
                "    id, " +
                "    version, " +
                "    name {value lang}, " +
                "    parentSiteRef, " +
                "    parkingType," +
                "    parkingVehicleTypes," +
                "    parkingLayout," +
                "    principalCapacity," +
                "    totalCapacity," +
                "    overnightParkingPermitted," +
                "    rechargingAvailable," +
                "    secure," +
                "    realTimeOccupancyAvailable," +
                "    parkingReservation," +
                "    bookingUrl," +
                "    freeParkingOutOfHours," +
                "    parkingProperties {" +
                "      parkingUserTypes," +
                "      maximumStay," +
                "      spaces {" +
                "        parkingVehicleType" +
                ",       parkingStayType," +
                "        numberOfSpaces" +
                "      }" +
                "    }" +
                "    parkingAreas {" +
                "      label {value}" +
                "      totalCapacity" +
                "      parkingProperties {" +
                "        parkingUserTypes," +
//                "        maximumStay," +
                "        spaces {," +
                "          parkingVehicleType," +
                "          parkingStayType," +
                "          numberOfSpaces" +
                "        }" +
                "      }" +
                "    }" +
                "    geometry{" +
                "      type," +
                "      coordinates" +
                "    }" +
                "  }" +
                "}\",\"variables\": \"\"}";
        executeGraphQL(graphQlQuery)
                .body("data.parking", notNullValue())
                .rootPath("data.parking[0]")
                    .body("id", notNullValue())
                    .body("version", notNullValue())
                    .body("name.value", notNullValue())
                    .body("name.lang", notNullValue())
                    .body("geometry.type", notNullValue())
                    .body("geometry.coordinates", notNullValue())
                    .body("parentSiteRef", equalTo(stopPlace.getNetexId()))
                    .body("parkingType", notNullValue())
                    .body("parkingVehicleTypes", notNullValue())
                    .body("parkingVehicleTypes", notNullValue())
                    .body("parkingLayout", notNullValue())
                    .body("principalCapacity", notNullValue())
                    .body("totalCapacity", notNullValue())
                    .body("overnightParkingPermitted", notNullValue())
                    .body("rechargingAvailable", notNullValue())
                    .body("secure", notNullValue())
                    .body("realTimeOccupancyAvailable", notNullValue())
                    .body("parkingReservation", notNullValue())
                    .body("bookingUrl", notNullValue())
                    .body("parkingProperties.parkingUserTypes", notNullValue())
                    .body("parkingProperties.spaces", notNullValue())
                    .body("parkingAreas", notNullValue())
                    .body("parkingAreas.label.value", notNullValue())
                    .body("parkingAreas.totalCapacity", notNullValue())
                    .body("parkingAreas.parkingProperties", notNullValue());

    }


    @Test
    void testMutateMultipleParkings() {

        StopPlace stopPlace = stopPlaceRepository.save(new StopPlace());

        String graphQlQuery = "{\n" +
                "\"query\": \"mutation { " +
                "  parking: " + GraphQLNames.MUTATE_PARKING + " (Parking : [{" +
                "     name: {" +
                "      value: \\\"Parking name\\\" " +
                "      lang: \\\"no\\\" " +
                "    }" +
                "    parentSiteRef:\\\"%s\\\"".formatted(stopPlace.getNetexId()) +
                "    geometry: { " +
                "      type:Point " +
                "      coordinates:[[59.0, 10.5]] " +
                "    }" +
                "  }, {" +
                "     name: {" +
                "      value: \\\"Parking name\\\" " +
                "      lang: \\\"no\\\" " +
                "    }" +
                "    parentSiteRef:\\\"%s\\\"".formatted(stopPlace.getNetexId()) +
                "    geometry: { " +
                "      type:Point " +
                "      coordinates:[[59.0, 10.5]] " +
                "    }" +
                "  }] ) {" +
                "    id, " +
                "    name {value lang}, " +

                "  }" +
                "}\",\"variables\": \"\"}";

        executeGraphQL(graphQlQuery)
                .body("data.parking", notNullValue())
                .body("data.parking", hasSize(2));


    }

    private static final String DIWIO_PARKING_INPUT = """
            name: {value: "Abri 11 novembre", lang: "fr"}
            geometry: {type: Point, coordinates: [[10.5, 59.0]]}
            keyValues: [{key: "imported-id", values: ["DIWIO:Parking:abri1"]}]
            operator: "Diwio"
            address: "3 rue du bourg"
            postalCode: "35200"
            city: "St Jean"
            availabilityConditions: [{isAvailable: true, dayTypes: [{daysOfWeek: [Everyday], timeBands: [{startTime: "00:00", endTime: "23:59"}]}]}]
            """;

    @Test
    void testMutateParkingWithOperatorAddressAndAvailabilityConditions() {
        String netexId = executeGraphqQLQueryOnly("mutation { parking: " + GraphQLNames.MUTATE_PARKING + " (Parking: [{" + DIWIO_PARKING_INPUT + "}]) { id version } }")
                .body("data.parking[0].version", equalTo("1"))
                .extract().path("data.parking[0].id");

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            Parking parking = entityManager.createQuery("select p from Parking p where p.netexId = :netexId", Parking.class)
                    .setParameter("netexId", netexId)
                    .getSingleResult();

            assertThat(parking.getOperator()).isEqualTo("Diwio");
            assertThat(parking.getAddress()).isEqualTo("3 rue du bourg");
            assertThat(parking.getPostalAddress().getPostCode()).isEqualTo("35200");
            assertThat(parking.getPostalAddress().getTown()).isEqualTo("St Jean");
            assertThat(parking.getKeyValues().get("imported-id").getItems()).containsExactly("DIWIO:Parking:abri1");
            assertThat(parking.getAvailabilityConditions()).hasSize(1);
            AvailabilityCondition availabilityCondition = parking.getAvailabilityConditions().iterator().next();
            assertThat(availabilityCondition.isAvailable()).isTrue();
            assertThat(availabilityCondition.getDayTypes()).hasSize(1);
            DayType dayType = availabilityCondition.getDayTypes().iterator().next();
            assertThat(dayType.getDaysOfWeek()).containsExactly(DayOfWeekEnumeration.EVERYDAY);
            assertThat(dayType.getTimeBand()).hasSize(1);
        } finally {
            entityManager.close();
        }
    }

    @Test
    void testMutateParkingWithSameValuesDoesNotCreateNewVersion() {
        String netexId = executeGraphqQLQueryOnly("mutation { parking: " + GraphQLNames.MUTATE_PARKING + " (Parking: [{" + DIWIO_PARKING_INPUT + "}]) { id } }")
                .extract().path("data.parking[0].id");

        executeGraphqQLQueryOnly("mutation { parking: " + GraphQLNames.MUTATE_PARKING + " (Parking: [{id: \"" + netexId + "\" " + DIWIO_PARKING_INPUT + "}]) { id version } }")
                .body("data.parking[0].id", equalTo(netexId))
                .body("data.parking[0].version", equalTo("1"));

        executeGraphqQLQueryOnly("mutation { parking: " + GraphQLNames.MUTATE_PARKING + " (Parking: [{id: \"" + netexId + "\" " + DIWIO_PARKING_INPUT.replace("Abri 11 novembre", "Abri renomme") + "}]) { id version } }")
                .body("data.parking[0].id", equalTo(netexId))
                .body("data.parking[0].version", equalTo("2"));
    }

    @Test
    void testMutateParkingsAndDeleteParkingInOneRequest() {
        Parking toDelete = new Parking();
        toDelete.setName(new EmbeddableMultilingualString("Old shelter"));
        toDelete.setCentroid(geometryFactory.createPoint(new Coordinate(10.533212, 59.678080)));
        toDelete.getKeyValues().put("imported-id", new Value("DIWIO:Parking:old"));
        toDelete = parkingVersionedSaverService.saveNewVersion(toDelete);

        executeGraphqQLQueryOnly("mutation { parking: " + GraphQLNames.MUTATE_PARKING + " (Parking: [{" + DIWIO_PARKING_INPUT + "}]) { id } "
                + "d1: " + GraphQLNames.DELETE_PARKING + " (" + GraphQLNames.PARKING_ID + ": \"" + toDelete.getNetexId() + "\") }")
                .body("data.parking", hasSize(1))
                .body("data.d1", equalTo(true));

        assertThat(parkingRepository.findFirstByNetexIdOrderByVersionDesc(toDelete.getNetexId())).isNull();
    }

    @Test
    void testDiwioSyncMutationWithVariables() {
        Parking toDelete = new Parking();
        toDelete.setName(new EmbeddableMultilingualString("Old shelter"));
        toDelete.setCentroid(geometryFactory.createPoint(new Coordinate(10.533212, 59.678080)));
        toDelete = parkingVersionedSaverService.saveNewVersion(toDelete);

        // Same request shape as the one sent by Damona DiwioSyncService
        String request = """
                {
                  "query": "mutation DiwioParkingSync($parkings: [ParkingInput], $d0: String!) { upserts: mutateParking(Parking: $parkings) { id version } d0: deleteParking(parkingId: $d0) }",
                  "variables": {
                    "parkings": [{
                      "name": {"value": "Abri 11 novembre", "lang": "fr"},
                      "geometry": {"type": "Point", "coordinates": [[10.5, 59.0]]},
                      "keyValues": [{"key": "imported-id", "values": ["DIWIO:Parking:abri1"]}],
                      "operator": "Diwio",
                      "totalCapacity": 1,
                      "parkingType": "urbanParking",
                      "parkingLayout": "undefined",
                      "parkingVehicleTypes": ["pedalCycle"],
                      "parkingPaymentProcess": ["undefined"],
                      "availabilityConditions": [{"isAvailable": true, "dayTypes": [{"daysOfWeek": ["Everyday"]}]}],
                      "address": "3 rue du bourg",
                      "postalCode": "35200",
                      "city": "St Jean"
                    }],
                    "d0": "%s"
                  }
                }
                """.formatted(toDelete.getNetexId());

        executeGraphQL(request)
                .body("errors", org.hamcrest.Matchers.nullValue())
                .body("data.upserts", hasSize(1))
                .body("data.upserts[0].version", equalTo("1"))
                .body("data.d0", equalTo(true));

        assertThat(parkingRepository.findFirstByNetexIdOrderByVersionDesc(toDelete.getNetexId())).isNull();
        assertThat(parkingRepository.findParkingIdsByOperator("Diwio"))
                .extracting(ParkingIdEntity::getOriginalId)
                .containsExactly("DIWIO:Parking:abri1");
    }

    @Test
    void testNewParkingGetsInseeCodeAndInseeBasedNetexId() {
        when(parkingInseeCodeResolver.resolve(10.5, 59.0)).thenReturn(Optional.of("35281"));

        String netexId = executeGraphqQLQueryOnly("mutation { parking: " + GraphQLNames.MUTATE_PARKING + " (Parking: [{" + DIWIO_PARKING_INPUT + "}]) { id } }")
                .extract().path("data.parking[0].id");

        assertThat(netexId).matches("FR:35281:Parking:\\d+:LOC");
        assertThat(parkingRepository.findFirstByNetexIdOrderByVersionDesc(netexId).getInsee()).isEqualTo("35281");
    }

    @Test
    void testInseeCodeIsNotLookedUpWhenParkingDidNotMove() {
        String netexId = executeGraphqQLQueryOnly("mutation { parking: " + GraphQLNames.MUTATE_PARKING + " (Parking: [{" + DIWIO_PARKING_INPUT + "}]) { id } }")
                .extract().path("data.parking[0].id");
        clearInvocations(parkingInseeCodeResolver);

        executeGraphqQLQueryOnly("mutation { parking: " + GraphQLNames.MUTATE_PARKING + " (Parking: [{id: \"" + netexId + "\" " + DIWIO_PARKING_INPUT + "}]) { id } }");

        verifyNoInteractions(parkingInseeCodeResolver);
    }
}
