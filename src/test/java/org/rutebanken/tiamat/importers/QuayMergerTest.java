package org.rutebanken.tiamat.importers;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.junit.Test;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.QuayRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QuayMergerTest {

    private GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();
    
    private QuayRepository quayRepository = mock(QuayRepository.class);
    
    private QuayMerger quayMerger = new QuayMerger(new KeyValueListAppender(), quayRepository);


    @Test
    public void twoQuaysWithSameOriginalIdButDifferentCoordinatesShouldBeTreatedAsSame() {

        AtomicInteger updatedQuaysCounter = new AtomicInteger();
        AtomicInteger createQuaysCounter = new AtomicInteger();

        Quay quay1 = new Quay();
        quay1.setId(123L);
        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(59, 10)));
        quay1.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");

        Quay quay2 = new Quay();
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        quay2.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(quay1);

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(quay2);


        Set<Quay> result = quayMerger.addNewQuaysOrAppendImportIds(incomingQuays, existingQuays, updatedQuaysCounter, createQuaysCounter);
        assertThat(result).hasSize(1);
    }

    @Test
    public void twoNewQuaysThatMatchesOnIdMustNotBeAddedMultipleTimes() {
        AtomicInteger updatedQuaysCounter = new AtomicInteger();
        AtomicInteger createQuaysCounter = new AtomicInteger();

        Quay quay1 = new Quay();
        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(59, 10)));
        quay1.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");

        Quay quay2 = new Quay();
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        quay2.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(quay2);
        incomingQuays.add(quay1);

        Set<Quay> result = quayMerger.addNewQuaysOrAppendImportIds(incomingQuays, new HashSet<>(), updatedQuaysCounter, createQuaysCounter);
        assertThat(result).hasSize(1);
    }

    @Test
    public void twoNewQuaysThatMatchesOnCoordinatesMustNotBeAddedMultipleTimes() {
        AtomicInteger updatedQuaysCounter = new AtomicInteger();
        AtomicInteger createQuaysCounter = new AtomicInteger();

        Quay quay1 = new Quay();
        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(59, 10)));
        quay1.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");

        Quay quay2 = new Quay();
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(59, 10)));
        quay2.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("another-id");

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(quay2);
        incomingQuays.add(quay1);

        Set<Quay> result = quayMerger.addNewQuaysOrAppendImportIds(incomingQuays, new HashSet<>(), updatedQuaysCounter, createQuaysCounter);

        assertThat(result).hasSize(1);

        for(Quay actualQuay : result) {
            assertThat(actualQuay.getOriginalIds()).contains("original-id-1", "another-id");
        }
    }

    @Test
    public void quaysAreClose() {
        Quay quay1 = new Quay();
        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(59.933307, 10.775973)));

        Quay quay2 = new Quay();
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(59.933307, 10.775973)));

        assertThat(quayMerger.areClose(quay1, quay2)).isTrue();
    }

    @Test
    public void quaysAreCloseWithSimilarCoordinates() {
        Quay quay1 = new Quay();
        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(59.933300, 10.775979)));

        Quay quay2 = new Quay();
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(59.933307, 10.775973)));

        assertThat(quayMerger.areClose(quay1, quay2)).isTrue();
    }

    @Test
    public void doesNotHaveSameCoordinates() {
        Quay quay1 = new Quay();
        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(60, 10.775973)));

        Quay quay2 = new Quay();
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(59.933307, 10.775973)));

        assertThat(quayMerger.areClose(quay1, quay2)).isFalse();
    }

    @Test
    public void notCloseEnoughIfAbout10MetersBetween() {
        Quay quay1 = new Quay(new EmbeddableMultilingualString("One side of the road"));
        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(59.858690, 10.493860)));

        Quay quay2 = new Quay(new EmbeddableMultilingualString("Other side of the road."));
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(59.858684, 10.493682)));
        assertThat(quayMerger.areClose(quay1, quay2)).isFalse();
    }

    @Test
    public void closeEnoughIfAbout8MetersBetween() {
        Quay quay1 = new Quay();
        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(59.858690, 10.493860)));

        Quay quay2 = new Quay();
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(59.858616, 10.493858)));
        assertThat(quayMerger.areClose(quay1, quay2)).isTrue();
    }


    @Test
    public void findQuayIfAlreadyExisting() {

        Point existingQuayPoint = geometryFactory.createPoint(new Coordinate(60, 11));

        Quay existingQuay = new Quay();
        existingQuay.setName(new EmbeddableMultilingualString("existing quay"));
        existingQuay.setCentroid(existingQuayPoint);

        Quay unrelatedExistingQuay = new Quay();
        unrelatedExistingQuay.setName(new EmbeddableMultilingualString("already added quay"));
        unrelatedExistingQuay.setCentroid(geometryFactory.createPoint(new Coordinate(59, 10)));

        Quay newQuayToInspect = new Quay();
        newQuayToInspect.setName(new EmbeddableMultilingualString("New quay which matches existing quay on the coordinates"));
        newQuayToInspect.setCentroid(existingQuayPoint);

        Set<Quay> existingQuays = new HashSet<>(Arrays.asList(existingQuay, unrelatedExistingQuay));
        Set<Quay> newQuays = new HashSet<>(Arrays.asList(unrelatedExistingQuay));

        Set<Quay> actual = quayMerger.addNewQuaysOrAppendImportIds(newQuays, existingQuays, new AtomicInteger(), new AtomicInteger() );
        assertThat(actual).as("The same quay object as existingQuay should be returned").contains(existingQuay);
    }

}