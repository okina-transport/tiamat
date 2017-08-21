package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import org.rutebanken.netex.model.AlternativeNames_RelStructure;
import org.rutebanken.tiamat.model.AlternativeName;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlternativeNamesConverter extends BidirectionalConverter<List<AlternativeName>, AlternativeNames_RelStructure> {
    @Override
    public AlternativeNames_RelStructure convertTo(List<AlternativeName> alternativeNames, Type<AlternativeNames_RelStructure> type, MappingContext mappingContext) {
        return null;
    }

    @Override
    public List<AlternativeName> convertFrom(AlternativeNames_RelStructure alternativeNames_relStructure, Type<List<AlternativeName>> type, MappingContext mappingContext) {
        return null;
    }
}
