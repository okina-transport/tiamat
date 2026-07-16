package org.rutebanken.tiamat.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.rutebanken.tiamat.model.Quay;

import java.io.IOException;

public class QuaySerializer extends JsonSerializer<Quay> {

    @Override
    public void serialize(Quay value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        if (value.getId() != null) {
            gen.writeNumberField("id", value.getId());
        } else {
            gen.writeNullField("id");
        }
        gen.writeStringField("netexId", value.getNetexId());
        serializers.defaultSerializeField("name", value.getName(), gen);
        gen.writeStringField("publicCode", value.getPublicCode());
        if (value.getCentroid() != null) {
            serializers.defaultSerializeField("centroid", value.getCentroid(), gen);
        }
        gen.writeStringField("inseeCode", value.getInseeCode());
        gen.writeStringField("town", value.getTown());
        gen.writeStringField("url", value.getUrl());
        gen.writeEndObject();
    }
}
