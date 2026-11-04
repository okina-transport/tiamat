package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.TypeBuilder;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.VehiclePoolingParkingArea;
import org.rutebanken.tiamat.model.ParkingArea;
import org.rutebanken.tiamat.netex.NetexUtils;

import java.math.BigInteger;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class VehiclePoolingParkingAreaConverterTest {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    private final VehiclePoolingParkingAreaConverter converter = new VehiclePoolingParkingAreaConverter();

    private final MappingContext mappingContext = new MappingContext(new HashMap<>());

    @Test
    public void convertToOmitsTotalCapacityWhenNull() {
        VehiclePoolingParkingArea netexParkingArea = converter.convertTo(createTiamatParkingArea(null), new TypeBuilder<VehiclePoolingParkingArea>() {}.build(), mappingContext);

        assertThat(netexParkingArea.getRest())
                .noneMatch(element -> "TotalCapacity".equals(element.getName().getLocalPart()));
    }

    @Test
    public void convertToKeepsTotalCapacity() {
        VehiclePoolingParkingArea netexParkingArea = converter.convertTo(createTiamatParkingArea(BigInteger.valueOf(42)), new TypeBuilder<VehiclePoolingParkingArea>() {}.build(), mappingContext);

        assertThat(NetexUtils.getParkingAreaTotalCapacity(netexParkingArea.getRest())).isEqualTo(BigInteger.valueOf(42));
    }

    @Test
    public void convertFromWithoutTotalCapacity() {
        ParkingArea tiamatParkingArea = converter.convertFrom(createNetexParkingArea(), new TypeBuilder<ParkingArea>() {}.build(), mappingContext);

        assertThat(tiamatParkingArea.getTotalCapacity()).isNull();
    }

    @Test
    public void convertFromWithTotalCapacity() {
        VehiclePoolingParkingArea netexParkingArea = createNetexParkingArea();
        netexParkingArea.withRest(netexObjectFactory.createParkingArea_VersionStructureTotalCapacity(BigInteger.valueOf(42)));

        ParkingArea tiamatParkingArea = converter.convertFrom(netexParkingArea, new TypeBuilder<ParkingArea>() {}.build(), mappingContext);

        assertThat(tiamatParkingArea.getTotalCapacity()).isEqualTo(BigInteger.valueOf(42));
    }

    private ParkingArea createTiamatParkingArea(BigInteger totalCapacity) {
        ParkingArea tiamatParkingArea = new ParkingArea();
        tiamatParkingArea.setNetexId("MOBIITI:ParkingArea:1");
        tiamatParkingArea.setVersion(1L);
        tiamatParkingArea.setTotalCapacity(totalCapacity);
        return tiamatParkingArea;
    }

    private VehiclePoolingParkingArea createNetexParkingArea() {
        VehiclePoolingParkingArea netexParkingArea = new VehiclePoolingParkingArea();
        netexParkingArea.setId("MOBIITI:ParkingArea:1");
        netexParkingArea.setVersion("1");
        return netexParkingArea;
    }
}
