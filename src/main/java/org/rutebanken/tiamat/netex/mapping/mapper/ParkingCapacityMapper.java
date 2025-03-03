package org.rutebanken.tiamat.netex.mapping.mapper;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.ParkingCapacity;
import org.rutebanken.netex.model.TransportTypeRefStructure;

public class ParkingCapacityMapper extends CustomMapper<ParkingCapacity, org.rutebanken.tiamat.model.ParkingCapacity> {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Override
    public void mapAtoB(ParkingCapacity netex, org.rutebanken.tiamat.model.ParkingCapacity tiamat, MappingContext context) {
        super.mapAtoB(netex, tiamat, context);

        if (netex.getTransportTypeRef() != null && netex.getTransportTypeRef().getValue() != null) {
            tiamat.setTransportTypeRef(netex.getTransportTypeRef().getValue().getRef());
        }

    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.ParkingCapacity tiamat, ParkingCapacity netex, MappingContext context) {
        super.mapBtoA(tiamat, netex, context);

        if (StringUtils.isNotBlank(tiamat.getTransportTypeRef())) {
            TransportTypeRefStructure ttrs = new TransportTypeRefStructure();
            netex.withTransportTypeRef(netexObjectFactory.createTransportTypeRef(ttrs));
        }

    }
}
