package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.model.ParkingArea;
import org.springframework.stereotype.Component;

@Component
public class VehicleSharingParkingAreaConverter extends BidirectionalConverter<ParkingArea, VehicleSharingParkingArea> {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Override
    public VehicleSharingParkingArea convertTo(ParkingArea parkingArea, Type<VehicleSharingParkingArea> type, MappingContext mappingContext) {
        if (parkingArea == null) {
            return null;
        }
        VehicleSharingParkingArea vehicleSharingParkingArea = new VehicleSharingParkingArea();
        vehicleSharingParkingArea.setId(parkingArea.getNetexId());
        vehicleSharingParkingArea.setName(new MultilingualString().withValue(parkingArea.getName().getValue()).withLang(parkingArea.getName().getLang()));
        vehicleSharingParkingArea.setMaximumHeight(parkingArea.getMaximumHeight());
        vehicleSharingParkingArea.setPublicUse(parkingArea.getPublicUse() != null ? PublicUseEnumeration.fromValue(parkingArea.getPublicUse().value()) : PublicUseEnumeration.ALL);
        vehicleSharingParkingArea.setVersion(String.valueOf(parkingArea.getVersion()));
        vehicleSharingParkingArea.withRest(netexObjectFactory.createParkingArea_VersionStructureTotalCapacity(parkingArea.getTotalCapacity()));
        vehicleSharingParkingArea.setModification(parkingArea.getModification() != null ? ModificationEnumeration.fromValue(parkingArea.getModification().value()) : null);
        return vehicleSharingParkingArea;
    }

    @Override
    public ParkingArea convertFrom(VehicleSharingParkingArea vehicleSharingParkingArea, Type<ParkingArea> type, MappingContext mappingContext) {
        return null;
    }
}
