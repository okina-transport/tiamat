package org.rutebanken.tiamat.service.parking;

import org.rutebanken.tiamat.importer.ImporterUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reverse geocoding of a parking position to its INSEE code (remote address APIs). A bean so that it can be mocked in tests.
 */
@Component
public class ParkingInseeCodeResolver {

    public Optional<String> resolve(double longitude, double latitude) {
        return ImporterUtils.getInseeFromLatLng(longitude, latitude);
    }
}
