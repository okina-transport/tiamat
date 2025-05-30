package org.rutebanken.tiamat.model.gbfs;

import org.mobilitydata.gbfs.v3_0.station_information.GBFSStationInformation;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSSystemInformation;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSVehicleTypes;

public record GbfsParkingImportData(GBFSStationInformation stationInformation, GBFSSystemInformation systemInformation,
                                    GBFSVehicleTypes vehicleTypes) {

}
