package org.rutebanken.tiamat.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.tiamat.model.StopPlace;

import java.io.IOException;

public class StopPlaceSerializer extends JsonSerializer<StopPlace> {

    @Override
    public void serialize(StopPlace value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        if (value.getId() != null) {
            gen.writeNumberField("id", value.getId());
        } else {
            gen.writeNullField("id");
        }
        gen.writeStringField("netexId", value.getNetexId());
        gen.writeNumberField("version", value.getVersion());
        serializers.defaultSerializeField("name", value.getName(), gen);
        if (value.getTransportMode() != null) {
            gen.writeStringField("transportMode", value.getTransportMode().value());
        }
        if (value.getStopPlaceType() != null) {
            gen.writeStringField("stopPlaceType", value.getStopPlaceType().value());
        }
        if (value.getCentroid() != null) {
            serializers.defaultSerializeField("centroid", value.getCentroid(), gen);
        }
        gen.writeArrayFieldStart("quays");
        for (var quay : value.getQuays()) {
            serializers.defaultSerializeValue(quay, gen);
        }
        gen.writeEndArray();
        
        if (CollectionUtils.isNotEmpty(value.getChildren())) {
            gen.writeArrayFieldStart("children");
            for (var child : value.getChildren()) {
                serializers.defaultSerializeValue(child, gen);
            }
            gen.writeEndArray();
        }

        gen.writeEndObject();
    }
}
