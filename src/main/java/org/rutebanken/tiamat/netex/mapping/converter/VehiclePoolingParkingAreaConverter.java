package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.model.ParkingArea;
import org.springframework.stereotype.Component;

@Component
public class VehiclePoolingParkingAreaConverter extends BidirectionalConverter<ParkingArea, VehiclePoolingParkingArea> {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Override
    public VehiclePoolingParkingArea convertTo(ParkingArea parkingArea, Type<VehiclePoolingParkingArea> type, MappingContext mappingContext) {
        if (parkingArea == null) {
            return null;
        }
        VehiclePoolingParkingArea vehiclePoolingParkingArea = new VehiclePoolingParkingArea();
        vehiclePoolingParkingArea.setId(parkingArea.getNetexId());
        vehiclePoolingParkingArea.setName(new MultilingualString().withValue(parkingArea.getName().getValue()).withLang(parkingArea.getName().getLang()));
        vehiclePoolingParkingArea.setMaximumHeight(parkingArea.getMaximumHeight());
        vehiclePoolingParkingArea.setPublicUse(parkingArea.getPublicUse() != null ? PublicUseEnumeration.fromValue(parkingArea.getPublicUse().value()) : PublicUseEnumeration.ALL);
        vehiclePoolingParkingArea.setVersion(String.valueOf(parkingArea.getVersion()));
        vehiclePoolingParkingArea.withRest(netexObjectFactory.createParkingArea_VersionStructureTotalCapacity(parkingArea.getTotalCapacity()));
        vehiclePoolingParkingArea.setModification(parkingArea.getModification() != null ? ModificationEnumeration.fromValue(parkingArea.getModification().value()) : null);
        return vehiclePoolingParkingArea;
    }

    @Override
    public ParkingArea convertFrom(VehiclePoolingParkingArea vehiclePoolingParkingArea, Type<ParkingArea> type, MappingContext mappingContext) {
        return null;
    }
}
