package org.rutebanken.tiamat.externalapis.gbfs.mapper;

import org.junit.Before;
import org.junit.Test;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.model.gbfs.RentalUri;
import org.rutebanken.tiamat.model.gbfs.StationInformation;
import org.rutebanken.tiamat.model.gbfs.VehicleType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.rutebanken.tiamat.externalapis.gbfs.mapper.StationInformationMapper.*;

public class StationInformationMapperTest {

    private static final String STATION_ID = "station_id";
    private static final String STATION_NAME = "station_name";
    private static final String ADDRESS = "3, Address";
    private static final String CROSS_STREET = "Cross Street";
    private static final String BOOKING_URL = "https://booking.com";
    private static final String RENTAL_URI_IOS = "https://ios.booking.com";
    private static final String RENTAL_URI_ANDROID = "https://android.booking.com";

    private StationInformationMapper mapper;

    @Before
    public void setUp() {
        mapper = new StationInformationMapper();
    }

    @Test
    public void toParkingPaymentMethodEnumeration_noRentalMethod_test() {
        Set<PaymentMethodEnumeration> parkingPaymentMethodEnumeration = mapper.toParkingPaymentMethodEnumeration(new StationInformation());

        assertThat(parkingPaymentMethodEnumeration).isEmpty();
    }

    @Test
    public void toParkingPaymentMethodEnumeration_multipleRentalMethods_test() {
        StationInformation stationInformation = new StationInformation();
        stationInformation.setRentalMethods(List.of("creditcard",
                "paypass",
                "applepay", "androidpay", "phone",
                "transitcard"
        ));
        Set<PaymentMethodEnumeration> parkingPaymentMethodEnumeration = mapper.toParkingPaymentMethodEnumeration(stationInformation);

        assertThat(parkingPaymentMethodEnumeration).hasSize(4).containsExactlyInAnyOrder(
                PaymentMethodEnumeration.CREDIT_CARD,
                PaymentMethodEnumeration.CONTACTLESS_PAYMENT_CARD,
                PaymentMethodEnumeration.MOBILE_PHONE,
                PaymentMethodEnumeration.TRAVEL_CARD
        );
    }

    @Test
    public void toParkingLayoutEnumeration_emptyInformation_test() {
        ParkingLayoutEnumeration parkingLayoutEnumeration = mapper.toParkingLayoutEnumeration(new StationInformation());

        assertThat(parkingLayoutEnumeration).isNull();
    }

    @Test
    public void toParkingLayoutEnumeration_parkingTypeDefined_test() {
        StationInformation stationInformation = new StationInformation();
        stationInformation.setParkingType("parking_lot");
        ParkingLayoutEnumeration parkingLayoutEnumeration = mapper.toParkingLayoutEnumeration(stationInformation);

        assertThat(parkingLayoutEnumeration).isEqualTo(ParkingLayoutEnumeration.OPEN_SPACE);
    }

    @Test
    public void toParkingLayoutEnumeration_parkingTypeDefined2_test() {
        StationInformation stationInformation = new StationInformation();
        stationInformation.setParkingType("street_parking");
        ParkingLayoutEnumeration parkingLayoutEnumeration = mapper.toParkingLayoutEnumeration(stationInformation);

        assertThat(parkingLayoutEnumeration).isEqualTo(ParkingLayoutEnumeration.ROADSIDE);
    }

    @Test
    public void toParkingLayoutEnumeration_parkingTypeDefined3_test() {
        StationInformation stationInformation = new StationInformation();
        stationInformation.setParkingType("underground_parking");
        ParkingLayoutEnumeration parkingLayoutEnumeration = mapper.toParkingLayoutEnumeration(stationInformation);

        assertThat(parkingLayoutEnumeration).isEqualTo(ParkingLayoutEnumeration.UNDERGROUND);
    }

    @Test
    public void mapVehicleCapacity_test() {
        VehicleType vehicleType = new VehicleType();
        vehicleType.setVehicleTypeId("bike");
        vehicleType.setFormFactor(ParkingVehicleEnumeration.PEDAL_CYCLE.value());
        Parking parking = new Parking();
        StationInformation stationInformation = new StationInformation();
        stationInformation.setVehicleCapacity(Map.of("bike", 10));

        mapVehicleCapacity(stationInformation, List.of(vehicleType), parking);

        assertThat(parking.getParkingProperties()).hasSize(1);
        List<ParkingCapacity> spaces = parking.getParkingProperties().get(0).getSpaces();
        assertThat(spaces).hasSize(1);
        assertThat(spaces.get(0).getNumberOfSpaces()).isEqualTo(10);
        assertThat(spaces.get(0).getParkingVehicleType()).isEqualTo(ParkingVehicleEnumeration.PEDAL_CYCLE);
    }

    @Test
    public void toParking_stationIdMapping_test() {
       StationInformation stationInformation = new StationInformation();
       stationInformation.setStationId(STATION_ID);

        Parking parking = mapper.toParking(null, stationInformation, ParkingTypeEnumeration.CYCLE_RENTAL, List.of());

        assertThat(parking.getNetexId()).isEqualTo("MOBIITI:PARKING:station_id");
    }

    @Test
    public void toParking_nameMapping_test() {
        StationInformation stationInformation = new StationInformation();
        stationInformation.setName(STATION_NAME);

        Parking parking = mapper.toParking(null, stationInformation, ParkingTypeEnumeration.CYCLE_RENTAL, List.of());

        assertThat(parking.getName().getValue()).isEqualTo(stationInformation.getName());
    }

    @Test
    public void toParking_shortNameMapping_test() {
        StationInformation stationInformation = new StationInformation();
        stationInformation.setShortName(STATION_NAME);

        Parking parking = mapper.toParking(null, stationInformation, ParkingTypeEnumeration.CYCLE_RENTAL, List.of());

        assertThat(parking.getShortName().getValue()).isEqualTo(stationInformation.getShortName());
    }

    @Test
    public void toParking_addressMapping_test() {
        StationInformation stationInformation = new StationInformation();
        stationInformation.setAddress(ADDRESS);

        Parking parking = mapper.toParking(null, stationInformation, ParkingTypeEnumeration.CYCLE_RENTAL, List.of());

        assertThat(parking.getAddress()).isEqualTo(stationInformation.getAddress());
    }

    @Test
    public void toParking_crossStreetMapping_test() {
        StationInformation stationInformation = new StationInformation();
        stationInformation.setCrossStreet(CROSS_STREET);

        Parking parking = mapper.toParking(null, stationInformation, ParkingTypeEnumeration.CYCLE_RENTAL, List.of());

        assertThat(parking.getCrossRoad().getValue()).isEqualTo(stationInformation.getCrossStreet());
    }

    @Test
    public void toParking_capacityMapping_test() {
        StationInformation stationInformation = new StationInformation();

        Parking parking = mapper.toParking(null, stationInformation, ParkingTypeEnumeration.CYCLE_RENTAL, List.of());

        assertThat(parking.getTotalCapacity()).isZero();
    }

    @Test
    public void toParking_rechargingAvailableMapping_test() {
        StationInformation stationInformation = new StationInformation();
        stationInformation.setChargingStation(Boolean.TRUE);

        Parking parking = mapper.toParking(null, stationInformation, ParkingTypeEnumeration.CYCLE_RENTAL, List.of());

        assertThat(parking.isRechargingAvailable()).isEqualTo(stationInformation.getChargingStation());
    }

    @Test
    public void toParking_bookingUrlMapping_test() {
        StationInformation stationInformation = new StationInformation();
        RentalUri rentalUri = new RentalUri();
        rentalUri.setBookingUri(BOOKING_URL);
        stationInformation.setRentalUri(rentalUri);

        Parking parking = mapper.toParking(null, stationInformation, ParkingTypeEnumeration.CYCLE_RENTAL, List.of());

        assertThat(parking.getBookingUrl()).isEqualTo(stationInformation.getRentalUri().getBookingUri());
    }

    @Test
    public void toParking_iosUrlMapping_test() {
        StationInformation stationInformation = new StationInformation();
        RentalUri rentalUri = new RentalUri();
        rentalUri.setRentalUriIos(RENTAL_URI_IOS);
        stationInformation.setRentalUri(rentalUri);

        Parking parking = mapper.toParking(null, stationInformation, ParkingTypeEnumeration.CYCLE_RENTAL, List.of());

        assertThat(parking.getRentalUriIos()).isEqualTo(stationInformation.getRentalUri().getRentalUriIos());
    }

    @Test
    public void toParking_androidUrlMapping_test() {
        StationInformation stationInformation = new StationInformation();
        RentalUri rentalUri = new RentalUri();
        rentalUri.setRentalUriAndroid(RENTAL_URI_ANDROID);
        stationInformation.setRentalUri(rentalUri);

        Parking parking = mapper.toParking(null, stationInformation, ParkingTypeEnumeration.CYCLE_RENTAL, List.of());

        assertThat(parking.getRentalUriAndroid()).isEqualTo(stationInformation.getRentalUri().getRentalUriAndroid());
    }

    @Test
    public void toParking_organisationMapping_test() {
        StationInformation stationInformation = new StationInformation();
       Organisation organisation = new Organisation();

        Parking parking = mapper.toParking(organisation, stationInformation, ParkingTypeEnumeration.CYCLE_RENTAL, List.of());

        assertThat(parking.getOrganisation()).isEqualTo(organisation);
    }

    @Test
    public void toParking_parkingTypeMapping_test() {
        StationInformation stationInformation = new StationInformation();
        Organisation organisation = new Organisation();

        Parking parking = mapper.toParking(organisation, stationInformation, ParkingTypeEnumeration.PARKING_ZONE, List.of());

        assertThat(parking.getParkingType()).isEqualTo(ParkingTypeEnumeration.PARKING_ZONE);
    }

}