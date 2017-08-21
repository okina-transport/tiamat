package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import org.rutebanken.netex.model.AccessSpaces_RelStructure;
import org.rutebanken.tiamat.model.AccessSpace;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccessSpacesConverter extends BidirectionalConverter<List<AccessSpace>, AccessSpaces_RelStructure> {
    @Override
    public AccessSpaces_RelStructure convertTo(List<AccessSpace> accessSpaces, Type<AccessSpaces_RelStructure> type, MappingContext mappingContext) {
        return null;
    }

    @Override
    public List<AccessSpace> convertFrom(AccessSpaces_RelStructure accessSpaces_relStructure, Type<List<AccessSpace>> type, MappingContext mappingContext) {
        return null;
    }
}
