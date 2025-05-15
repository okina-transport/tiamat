package org.rutebanken.tiamat.netex.mapping.mapper;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.rutebanken.tiamat.model.ParkingVehicleEnumeration;
import org.rutebanken.tiamat.model.ParkingBay;

public class ParkingBayMapper extends CustomMapper<org.rutebanken.netex.model.ParkingBay, ParkingBay> {

    @Override
    public void mapBtoA(ParkingBay tiamatParkingBay, org.rutebanken.netex.model.ParkingBay netexParkingBay, MappingContext context) {
        if (tiamatParkingBay.getParkingVehicleType() != null) {
            netexParkingBay.setParkingVehicleType(
                    org.rutebanken.netex.model.ParkingVehicleEnumeration.valueOf(
                            tiamatParkingBay.getParkingVehicleType().toString()
                    )
            );
        }
    }

    @Override
    public void mapAtoB(org.rutebanken.netex.model.ParkingBay netexParkingBay, ParkingBay tiamatParkingBay, MappingContext context) {
        if (netexParkingBay.getParkingVehicleType() != null) {
            tiamatParkingBay.setParkingVehicleType(
                    ParkingVehicleEnumeration.valueOf(netexParkingBay.getParkingVehicleType().toString())
            );
        }
    }
}
