package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.tiamat.model.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KeyValuesToKeyListConverter extends CustomConverter<Map<String, Value>, KeyListStructure> {
    @Override
    public KeyListStructure convert(Map<String, Value> stringValueMap, Type<? extends KeyListStructure> type, MappingContext mappingContext) {
        if(stringValueMap != null) {
            KeyListStructure keyListStructure = new KeyListStructure();
            for (String key : stringValueMap.keySet()) {
                Value values = stringValueMap.get(key);
                if(values != null && values.getItems() != null) {
                    String value = String.join(",", values.getItems());
                    keyListStructure.getKeyValue().add(new KeyValueStructure().withKey(key).withValue(value));
                } else {
                    // No values
                    keyListStructure.getKeyValue().add(new KeyValueStructure().withKey(key));
                }
            }
            if(keyListStructure.getKeyValue().isEmpty()) {
                return null;
            }
            return keyListStructure;
        }
        return null;
    }
}
