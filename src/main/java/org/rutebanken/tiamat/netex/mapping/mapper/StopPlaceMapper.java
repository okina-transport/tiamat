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

import io.micrometer.core.instrument.util.StringUtils;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.exporter.params.TiamatVehicleModeStopPlacetypeMapping;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.repository.StopPlaceRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class StopPlaceMapper extends CustomMapper<StopPlace, org.rutebanken.tiamat.model.StopPlace> {

    public static final String IS_PARENT_STOP_PLACE = "IS_PARENT_STOP_PLACE";

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceMapper.class);

    private final PublicationDeliveryHelper publicationDeliveryHelper;

    @Autowired
    public StopPlaceMapper(PublicationDeliveryHelper publicationDeliveryHelper) {
        this.publicationDeliveryHelper = publicationDeliveryHelper;
    }

    @Override
    public void mapAtoB(StopPlace netexStopPlace, org.rutebanken.tiamat.model.StopPlace stopPlace, MappingContext context) {
        try{
            super.mapAtoB(netexStopPlace, stopPlace, context);
            if (netexStopPlace.getPlaceEquipments() != null &&
                    netexStopPlace.getPlaceEquipments().getInstalledEquipmentRefOrInstalledEquipment() != null &&
                    netexStopPlace.getPlaceEquipments().getInstalledEquipmentRefOrInstalledEquipment().isEmpty()) {
                netexStopPlace.setPlaceEquipments(null);
                stopPlace.setPlaceEquipments(null);
            }

            if (netexStopPlace.getAlternativeNames() != null &&
                    netexStopPlace.getAlternativeNames().getAlternativeName() != null &&
                    !netexStopPlace.getAlternativeNames().getAlternativeName().isEmpty()) {

                List<AlternativeName> netexAlternativeName = netexStopPlace.getAlternativeNames().getAlternativeName();
                List<org.rutebanken.tiamat.model.AlternativeName> alternativeNames = new ArrayList<>();

                for (AlternativeName netexAltName : netexAlternativeName) {
                    if (netexAltName != null
                            && netexAltName.getName() != null
                            && netexAltName.getName().getValue() != null
                            && !netexAltName.getName().getValue().isEmpty()) {
                        //Only include non-empty alternative names
                        org.rutebanken.tiamat.model.AlternativeName tiamatAltName = new org.rutebanken.tiamat.model.AlternativeName();
                        mapperFacade.map(netexAltName, tiamatAltName);
                        alternativeNames.add(tiamatAltName);
                    }
                }

                if (!alternativeNames.isEmpty()) {
                    stopPlace.getAlternativeNames().addAll(alternativeNames);
                }
            }

            String isParentStopPlaceStringValue = publicationDeliveryHelper.getValueByKey(netexStopPlace, IS_PARENT_STOP_PLACE);
            if(isParentStopPlaceStringValue != null) {
                if(isParentStopPlaceStringValue.equalsIgnoreCase("true")) {
                    stopPlace.setParentStopPlace(true);
                }
            }
        }catch(Exception e ){
            logger.error("Can't map to tiamat stop place:" + netexStopPlace.getId());
        }



    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.StopPlace stopPlace, StopPlace netexStopPlace, MappingContext context) {
        try{
            super.mapBtoA(stopPlace, netexStopPlace, context);
            if (stopPlace.getPlaceEquipments() != null &&
                    stopPlace.getPlaceEquipments().getInstalledEquipment() != null &&
                    stopPlace.getPlaceEquipments().getInstalledEquipment().isEmpty()) {
                stopPlace.setPlaceEquipments(null);
                netexStopPlace.setPlaceEquipments(null);
            }

            if (stopPlace.getAlternativeNames() != null &&
                    !stopPlace.getAlternativeNames().isEmpty()) {
                List<org.rutebanken.tiamat.model.AlternativeName> alternativeNames = stopPlace.getAlternativeNames();
                List<AlternativeName> netexAlternativeNames = new ArrayList<>();

                for (org.rutebanken.tiamat.model.AlternativeName alternativeName : alternativeNames) {
                    if (alternativeName != null
                            && alternativeName.getName() != null
                            && alternativeName.getName().getValue() != null
                            && !alternativeName.getName().getValue().isEmpty()) {
                        //Only include non-empty alternative names
                        AlternativeName netexAltName = new AlternativeName();
                        mapperFacade.map(alternativeName, netexAltName);
                        netexAltName.setId(alternativeName.getNetexId());
                        netexAlternativeNames.add(netexAltName);
                    }
                }

                if (!netexAlternativeNames.isEmpty()) {
                    AlternativeNames_RelStructure altName = new AlternativeNames_RelStructure();
                    altName.getAlternativeName().addAll(netexAlternativeNames);
                    netexStopPlace.setAlternativeNames(altName);
                }
            } else {
                netexStopPlace.setAlternativeNames(null);
            }

            if(netexStopPlace.getKeyList() == null) {
                netexStopPlace.withKeyList(new KeyListStructure());
            }
            netexStopPlace.getKeyList()
                    .withKeyValue(new KeyValueStructure()
                            .withKey(IS_PARENT_STOP_PLACE)
                            .withValue(String.valueOf(stopPlace.isParentStopPlace())));


            if (stopPlace.getTransportMode() == null){
                feedTransportMode(stopPlace,netexStopPlace);
            }

            if (stopPlace.getName() == null || StringUtils.isEmpty(stopPlace.getName().getValue())){
                //handle empty or null name in stopPlace

                MultilingualString name = new MultilingualString();
                name.setValue("");
                name.setLang("fr");
                netexStopPlace.setName(name);
            }

        }catch(Exception e){
            logger.error("Can't map to netex stop place:" + stopPlace.getNetexId());
        }

    }


    private void feedTransportMode(org.rutebanken.tiamat.model.StopPlace stopPlace, StopPlace netexStopPlace){
        if(stopPlace.getStopPlaceType() != null ){
            org.rutebanken.tiamat.model.VehicleModeEnumeration transportMode = TiamatVehicleModeStopPlacetypeMapping.getVehicleModeEnumeration(stopPlace.getStopPlaceType());
            if (transportMode == null ){
                logger.error("Unable to find transportMode for stopPlaceType:" + stopPlace.getStopPlaceType() + ", on stopPlace:" + stopPlace.getNetexId());
            }
            netexStopPlace.setTransportMode(VehicleModeEnumeration.fromValue(transportMode.value()));
        }else{
            //Neither transportMode or stop place type is filled. Filling transport mode with "other" type
            netexStopPlace.setTransportMode(VehicleModeEnumeration.OTHER);
        }

    }
}
