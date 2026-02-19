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
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.model.ParkingArea;
import org.rutebanken.tiamat.model.SpecificParkingAreaUsageEnumeration;

import jakarta.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ParkingAreaMapper extends CustomMapper<org.rutebanken.netex.model.ParkingArea, ParkingArea> {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Override
    public void mapAtoB(org.rutebanken.netex.model.ParkingArea netexParkingArea, ParkingArea tiamatParkingArea, MappingContext context) {
        super.mapAtoB(netexParkingArea, tiamatParkingArea, context);

        for (JAXBElement rest : netexParkingArea.getRest()) {
            if (rest.getName().getLocalPart().equals("TotalCapacity")) {
                tiamatParkingArea.setTotalCapacity((BigInteger) rest.getValue());
            }

            if (rest.getName().getLocalPart().equals("NumberOfBaysWithRecharging")) {
                tiamatParkingArea.setNumberOfBaysWithRecharging((BigInteger) rest.getValue());
            }

            if (rest.getName().getLocalPart().equals("bays")) {
                if (rest.getValue() instanceof ParkingBays_RelStructure parkingBaysRelStructure) {
                    List<org.rutebanken.tiamat.model.ParkingBay> tiamatBays = new ArrayList<>();

                    for (Object object : parkingBaysRelStructure.getParkingBayRefOrParkingBay_()) {
                        if (object instanceof JAXBElement jaxbElement) {
                            if (jaxbElement.getValue() instanceof ParkingBay parkingBay) {
                                org.rutebanken.tiamat.model.ParkingBay tiamatParkingBay = mapperFacade.map(parkingBay, org.rutebanken.tiamat.model.ParkingBay.class);
                                tiamatParkingBay.setParkingArea(tiamatParkingArea);
                                tiamatBays.add(tiamatParkingBay);
                            }
                        }
                    }
                    tiamatParkingArea.setBays(tiamatBays);
                }
            }

        }
    }

    @Override
    public void mapBtoA(ParkingArea tiamatParkingArea, org.rutebanken.netex.model.ParkingArea netexParkingArea, MappingContext context) {
        super.mapBtoA(tiamatParkingArea, netexParkingArea, context);

        // NETEX Parking profile FRANCE v1.2 requires ":LOC" suffix
        netexParkingArea.setId(StringUtils.appendIfMissing(tiamatParkingArea.getNetexId(), ":LOC"));

        netexParkingArea.setPublicUse(tiamatParkingArea.getPublicUse() != null ? PublicUseEnumeration.fromValue(tiamatParkingArea.getPublicUse().value()) : PublicUseEnumeration.ALL);
        netexParkingArea.withRest(netexObjectFactory.createParkingArea_VersionStructureTotalCapacity(tiamatParkingArea.getTotalCapacity()));

        if (tiamatParkingArea.getBays() != null && !tiamatParkingArea.getBays().isEmpty()) {
            ParkingBays_RelStructure parkingBaysRelStructure = new ParkingBays_RelStructure();

            for (org.rutebanken.tiamat.model.ParkingBay tiamatBay : tiamatParkingArea.getBays()) {
                org.rutebanken.netex.model.ParkingBay netexBay = mapperFacade.map(
                        tiamatBay,
                        org.rutebanken.netex.model.ParkingBay.class
                );

                JAXBElement<ParkingBay> bayElement = netexObjectFactory.createParkingBay(netexBay);
                parkingBaysRelStructure.getParkingBayRefOrParkingBay_().add(bayElement);
            }
            JAXBElement<ParkingBays_RelStructure> baysElement =
                    netexObjectFactory.createParkingArea_VersionStructureBays(parkingBaysRelStructure);
            netexParkingArea.getRest().add(baysElement);
        }

        if (SpecificParkingAreaUsageEnumeration.PARK_AND_RIDE.equals(tiamatParkingArea.getSpecificParkingAreaUsage())) {

            TypeOfPlaceRefs_RelStructure typeOfPlaceRefRel = new TypeOfPlaceRefs_RelStructure();
            TypeOfPlaceRefStructure typeOfPlaceRef = new TypeOfPlaceRefStructure();

            if (SpecificParkingAreaUsageEnumeration.PARK_AND_RIDE.equals(tiamatParkingArea.getSpecificParkingAreaUsage())) {
                typeOfPlaceRef.withRef("parkAndRide");
            }

            typeOfPlaceRefRel.withTypeOfPlaceRef(typeOfPlaceRef);
            netexParkingArea.setPlaceTypes(typeOfPlaceRefRel);
        }

        if (tiamatParkingArea.getNumberOfBaysWithRecharging() != null){
            netexParkingArea.withRest(netexObjectFactory.createParkingArea_VersionStructureNumberOfBaysWithRecharging(tiamatParkingArea.getNumberOfBaysWithRecharging()));
        }


    }
}
