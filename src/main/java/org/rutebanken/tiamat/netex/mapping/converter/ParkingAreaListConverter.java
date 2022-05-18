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

package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.ParkingAreas_RelStructure;
import org.rutebanken.tiamat.model.ParkingArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ParkingAreaListConverter extends BidirectionalConverter<List<ParkingArea>, ParkingAreas_RelStructure> {

    private static final Logger logger = LoggerFactory.getLogger(ParkingAreaListConverter.class);
    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Override
    public ParkingAreas_RelStructure convertTo(List<ParkingArea> parkingAreas, Type<ParkingAreas_RelStructure> destinationType, MappingContext mappingContext) {
        if(parkingAreas == null || parkingAreas.isEmpty()) {
            return null;
        }


        ParkingAreas_RelStructure parkingAreas_relStructure = new ParkingAreas_RelStructure();

        logger.debug("Mapping {} parkingAreas to netex", parkingAreas != null ? parkingAreas.size() : 0);

        parkingAreas.forEach(parkingArea -> {
            org.rutebanken.netex.model.ParkingArea netexParkingArea = mapperFacade.map(parkingArea, org.rutebanken.netex.model.ParkingArea.class);
            parkingAreas_relStructure.getParkingAreaRefOrParkingArea_().add(netexObjectFactory.createParkingArea(netexParkingArea));
        });
        return parkingAreas_relStructure;
    }

    @Override
    public List<ParkingArea> convertFrom(ParkingAreas_RelStructure parkingAreas_relStructure, Type<List<ParkingArea>> destinationType, MappingContext mappingContext) {
        logger.debug("Mapping {} quays to internal model", parkingAreas_relStructure != null ? parkingAreas_relStructure.getParkingAreaRefOrParkingArea_().size() : 0);
        List<ParkingArea> parkingAreas = new ArrayList<>();
        if(parkingAreas_relStructure.getParkingAreaRefOrParkingArea_() != null) {
            parkingAreas_relStructure.getParkingAreaRefOrParkingArea_().stream()
                    .filter(object -> object.getValue() instanceof org.rutebanken.netex.model.ParkingArea)
                    .map(object -> ((org.rutebanken.netex.model.ParkingArea) object.getValue()))
                    .map(netexParkingArea -> {
                        ParkingArea tiamatQuay = mapperFacade.map(netexParkingArea, ParkingArea.class);
                        return tiamatQuay;
                    })
                    .forEach(parkingArea -> parkingAreas.add(parkingArea));
        }

        return parkingAreas;
    }
}
