package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import org.rutebanken.netex.model.CheckConstraints_RelStructure;
import org.rutebanken.tiamat.model.CheckConstraint;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CheckConstraintsConverter extends BidirectionalConverter<List<CheckConstraint>, CheckConstraints_RelStructure> {

    @Override
    public CheckConstraints_RelStructure convertTo(List<CheckConstraint> checkConstraints, Type<CheckConstraints_RelStructure> type, MappingContext mappingContext) {
        return null;
    }

    @Override
    public List<CheckConstraint> convertFrom(CheckConstraints_RelStructure checkConstraints_relStructure, Type<List<CheckConstraint>> type, MappingContext mappingContext) {
        return null;
    }
}
