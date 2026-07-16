package org.rutebanken.tiamat.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PointSerializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PointSerializerTest() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Point.class, new PointSerializer());
        objectMapper.registerModule(module);
    }

    @Test
    void serialize_writesXAndYFields() throws IOException {
        Point point = new GeometryFactory().createPoint(new Coordinate(10.5, 59.9));

        String json = objectMapper.writeValueAsString(point);

        assertEquals("{\"x\":10.5,\"y\":59.9}", json);
    }
}
