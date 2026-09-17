package org.rutebanken.tiamat.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;

import java.io.IOException;

public class EmbeddableMultilingualStringSerializer extends JsonSerializer<EmbeddableMultilingualString> {

    @Override
    public void serialize(EmbeddableMultilingualString value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("lang", value.getLang());
        gen.writeStringField("value", value.getValue());
        gen.writeEndObject();
    }
}
