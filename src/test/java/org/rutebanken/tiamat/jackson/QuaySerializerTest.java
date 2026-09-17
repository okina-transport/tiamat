package org.rutebanken.tiamat.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuaySerializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    QuaySerializerTest() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Quay.class, new QuaySerializer());
        module.addSerializer(EmbeddableMultilingualString.class, new EmbeddableMultilingualStringSerializer());
        module.addSerializer(org.locationtech.jts.geom.Point.class, new PointSerializer());
        objectMapper.registerModule(module);
    }

    @Test
    void serialize_writesNullIdWhenIdIsNull() throws IOException {
        Quay quay = new Quay();
        quay.setNetexId("MOBIITI:Quay:1");
        quay.setName(new EmbeddableMultilingualString("Perrong 1", "no"));
        quay.setPublicCode("A");
        quay.setInseeCode("75056");
        quay.setTown("Paris");
        quay.setUrl("https://example.org/quay/1");

        String json = objectMapper.writeValueAsString(quay);

        assertEquals("{\"id\":null,\"netexId\":\"MOBIITI:Quay:1\",\"name\":{\"lang\":\"no\",\"value\":\"Perrong 1\"}," +
                "\"publicCode\":\"A\",\"inseeCode\":\"75056\",\"town\":\"Paris\",\"url\":\"https://example.org/quay/1\"}", json);
    }

    @Test
    void serialize_writesIdAndCentroidWhenPresent() throws IOException {
        Quay quay = new Quay();
        ReflectionTestUtils.setField(quay, "id", 42L);
        quay.setNetexId("MOBIITI:Quay:2");
        quay.setName(new EmbeddableMultilingualString("Perrong 2", "no"));
        quay.setCentroid(new GeometryFactory().createPoint(new Coordinate(10.5, 59.9)));

        String json = objectMapper.writeValueAsString(quay);

        assertEquals("{\"id\":42,\"netexId\":\"MOBIITI:Quay:2\",\"name\":{\"lang\":\"no\",\"value\":\"Perrong 2\"}," +
                "\"publicCode\":null,\"centroid\":{\"x\":10.5,\"y\":59.9},\"inseeCode\":null,\"town\":null,\"url\":null}", json);
    }
}
