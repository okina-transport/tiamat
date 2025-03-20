package org.rutebanken.tiamat.model.gbfs;

import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.SpecificParkingAreaUsageEnumeration;

import java.util.List;

public record GbfsParkingImportData(List<StationInformation> stations, List<VehicleType> vehicleTypes,
                                    SystemInformation systemInformation,
                                    ParkingTypeEnumeration parkingType,
                                    SpecificParkingAreaUsageEnumeration parkingAreaType) {

}
