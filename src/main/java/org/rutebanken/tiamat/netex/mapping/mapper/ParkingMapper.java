/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.netex.mapping.mapper;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.model.SpecificParkingAreaUsageEnumeration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ParkingMapper extends CustomMapper<Parking, org.rutebanken.tiamat.model.Parking> {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Override
    public void mapAtoB(Parking parking, org.rutebanken.tiamat.model.Parking parking2, MappingContext context) {
        super.mapAtoB(parking, parking2, context);
        if (parking.getParkingAreas() != null &&
                parking.getParkingAreas().getParkingAreaRefOrParkingArea_() != null &&
                !parking.getParkingAreas().getParkingAreaRefOrParkingArea_().isEmpty()) {
            List<org.rutebanken.tiamat.model.ParkingArea> parkingAreas = mapperFacade.mapAsList(parking.getParkingAreas().getParkingAreaRefOrParkingArea_(), org.rutebanken.tiamat.model.ParkingArea.class, context);
            if (!parkingAreas.isEmpty()) {
                parking2.setParkingAreas(parkingAreas);
            }
        }
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.Parking tiamatParking, Parking netexParking, MappingContext context) {
        super.mapBtoA(tiamatParking, netexParking, context);

        if (tiamatParking.getInsee() != null) {
            PostalAddress postalAddress = new PostalAddress();
            postalAddress.setId("MOBIITI:PostalAddress:" + UUID.randomUUID().toString());
            postalAddress.setVersion("any");
            postalAddress.setPostalRegion(tiamatParking.getInsee());
            netexParking.setPostalAddress(postalAddress);
        }

        if (tiamatParking.getParkingAreas() != null &&
                !tiamatParking.getParkingAreas().isEmpty()) {

            List<ParkingArea_VersionStructure> parkingAreas = new ArrayList<>();
            for (org.rutebanken.tiamat.model.ParkingArea pa : tiamatParking.getParkingAreas()) {
                if (pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.CARPOOL)) {
                    parkingAreas.add(mapperFacade.map(pa, VehiclePoolingParkingArea.class));
                } else if (pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.CARSHARE)) {
                    parkingAreas.add(mapperFacade.map(pa, VehicleSharingParkingArea.class));
                } else {
                    parkingAreas.add(mapperFacade.map(pa, ParkingArea.class));
                }
            }
            
            if (!parkingAreas.isEmpty()) {
                parkingAreas.forEach(pa -> {
                    if (pa.getSiteRef() == null) {
                        SiteRefStructure siteRefStructure = new SiteRefStructure();
                        siteRefStructure.setRef(tiamatParking.getNetexId());
                        pa.setSiteRef(siteRefStructure);
                    }
                });
                ParkingAreas_RelStructure parkingAreas_relStructure = new ParkingAreas_RelStructure();
                parkingAreas_relStructure.getParkingAreaRefOrParkingArea_().addAll(parkingAreas.stream()
                        .map(pa -> {
                            if (pa instanceof VehiclePoolingParkingArea) {
                                return netexObjectFactory.createVehiclePoolingParkingArea((VehiclePoolingParkingArea) pa);
                            } else if (pa instanceof VehicleSharingParkingArea) {
                                return netexObjectFactory.createVehicleSharingParkingArea((VehicleSharingParkingArea) pa);
                            } else {
                                return netexObjectFactory.createParkingArea((ParkingArea) pa);
                            }
                        })
                        .collect(Collectors.toList()));

                netexParking.setParkingAreas(parkingAreas_relStructure);
            }
        }
    }
}
