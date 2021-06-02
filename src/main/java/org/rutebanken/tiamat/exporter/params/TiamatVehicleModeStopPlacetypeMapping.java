package org.rutebanken.tiamat.exporter.params;



import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.VehicleModeEnumeration;

import java.util.HashMap;
import java.util.Map;

public class TiamatVehicleModeStopPlacetypeMapping {

    private static Map<StopTypeEnumeration, VehicleModeEnumeration> stopPlaceTypeVehicleMode = new HashMap<>();


    public static VehicleModeEnumeration getVehicleModeEnumeration(StopTypeEnumeration stopTypeEnumeration){
        if (stopPlaceTypeVehicleMode.size() == 0){
            stopPlaceTypeVehicleMode.put(StopTypeEnumeration.ONSTREET_BUS, VehicleModeEnumeration.BUS);
            stopPlaceTypeVehicleMode.put(StopTypeEnumeration.ONSTREET_TRAM, VehicleModeEnumeration.TRAM);
            stopPlaceTypeVehicleMode.put(StopTypeEnumeration.AIRPORT, VehicleModeEnumeration.AIR);
            stopPlaceTypeVehicleMode.put(StopTypeEnumeration.RAIL_STATION, VehicleModeEnumeration.RAIL);
            stopPlaceTypeVehicleMode.put(StopTypeEnumeration.METRO_STATION, VehicleModeEnumeration.TRAM);
            stopPlaceTypeVehicleMode.put(StopTypeEnumeration.BUS_STATION, VehicleModeEnumeration.BUS);
            stopPlaceTypeVehicleMode.put(StopTypeEnumeration.COACH_STATION, VehicleModeEnumeration.COACH);
            stopPlaceTypeVehicleMode.put(StopTypeEnumeration.FERRY_STOP, VehicleModeEnumeration.FERRY);
            stopPlaceTypeVehicleMode.put(StopTypeEnumeration.FERRY_PORT, VehicleModeEnumeration.FERRY);
        }
        return stopPlaceTypeVehicleMode.get(stopTypeEnumeration);
    }


}
