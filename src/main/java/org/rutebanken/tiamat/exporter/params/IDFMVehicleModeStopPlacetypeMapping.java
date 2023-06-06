package org.rutebanken.tiamat.exporter.params;

import org.rutebanken.netex.model.VehicleModeEnumeration;

import java.util.HashMap;
import java.util.Map;

public class IDFMVehicleModeStopPlacetypeMapping {

    private Map<org.rutebanken.netex.model.StopTypeEnumeration, VehicleModeEnumeration> stopPlaceTypeVehicleMode = new HashMap<>();

    public IDFMVehicleModeStopPlacetypeMapping() {
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.ONSTREET_BUS, VehicleModeEnumeration.BUS);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.ONSTREET_TRAM, VehicleModeEnumeration.TRAM);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.AIRPORT, VehicleModeEnumeration.AIR);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.RAIL_STATION, VehicleModeEnumeration.RAIL);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.METRO_STATION, VehicleModeEnumeration.METRO);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.BUS_STATION, VehicleModeEnumeration.BUS);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.COACH_STATION, VehicleModeEnumeration.COACH);
    }

    public VehicleModeEnumeration getVehicleModeEnumeration(org.rutebanken.netex.model.StopTypeEnumeration stopTypeEnumeration){
        return stopPlaceTypeVehicleMode.get(stopTypeEnumeration);
    }

}
