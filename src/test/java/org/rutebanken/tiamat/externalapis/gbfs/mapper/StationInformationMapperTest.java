package org.rutebanken.tiamat.externalapis.gbfs.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStationInformation;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSVehicleTypes;
import org.rutebanken.tiamat.model.*;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StationInformationMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String RAW_GBFS_V_TYPES = """
            {
               "last_updated":"2025-05-30T14:35:33.000+00:00",
               "ttl":0,
               "version":"3.0",
               "data":{
                 "vehicle_types":[
                   {
                     "vehicle_type_id":"CAL:VehicleType:titibike",
                     "form_factor":"bicycle",
                     "propulsion_type":"electric_assist",
                     "max_range_meters":20000
                   },
                   {
                     "vehicle_type_id":"CAL:VehicleType:x2",
                     "form_factor":"bicycle",
                     "propulsion_type":"electric_assist",
                     "max_range_meters":60000
                   },
                   {
                     "vehicle_type_id":"CAL:VehicleType:knot",
                     "form_factor":"scooter",
                     "propulsion_type":"electric",
                     "max_range_meters":35000
                   },
                   {
                     "vehicle_type_id":"CAL:VehicleType:pony",
                     "form_factor":"scooter",
                     "propulsion_type":"electric",
                     "max_range_meters":60000
                   }
                 ]
               }
             }
            """;

    private static final String RAW_GBFS_STATION_INFORMATION_JSON = """
            {
              "last_updated":"2025-05-30T14:29:18.000+00:00",
              "ttl":0,
              "version":"3.0",
              "data":{
                "stations":[
                  {
                    "station_id":"CAL:Station:stn_bE8SHEQRa5rnLSuzpqoxJs",
                    "name":[
                      {
                        "text":"Lac des Nauves",
                        "language":"fr"
                      }
                    ],
                    "short_name":[
                      {
                        "text":"LDN",
                        "language":"fr"
                      }
                    ],
                    "address":"36 quai des orfèvres",
                    "cross_street":"ABCD",
                    "lat":45.055792,
                    "lon":-0.099944,
                    "is_virtual_station":false,
                    "capacity":10,
                    "is_valet_station":false,
                    "is_charging_station":false,
                    "rental_uris":{
                      "web":"https://calivelo.ecovelo.mobi/#/station/stn_bE8SHEQRa5rnLSuzpqoxJs",
                      "ios":"ios",
                      "android":"android"
                    },
                    "rental_methods":[
                      "creditcard",
                      "applepay",
                      "androidpay"
                    ],
                    "vehicle_types_capacity":[
                      {
                        "vehicle_type_ids":[
                          "CAL:VehicleType:knot",
                          "CAL:VehicleType:pony"
                        ],
                        "count":10
                      }
                    ],
                    "parking_type": "underground_parking"
                  }
                ]
              }
            }
            """;

    private final StationInformationMapper tested = new StationInformationMapper("MOBIITI");

    @Test
    public void test_toParking_whenInputIsValid_shouldMapCorrectly() throws JsonProcessingException {
        // Arrange
        GBFSStationInformation gbfsStationInformation = MAPPER.readValue(RAW_GBFS_STATION_INFORMATION_JSON, GBFSStationInformation.class);
        GBFSVehicleTypes gbfsVehicleTypes = MAPPER.readValue(RAW_GBFS_V_TYPES, GBFSVehicleTypes.class);
        Organisation organisation = new Organisation();

        // Act
        Parking output = tested.toParking(organisation, gbfsStationInformation.getData().getStations().get(0), gbfsVehicleTypes, ParkingTypeEnumeration.CYCLE_RENTAL, SpecificParkingAreaUsageEnumeration.PEDAL_CYCLE);

        // Assert
        assertNotNull(output);
        assertEquals("MOBIITI:PARKING:CAL##3A##Station##3A##stn_bE8SHEQRa5rnLSuzpqoxJs", output.getNetexId());
        assertEquals("CAL:Station:stn_bE8SHEQRa5rnLSuzpqoxJs", output.getOriginalId());
        assertEquals("Lac des Nauves", output.getName().getValue());
        assertEquals("fr", output.getName().getLang());
        assertEquals("LDN", output.getShortName().getValue());
        assertEquals("fr", output.getShortName().getLang());
        assertNotNull(output.getCentroid());
        assertEquals("36 quai des orfèvres", output.getAddress());
        assertEquals("ABCD", output.getCrossRoad().getValue());
        assertEquals(BigInteger.valueOf(10), output.getTotalCapacity());
        assertFalse(output.isRechargingAvailable());
        assertEquals("https://calivelo.ecovelo.mobi/#/station/stn_bE8SHEQRa5rnLSuzpqoxJs", output.getBookingUrl());
        assertEquals("ios", output.getRentalUriIos());
        assertEquals("android", output.getRentalUriAndroid());
        assertEquals(List.of(PaymentMethodEnumeration.CREDIT_CARD, PaymentMethodEnumeration.MOBILE_PHONE),
                output.getParkingPaymentMethods().stream().sorted().toList());
        assertEquals(List.of(ParkingPaymentProcessEnumeration.PAY_BY_MOBILE_DEVICE,
                ParkingPaymentProcessEnumeration.PAY_AND_DISPLAY,
                ParkingPaymentProcessEnumeration.PAY_BY_PREPAID_TOKEN), output.getParkingPaymentProcess());
        assertEquals(List.of(ParkingVehicleEnumeration.MOTOR_SCOOTER), output.getParkingVehicleTypes());
        assertEquals(ParkingLayoutEnumeration.UNDERGROUND, output.getParkingLayout());
        assertEquals(organisation, output.getOrganisation());
        assertEquals(ParkingTypeEnumeration.CYCLE_RENTAL, output.getParkingType());
        assertEquals(1, output.getParkingProperties().size());
        ParkingProperties parkingProperties = output.getParkingProperties().get(0);
        assertEquals(2, parkingProperties.getSpaces().size());
        ParkingCapacity parkingCapacity = parkingProperties.getSpaces().get(0);
        assertEquals(BigInteger.valueOf(10), parkingCapacity.getNumberOfSpaces());
        assertEquals(ParkingVehicleEnumeration.MOTOR_SCOOTER, parkingCapacity.getParkingVehicleType());
        ParkingCapacity parkingCapacity2 = parkingProperties.getSpaces().get(0);
        assertEquals(BigInteger.valueOf(10), parkingCapacity2.getNumberOfSpaces());
        assertEquals(ParkingVehicleEnumeration.MOTOR_SCOOTER, parkingCapacity2.getParkingVehicleType());
    }

    @Test
    void test_toParking_whenParkingAreaTypeIsCarshare_shouldSetCarVehicleType() throws JsonProcessingException {
        // Arrange
        GBFSStationInformation gbfsStationInformation = MAPPER.readValue(RAW_GBFS_STATION_INFORMATION_JSON, GBFSStationInformation.class);
        GBFSVehicleTypes gbfsVehicleTypes = MAPPER.readValue(RAW_GBFS_V_TYPES, GBFSVehicleTypes.class);
        Organisation organisation = new Organisation();

        // Act
        Parking output = tested.toParking(organisation, gbfsStationInformation.getData().getStations().getFirst(), gbfsVehicleTypes, ParkingTypeEnumeration.RENTAL_CAR_PARKING, SpecificParkingAreaUsageEnumeration.CARSHARE);

        // Assert
        assertEquals(List.of(ParkingVehicleEnumeration.CAR), output.getParkingVehicleTypes());
    }

    @Test
    void test_toParking_whenParkingAreaTypeIsCarpool_shouldSetCarVehicleType() throws JsonProcessingException {
        // Arrange
        GBFSStationInformation gbfsStationInformation = MAPPER.readValue(RAW_GBFS_STATION_INFORMATION_JSON, GBFSStationInformation.class);
        GBFSVehicleTypes gbfsVehicleTypes = MAPPER.readValue(RAW_GBFS_V_TYPES, GBFSVehicleTypes.class);
        Organisation organisation = new Organisation();

        // Act
        Parking output = tested.toParking(organisation, gbfsStationInformation.getData().getStations().getFirst(), gbfsVehicleTypes, ParkingTypeEnumeration.RENTAL_CAR_PARKING, SpecificParkingAreaUsageEnumeration.CARPOOL);

        // Assert
        assertEquals(List.of(ParkingVehicleEnumeration.CAR), output.getParkingVehicleTypes());
    }

    @Test
    void test_toParking_whenParkingAreaTypeIsNotCarshareOrCarpool_shouldNotSetCarVehicleType() throws JsonProcessingException {
        // Arrange
        GBFSStationInformation gbfsStationInformation = MAPPER.readValue(RAW_GBFS_STATION_INFORMATION_JSON, GBFSStationInformation.class);
        GBFSVehicleTypes gbfsVehicleTypes = MAPPER.readValue(RAW_GBFS_V_TYPES, GBFSVehicleTypes.class);
        Organisation organisation = new Organisation();

        // Act
        Parking output = tested.toParking(organisation, gbfsStationInformation.getData().getStations().getFirst(), gbfsVehicleTypes, ParkingTypeEnumeration.CYCLE_RENTAL, SpecificParkingAreaUsageEnumeration.PEDAL_CYCLE);

        // Assert
        assertFalse(output.getParkingVehicleTypes().contains(ParkingVehicleEnumeration.CAR));
    }

}