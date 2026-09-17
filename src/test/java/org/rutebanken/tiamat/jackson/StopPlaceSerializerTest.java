package org.rutebanken.tiamat.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.VehicleModeEnumeration;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StopPlaceSerializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    StopPlaceSerializerTest() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(StopPlace.class, new StopPlaceSerializer());
        module.addSerializer(Quay.class, new QuaySerializer());
        module.addSerializer(EmbeddableMultilingualString.class, new EmbeddableMultilingualStringSerializer());
        module.addSerializer(Point.class, new PointSerializer());
        objectMapper.registerModule(module);
    }

    @Test
    void serialize_writesNullIdAndEmptyQuaysWhenAbsent() throws IOException {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setNetexId("MOBIITI:StopPlace:1");
        stopPlace.setName(new EmbeddableMultilingualString("Jernbanetorget", "no"));

        String json = objectMapper.writeValueAsString(stopPlace);

        assertEquals("{\"id\":null,\"netexId\":\"MOBIITI:StopPlace:1\",\"version\":0," +
                "\"name\":{\"lang\":\"no\",\"value\":\"Jernbanetorget\"},\"quays\":[]}", json);
    }

    @Test
    void serialize_writesIdTransportModeStopPlaceTypeCentroidAndQuays() throws IOException {
        StopPlace stopPlace = new StopPlace();
        ReflectionTestUtils.setField(stopPlace, "id", 7L);
        stopPlace.setNetexId("MOBIITI:StopPlace:2");
        stopPlace.setVersion(2);
        stopPlace.setName(new EmbeddableMultilingualString("Jernbanetorget", "no"));
        stopPlace.setTransportMode(VehicleModeEnumeration.BUS);
        stopPlace.setStopPlaceType(StopTypeEnumeration.ONSTREET_BUS);
        stopPlace.setCentroid(new GeometryFactory().createPoint(new Coordinate(10.5, 59.9)));

        Quay quay = new Quay();
        quay.setNetexId("MOBIITI:Quay:1");
        quay.setName(new EmbeddableMultilingualString("Perrong 1", "no"));
        Set<Quay> quays = new LinkedHashSet<>();
        quays.add(quay);
        stopPlace.setQuays(quays);

        String json = objectMapper.writeValueAsString(stopPlace);

        assertEquals("{\"id\":7,\"netexId\":\"MOBIITI:StopPlace:2\",\"version\":2," +
                "\"name\":{\"lang\":\"no\",\"value\":\"Jernbanetorget\"}," +
                "\"transportMode\":\"bus\",\"stopPlaceType\":\"onstreetBus\"," +
                "\"centroid\":{\"x\":10.5,\"y\":59.9}," +
                "\"quays\":[{\"id\":null,\"netexId\":\"MOBIITI:Quay:1\"," +
                "\"name\":{\"lang\":\"no\",\"value\":\"Perrong 1\"},\"publicCode\":null," +
                "\"inseeCode\":null,\"town\":null,\"url\":null}]}", json);
    }

    @Test
    void serialize_writesChildrenWhenPresent() throws IOException {
        StopPlace child = new StopPlace();
        child.setNetexId("MOBIITI:StopPlace:2");
        child.setName(new EmbeddableMultilingualString("Jernbanetorget vest", "no"));

        StopPlace stopPlace = new StopPlace();
        stopPlace.setNetexId("MOBIITI:StopPlace:1");
        stopPlace.setName(new EmbeddableMultilingualString("Jernbanetorget", "no"));
        Set<StopPlace> children = new LinkedHashSet<>();
        children.add(child);
        stopPlace.setChildren(children);

        String json = objectMapper.writeValueAsString(stopPlace);

        assertEquals("{\"id\":null,\"netexId\":\"MOBIITI:StopPlace:1\",\"version\":0," +
                "\"name\":{\"lang\":\"no\",\"value\":\"Jernbanetorget\"},\"quays\":[]," +
                "\"children\":[{\"id\":null,\"netexId\":\"MOBIITI:StopPlace:2\",\"version\":0," +
                "\"name\":{\"lang\":\"no\",\"value\":\"Jernbanetorget vest\"},\"quays\":[]}]}", json);
    }
}
