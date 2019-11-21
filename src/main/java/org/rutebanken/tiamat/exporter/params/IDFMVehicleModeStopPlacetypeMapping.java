package org.rutebanken.tiamat.exporter.params;

import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.VehicleModeEnumeration;

import java.util.HashMap;
import java.util.Map;

public class IDFMVehicleModeStopPlacetypeMapping {

    private Map<org.rutebanken.netex.model.StopTypeEnumeration, org.rutebanken.netex.model.VehicleModeEnumeration> stopPlaceTypeVehicleMode = new HashMap<>();

    public IDFMVehicleModeStopPlacetypeMapping() {
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.ONSTREET_BUS, org.rutebanken.netex.model.VehicleModeEnumeration.BUS);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.ONSTREET_TRAM, org.rutebanken.netex.model.VehicleModeEnumeration.BUS);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.AIRPORT, org.rutebanken.netex.model.VehicleModeEnumeration.AIR);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.RAIL_STATION, org.rutebanken.netex.model.VehicleModeEnumeration.RAIL);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.METRO_STATION, org.rutebanken.netex.model.VehicleModeEnumeration.TRAM);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.BUS_STATION, org.rutebanken.netex.model.VehicleModeEnumeration.BUS);
        stopPlaceTypeVehicleMode.put(org.rutebanken.netex.model.StopTypeEnumeration.COACH_STATION, org.rutebanken.netex.model.VehicleModeEnumeration.COACH);
    }

    public org.rutebanken.netex.model.VehicleModeEnumeration getVehicleModeEnumeration(org.rutebanken.netex.model.StopTypeEnumeration stopTypeEnumeration){
        return stopPlaceTypeVehicleMode.get(stopTypeEnumeration);
    }

}
