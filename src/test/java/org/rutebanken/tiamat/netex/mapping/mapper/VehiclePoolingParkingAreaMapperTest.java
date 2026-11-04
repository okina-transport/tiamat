package org.rutebanken.tiamat.netex.mapping.mapper;

import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.VehiclePoolingParkingArea;
import org.rutebanken.tiamat.model.ParkingArea;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class VehiclePoolingParkingAreaMapperTest {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    private final VehiclePoolingParkingAreaMapper mapper = new VehiclePoolingParkingAreaMapper();

    @Test
    public void mapAtoBWithoutTotalCapacity() {
        ParkingArea tiamatParkingArea = new ParkingArea();

        mapper.mapAtoB(new VehiclePoolingParkingArea(), tiamatParkingArea, null);

        assertThat(tiamatParkingArea.getTotalCapacity()).isNull();
    }

    @Test
    public void mapAtoBWithTotalCapacity() {
        VehiclePoolingParkingArea netexParkingArea = new VehiclePoolingParkingArea();
        netexParkingArea.withRest(netexObjectFactory.createParkingArea_VersionStructureTotalCapacity(BigInteger.valueOf(42)));
        ParkingArea tiamatParkingArea = new ParkingArea();

        mapper.mapAtoB(netexParkingArea, tiamatParkingArea, null);

        assertThat(tiamatParkingArea.getTotalCapacity()).isEqualTo(BigInteger.valueOf(42));
    }
}
