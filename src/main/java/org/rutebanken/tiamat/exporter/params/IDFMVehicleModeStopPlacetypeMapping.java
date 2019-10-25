package org.rutebanken.tiamat.exporter.params;

import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.VehicleModeEnumeration;

import java.util.HashMap;
import java.util.Map;

public class IDFMVehicleModeStopPlacetypeMapping {

    private Map<StopTypeEnumeration, VehicleModeEnumeration> stopPlaceTypeVehicleMode = new HashMap<>();

    public IDFMVehicleModeStopPlacetypeMapping() {
        stopPlaceTypeVehicleMode.put(StopTypeEnumeration.ONSTREET_BUS, VehicleModeEnumeration.BUS);
        stopPlaceTypeVehicleMode.put(StopTypeEnumeration.ONSTREET_TRAM, VehicleModeEnumeration.BUS);
        stopPlaceTypeVehicleMode.put(StopTypeEnumeration.AIRPORT, VehicleModeEnumeration.AIR);
        stopPlaceTypeVehicleMode.put(StopTypeEnumeration.RAIL_STATION, VehicleModeEnumeration.RAIL);
        stopPlaceTypeVehicleMode.put(StopTypeEnumeration.METRO_STATION, VehicleModeEnumeration.TRAM);
        stopPlaceTypeVehicleMode.put(StopTypeEnumeration.BUS_STATION, VehicleModeEnumeration.BUS);
        stopPlaceTypeVehicleMode.put(StopTypeEnumeration.COACH_STATION, VehicleModeEnumeration.COACH);
    }

    public VehicleModeEnumeration getVehicleModeEnumeration(StopTypeEnumeration stopTypeEnumeration){
        return stopPlaceTypeVehicleMode.get(stopTypeEnumeration);
    }

}
