package org.rutebanken.tiamat.netex;

import jakarta.xml.bind.JAXBElement;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.ParkingBays_RelStructure;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NetexUtilsTest {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Test
    public void getParkingAreaTotalCapacityReturnsNullWhenRestIsNull() {
        assertThat(NetexUtils.getParkingAreaTotalCapacity(null)).isNull();
    }

    @Test
    public void getParkingAreaTotalCapacityReturnsNullWhenRestIsEmpty() {
        assertThat(NetexUtils.getParkingAreaTotalCapacity(new ArrayList<>())).isNull();
    }

    @Test
    public void getParkingAreaTotalCapacityReturnsNullWhenTotalCapacityIsAbsent() {
        List<JAXBElement<?>> rest = new ArrayList<>();
        rest.add(netexObjectFactory.createParkingArea_VersionStructureBays(new ParkingBays_RelStructure()));

        assertThat(NetexUtils.getParkingAreaTotalCapacity(rest)).isNull();
    }

    @Test
    public void getParkingAreaTotalCapacityReturnsValue() {
        List<JAXBElement<?>> rest = new ArrayList<>();
        rest.add(netexObjectFactory.createParkingArea_VersionStructureTotalCapacity(BigInteger.valueOf(42)));

        assertThat(NetexUtils.getParkingAreaTotalCapacity(rest)).isEqualTo(BigInteger.valueOf(42));
    }

    @Test
    public void getParkingAreaTotalCapacityReturnsValueWhenNotFirstElement() {
        List<JAXBElement<?>> rest = new ArrayList<>();
        rest.add(netexObjectFactory.createParkingArea_VersionStructureNumberOfBaysWithRecharging(BigInteger.valueOf(3)));
        rest.add(netexObjectFactory.createParkingArea_VersionStructureTotalCapacity(BigInteger.valueOf(42)));

        assertThat(NetexUtils.getParkingAreaTotalCapacity(rest)).isEqualTo(BigInteger.valueOf(42));
    }
}
