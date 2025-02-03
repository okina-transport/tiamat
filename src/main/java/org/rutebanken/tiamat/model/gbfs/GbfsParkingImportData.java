package org.rutebanken.tiamat.model.gbfs;

import org.rutebanken.tiamat.model.ParkingTypeEnumeration;

import java.util.List;

public record GbfsParkingImportData(List<StationInformation> stations, List<VehicleType> vehicleTypes,
                                    SystemInformation systemInformation,
                                    ParkingTypeEnumeration parkingTypeEnumeration) {

}
