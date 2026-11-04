package org.rutebanken.tiamat.netex.mapping.mapper;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.SiteRefStructure;
import org.rutebanken.netex.validation.NeTExValidator;
import org.rutebanken.tiamat.model.ParkingArea;
import org.rutebanken.tiamat.netex.NetexUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class ParkingAreaMapperTest {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    private final ParkingAreaMapper parkingAreaMapper = new ParkingAreaMapper();

    @Test
    public void mapBtoAOmitsTotalCapacityWhenNull() {
        org.rutebanken.netex.model.ParkingArea netexParkingArea = mapToNetex(createTiamatParkingArea(null));

        assertThat(netexParkingArea.getRest())
                .noneMatch(element -> "TotalCapacity".equals(element.getName().getLocalPart()));
    }

    @Test
    public void mapBtoAKeepsTotalCapacity() {
        org.rutebanken.netex.model.ParkingArea netexParkingArea = mapToNetex(createTiamatParkingArea(BigInteger.valueOf(42)));

        assertThat(NetexUtils.getParkingAreaTotalCapacity(netexParkingArea.getRest())).isEqualTo(BigInteger.valueOf(42));
    }

    /**
     * TotalCapacity is not nillable in the NeTEx XSD: a null capacity used to produce xsi:nil
     * and make the schema validating marshaller of the parking export fail
     */
    @Test
    public void parkingAreaWithoutTotalCapacityPassesSchemaValidation() throws Exception {
        org.rutebanken.netex.model.ParkingArea netexParkingArea = mapToNetex(createTiamatParkingArea(null));
        // SiteRef is required by the French profile XSD and is set by the parent Parking mapping
        netexParkingArea.setSiteRef(new SiteRefStructure().withRef("MOBIITI:Parking:1"));

        Marshaller marshaller = JAXBContext.newInstance(PublicationDeliveryStructure.class).createMarshaller();
        marshaller.setSchema(NeTExValidator.getNeTExValidator().getSchema());

        assertThatCode(() -> marshaller.marshal(netexObjectFactory.createParkingArea(netexParkingArea), new ByteArrayOutputStream()))
                .doesNotThrowAnyException();
    }

    private org.rutebanken.netex.model.ParkingArea mapToNetex(ParkingArea tiamatParkingArea) {
        org.rutebanken.netex.model.ParkingArea netexParkingArea = new org.rutebanken.netex.model.ParkingArea();
        parkingAreaMapper.mapBtoA(tiamatParkingArea, netexParkingArea, null);
        return netexParkingArea;
    }

    private ParkingArea createTiamatParkingArea(BigInteger totalCapacity) {
        ParkingArea tiamatParkingArea = new ParkingArea();
        tiamatParkingArea.setNetexId("MOBIITI:ParkingArea:1");
        tiamatParkingArea.setTotalCapacity(totalCapacity);
        return tiamatParkingArea;
    }
}
