package org.rutebanken.tiamat.service.parking.gbfs;

import org.junit.Test;
import org.rutebanken.tiamat.externalapis.gbfs.GbfsClient;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class GbfsFeedValidatorFactoryTest {

    private final GbfsClient gbfsClient = new GbfsClient();

    @Test
    public void testCreateVehicleTypeFeedValidator() {
        GbfsFeedValidator validator = GbfsFeedValidatorFactory.init(gbfsClient, "vehicle_types").build();

        assertThat(validator).isNotNull();
        assertThat(validator).isInstanceOf(VehicleTypeFeedValidator.class);
    }

    @Test
    public void testCreateStationInformationFeedValidator() {
        GbfsFeedValidator validator = GbfsFeedValidatorFactory.init(gbfsClient, "station_information").build();

        assertThat(validator).isNotNull();
        assertThat(validator).isInstanceOf(StationInformationFeedValidator.class);
    }

    @Test
    public void testCreateSystemInformationFeedValidator() {
        GbfsFeedValidator validator = GbfsFeedValidatorFactory.init(gbfsClient, "system_information").build();

        assertThat(validator).isNotNull();
        assertThat(validator).isInstanceOf(SystemInformationFeedValidator.class);
    }

    @Test
    public void testCreateDefaultFeedValidator() {
        GbfsFeedValidator validator = GbfsFeedValidatorFactory.init(gbfsClient, "").build();

        assertThat(validator).isNotNull();
        assertThat(validator).isInstanceOf(DefaultFeedValidator.class);
    }

    @Test
    public void testCreateDefaultFeedValidatorWhenInvalidTypeIsGiven() {
        GbfsFeedValidator validator = GbfsFeedValidatorFactory.init(gbfsClient, "invalid").build();

        assertThat(validator).isNotNull();
        assertThat(validator).isInstanceOf(DefaultFeedValidator.class);
    }
}