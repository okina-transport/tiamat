package org.rutebanken.tiamat.importer.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.repository.QuayRepository;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StopPlaceGeocodeHandlerTest {

    private StopPlaceGeocodeHandler stopPlaceGeocodeHandler;

    @Mock
    private QuayRepository quayRepository;

    @Before
    public void setUp() {
        stopPlaceGeocodeHandler = new StopPlaceGeocodeHandler(quayRepository);
    }

    @Test
    public void setPostalCodeTest() {
        Quay quay1 = new Quay();
        quay1.setNetexId("MOBIITI:Quay:7");
        quay1.setZipCode("72455");
        Quay quay2 = new Quay();
        quay2.setNetexId("MOBIITI:Quay:255");
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate coordinate = new Coordinate(1.0, 2.0); // x = 1.0, y = 2.0
        Point point = geometryFactory.createPoint(coordinate);
        quay2.setCentroid(point);
        List<String> inputQuayIds = List.of("MOBIITI:QUAY:7", "MOBIITI:QUAY:255");
        List<Quay> databaseQuays = List.of(quay1, quay2);
        when(quayRepository.findAllLatestVersionByNetexId(inputQuayIds)).thenReturn(databaseQuays);

        stopPlaceGeocodeHandler.setPostalCode(inputQuayIds);

        Mockito.verify(quayRepository).findAllLatestVersionByNetexId(Mockito.any());
        Mockito.verify(quayRepository).saveAll(Mockito.any());
    }
}