package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
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
        if (parkingArea.getName() != null ){
            vehiclePoolingParkingArea.setName(new MultilingualString().withValue(parkingArea.getName().getValue()).withLang(parkingArea.getName().getLang()));
        }
        vehiclePoolingParkingArea.setMaximumHeight(parkingArea.getMaximumHeight());
        vehiclePoolingParkingArea.setPublicUse(parkingArea.getPublicUse() != null ? PublicUseEnumeration.fromValue(parkingArea.getPublicUse().value()) : PublicUseEnumeration.ALL);
        vehiclePoolingParkingArea.setVersion(String.valueOf(parkingArea.getVersion()));
        vehiclePoolingParkingArea.withRest(netexObjectFactory.createParkingArea_VersionStructureTotalCapacity(parkingArea.getTotalCapacity()));
        vehiclePoolingParkingArea.setModification(parkingArea.getModification() != null ? ModificationEnumeration.fromValue(parkingArea.getModification().value()) : null);
        return vehiclePoolingParkingArea;
    }

    @Override
    public ParkingArea convertFrom(VehiclePoolingParkingArea vehiclePoolingParkingArea, Type<ParkingArea> type, MappingContext mappingContext) {
        if (vehiclePoolingParkingArea == null) {
            return null;
        }
        ParkingArea parkingArea = new ParkingArea();
        parkingArea.setNetexId(vehiclePoolingParkingArea.getId());
        if (vehiclePoolingParkingArea.getName() != null ){
            parkingArea.setName(new EmbeddableMultilingualString(vehiclePoolingParkingArea.getName().getValue()));
        }
        parkingArea.setMaximumHeight(vehiclePoolingParkingArea.getMaximumHeight());
        parkingArea.setPublicUse(parkingArea.getPublicUse() != null ? org.rutebanken.tiamat.model.PublicUseEnumeration.fromValue(vehiclePoolingParkingArea.getPublicUse().value()) : org.rutebanken.tiamat.model.PublicUseEnumeration.ALL);
        parkingArea.setVersion(Long.parseLong(vehiclePoolingParkingArea.getVersion()));
        parkingArea.setTotalCapacity(netexObjectFactory.createParkingArea_VersionStructureTotalCapacity(parkingArea.getTotalCapacity()).getValue());
        parkingArea.setModification(vehiclePoolingParkingArea.getModification() != null ? org.rutebanken.tiamat.model.ModificationEnumeration.fromValue(vehiclePoolingParkingArea.getModification().value()) : null);
        return parkingArea;
    }
}
