package org.rutebanken.tiamat.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmbeddableMultilingualStringSerializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    EmbeddableMultilingualStringSerializerTest() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(EmbeddableMultilingualString.class, new EmbeddableMultilingualStringSerializer());
        objectMapper.registerModule(module);
    }

    @Test
    void serialize_writesLangAndValueFields() throws IOException {
        EmbeddableMultilingualString name = new EmbeddableMultilingualString("Jernbanetorget", "no");

        String json = objectMapper.writeValueAsString(name);

        assertEquals("{\"lang\":\"no\",\"value\":\"Jernbanetorget\"}", json);
    }

    @Test
    void serialize_writesNullFieldsWhenValueAndLangAreNull() throws IOException {
        EmbeddableMultilingualString name = new EmbeddableMultilingualString();

        String json = objectMapper.writeValueAsString(name);

        assertEquals("{\"lang\":null,\"value\":null}", json);
    }
}
