package org.rutebanken.tiamat.service.parking.gbfs;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.externalapis.gbfs.GbfsClient;

public class GbfsFeedValidatorFactory {

    private final GbfsFeedValidator instance;

    private GbfsFeedValidatorFactory(GbfsClient client, String type) {
        if (StringUtils.isBlank(type)) {
            instance = new DefaultFeedValidator();
        } else {
            switch (type) {
                case "vehicle_types":
                    instance = new VehicleTypeFeedValidator();
                    break;
                case "station_information":
                    instance = new StationInformationFeedValidator();
                    break;
                case "system_information":
                    instance = new SystemInformationFeedValidator();
                    break;
                default:
                    instance = new DefaultFeedValidator();
                    break;
            }
        }
        instance.gbfsClient = client;
    }

    public static GbfsFeedValidatorFactory init(GbfsClient client, String type) {
        return new GbfsFeedValidatorFactory(client, type);
    }

    public GbfsFeedValidator build() {
        return instance;
    }
}
