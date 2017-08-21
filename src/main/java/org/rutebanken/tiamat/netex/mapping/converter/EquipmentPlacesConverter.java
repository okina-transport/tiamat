package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import org.rutebanken.tiamat.model.EquipmentPlace;
import org.rutebanken.netex.model.EquipmentPlaces_RelStructure;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EquipmentPlacesConverter extends BidirectionalConverter<List<EquipmentPlace>, EquipmentPlaces_RelStructure> {

    @Override
    public EquipmentPlaces_RelStructure convertTo(List<EquipmentPlace> equipmentPlaces, Type<EquipmentPlaces_RelStructure> type, MappingContext mappingContext) {
        return null;
    }

    @Override
    public List<EquipmentPlace> convertFrom(EquipmentPlaces_RelStructure equipmentPlaces_relStructure, Type<List<EquipmentPlace>> type, MappingContext mappingContext) {
        return null;
    }
}
