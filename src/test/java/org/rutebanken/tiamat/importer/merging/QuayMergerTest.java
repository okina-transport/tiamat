/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.importer.merging;

import com.google.common.collect.Sets;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.geotools.referencing.GeodeticCalculator;
import org.junit.Test;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.matching.OriginalIdMatcher;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.netex.id.NetexIdHelper;
import org.rutebanken.tiamat.netex.id.ValidPrefixList;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.springframework.test.annotation.DirtiesContext;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class QuayMergerTest {

    private GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();

    private NetexIdHelper netexIdHelper = new NetexIdHelper(new ValidPrefixList("NSR", new HashMap<>()));
    private QuayMerger quayMerger = new QuayMerger(new OriginalIdMatcher(netexIdHelper));

    @Test
    public void disableMatchingQuaysWithinLowDistanceBeforeIdMatch() {

        Quay quay1 = new Quay();
        quay1.getOriginalIds().add("BRA:Quay:12321234");
        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(59, 10)));


        Quay quay2 = new Quay();
        quay2.getOriginalIds().add("BRA:Quay:12321234");
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(59, 10)));

        ImportParams importParams = new ImportParams();
        importParams.keepStopGeolocalisation = true;
        importParams.keepStopNames = true;

        Set<Quay> result = quayMerger.mergeQuays(null, Sets.newHashSet(quay2), Sets.newHashSet(quay1), new AtomicInteger(), new AtomicInteger(), true, false, importParams);

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getChanged()).isNull();
    }

    @Test
    public void twoQuaysWithSameOriginalIdAfterPrefixShouldBeTreatedAsSame() {

        AtomicInteger updatedQuaysCounter = new AtomicInteger();
        AtomicInteger createQuaysCounter = new AtomicInteger();

        Quay quay1 = new Quay();
        quay1.setNetexId("123");
        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(59, 10)));
        quay1.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("BRA:StopArea:123123");

        Quay quay2 = new Quay();
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        quay2.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("RUT:StopArea:123123");

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(quay1);

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(quay2);

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, existingQuays, updatedQuaysCounter, createQuaysCounter, true);
        assertThat(result).hasSize(1);
    }

    @Test
    public void twoQuaysWithSameOriginalIdButDifferentCoordinatesShouldBeTreatedAsSame() {

        AtomicInteger updatedQuaysCounter = new AtomicInteger();
        AtomicInteger createQuaysCounter = new AtomicInteger();

        Quay quay1 = new Quay();
        quay1.setNetexId("123");
        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(59, 10)));
        quay1.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");

        Quay quay2 = new Quay();
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        quay2.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(quay1);

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(quay2);


        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, existingQuays, updatedQuaysCounter, createQuaysCounter, true);
        assertThat(result).hasSize(1);
    }

    /**
     * https://rutebanken.atlassian.net/browse/NRP-894
     */
    @Test
    public void twoQuaysWithDifferentBearingPointShouldNotBeTreatedAsSame() {

        Quay west = new Quay();
        west.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        west.setCompassBearing(270f);
        west.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");

        Quay east = new Quay();
        east.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        east.setCompassBearing(40f);
        east.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-2");

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(west);

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(east);

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, existingQuays, new AtomicInteger(), new AtomicInteger(), true);
        assertThat(result).as("Number of quays in response").hasSize(2);
    }

    @Test
    public void twoQuaysWithSimilarCompassBearing() {
        Quay one = new Quay();
        one.setCompassBearing(1f);

        Quay two = new Quay();
        two.setCompassBearing(60f);

        assertThat(quayMerger.haveSimilarOrAnyNullCompassBearing(one, two))
                .as("Quays with less than 180 degrees difference should be treated as same bearing point")
                .isTrue();
    }

    @Test
    public void twoQuaysWithSimilarCompassBearingCrossingZero() {
        Quay one = new Quay();
        one.setCompassBearing(350f);

        Quay two = new Quay();
        two.setCompassBearing(2f);

        assertThat(quayMerger.haveSimilarOrAnyNullCompassBearing(one, two))
                .as("Quays with less than 180 degrees difference should be treated as same bearing point")
                .isTrue();
    }

    @Test
    public void twoQuaysWithTooMuchdifferenceInCompassBearing() {
        Quay one = new Quay();
        one.setCompassBearing(90f);

        Quay two = new Quay();
        two.setCompassBearing(290f);

        assertThat(quayMerger.haveSimilarOrAnyNullCompassBearing(one, two)).isFalse();
    }

    @Test
    public void twoQuaysWithSimilarCompassBearingOneAndThreeFiftyNine() {
        Quay one = new Quay();
        one.setCompassBearing(1f);

        Quay two = new Quay();
        two.setCompassBearing(359f);

        assertThat(quayMerger.haveSimilarOrAnyNullCompassBearing(one, two)).isTrue();
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

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, new HashSet<>(), updatedQuaysCounter, createQuaysCounter, true);
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

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, new HashSet<>(), updatedQuaysCounter, createQuaysCounter, true);

        assertThat(result).hasSize(1);

        for (Quay actualQuay : result) {
            assertThat(actualQuay.getOriginalIds()).contains("original-id-1", "another-id");
        }
    }


    /**
     * Add two new quays with already existing original IDs with different coordinates that are close to other quay.
     */
    @Test
    public void idsMustNotBeAddedToOtherQuayEvenIfTheyAreClose() {
        Quay existingQuay1 = new Quay(new EmbeddableMultilingualString("Fredheimveien"));
        existingQuay1.setNetexId("123");
        existingQuay1.setCentroid(geometryFactory.createPoint(new Coordinate(11.142897636770531, 59.83297022041692)));
        existingQuay1.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("RUT:StopArea:0229012202");

        Quay existingQuay2 = new Quay(new EmbeddableMultilingualString("Fredheimveien"));
        existingQuay2.setNetexId("2");
        existingQuay2.setCentroid(geometryFactory.createPoint(new Coordinate(11.142676854561447, 59.83314448493502)));
        existingQuay2.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("RUT:StopArea:0229012201");

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(existingQuay1);
        existingQuays.add(existingQuay2);

        Quay incomingQuay1 = new Quay(new EmbeddableMultilingualString("Fredheimveien"));
        incomingQuay1.setCentroid(geometryFactory.createPoint(new Coordinate(11.14317535486387, 59.832848923825956)));
        incomingQuay1.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("RUT:StopArea:0229012202");

        Quay incomingQuay2 = new Quay(new EmbeddableMultilingualString("Fredheimveien"));
        incomingQuay2.setCentroid(geometryFactory.createPoint(new Coordinate(11.142902250197631, 59.83304200609072)));
        incomingQuay2.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("RUT:StopArea:0229012201");

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(incomingQuay2);
        incomingQuays.add(incomingQuay1);

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, existingQuays, new AtomicInteger(), new AtomicInteger(), true);
        assertThat(result).hasSize(2);

        List<String> actualOriginalIds = result.stream()
                .flatMap(q -> q.getOriginalIds().stream())
                .peek(originalId -> System.out.println(originalId))
                .collect(toList());

        assertThat(actualOriginalIds).as("Number of original IDs in total").hasSize(2);

        result.forEach(q -> System.out.println(q.getOriginalIds()));
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
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(59.858674,10.493818)));
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

        Set<Quay> actual = quayMerger.mergeQuays(newQuays, existingQuays, new AtomicInteger(), new AtomicInteger(), true);
        assertThat(actual).as("The same quay object as existingQuay should be returned").contains(existingQuay);
    }

    /**
     * https://rutebanken.atlassian.net/browse/NRP-1149
     */
    @Test
    public void twoQuaysOneWithCompassBearingAndOtherWithoutShouldMatchIfNearby() {

        Quay first = new Quay();
        first.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        first.setCompassBearing(270f);
        first.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");
        first.setName(new EmbeddableMultilingualString("A"));

        Quay second = new Quay();
        second.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        // No compass bearing
        second.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-2");
        first.setName(new EmbeddableMultilingualString("A"));

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(first);

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(second);

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, existingQuays, new AtomicInteger(), new AtomicInteger(), true);
        assertThat(result).as("Number of quays in response should be one. Because one quay lacks compass bearing").hasSize(1);
    }

    @Test
    public void twoQuaysOneWithCompassBearingAndOtherWithoutShouldNotMatchIfNearbyButDifferentName() {

        Quay first = new Quay();
        first.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        first.setCompassBearing(270f);
        first.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");
        first.setName(new EmbeddableMultilingualString("A"));

        Quay second = new Quay();
        second.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        // No compass bearing
        second.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-2");
        second.setName(new EmbeddableMultilingualString("B"));

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(first);

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(second);

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, existingQuays, new AtomicInteger(), new AtomicInteger(), true);
        assertThat(result).as("Number of quays in response should be two. Because name differs.").hasSize(2);
    }

    @Test
    public void ifTwoQuaysAreMergedKeepName() {

        Quay first = new Quay();
        first.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        first.setCompassBearing(270f);
        first.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");

        Quay second = new Quay();
        second.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        // No compass bearing
        second.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");
        second.setName(new EmbeddableMultilingualString("A"));

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(first);

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(second);

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, existingQuays, new AtomicInteger(), new AtomicInteger(), true);
        assertThat(result).as("Quays should have been merged.").hasSize(1);
        Quay actual = result.iterator().next();
        assertThat(actual.getName()).describedAs("name should not be null").isNotNull();
        assertThat(actual.getName().getValue()).isEqualTo("A");
    }


    @Test
    public void ifTwoQuaysAreMergedKeepCompassBearing() {

        Quay first = new Quay();
        first.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        first.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");

        Quay second = new Quay();
        second.setCentroid(geometryFactory.createPoint(new Coordinate(60, 11)));
        second.setCompassBearing(270f);
        second.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("original-id-1");

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(first);

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(second);

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, existingQuays, new AtomicInteger(), new AtomicInteger(), true);
        assertThat(result).as("Quays should have been merged.").hasSize(1);
        Quay actual = result.iterator().next();
        assertThat(actual.getCompassBearing()).describedAs("compass bearing should not be null").isNotNull();
        assertThat(actual.getCompassBearing()).isEqualTo(270f);
    }

    /**
     * When two quays have similar compass bearing, we can use a greater limit for distance in meters when merging.
     */
    @Test
    public void ifTwoQuaysHaveSimilarCompassBearingIncreaseMergeDistance() {

        Quay first = new Quay();
        Point firstQuayPoint = geometryFactory.createPoint(new Coordinate(60, 11));
        first.setCentroid(firstQuayPoint);
        first.setCompassBearing(270f);
        first.setPublicCode("test");

        Quay second = new Quay();
        second.setCentroid(getOffsetPoint(firstQuayPoint, 29, 15));
        second.setCompassBearing(270f);

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(first);

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(second);

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, existingQuays, new AtomicInteger(), new AtomicInteger(), true);
        assertThat(result).as("Quays should have been merged.").hasSize(1);
        Quay actual = result.iterator().next();

        assertThat(actual.getCentroid()).isEqualTo(first.getCentroid());
    }

    @Test
    public void twoQuaysWithSimilarCompassBearingNoMatchIfDistanceExceedsExtendedMergeDistance() {
        int distanceBetweenQuays = 40;

        Quay first = new Quay();
        Point firstQuayPoint = geometryFactory.createPoint(new Coordinate(60, 11));
        first.setCentroid(firstQuayPoint);
        first.setCompassBearing(270f);

        Quay second = new Quay();
        second.setCentroid(getOffsetPoint(firstQuayPoint, distanceBetweenQuays, 15));
        second.setCompassBearing(270f);

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(first);

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(second);

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, existingQuays, new AtomicInteger(), new AtomicInteger(), true);
        assertThat(result).as("Quays should NOT have been merged, because the distance between them exceeds extended merge distance.").hasSize(2);
        Quay actual = result.iterator().next();

        assertThat(actual.getCentroid()).isEqualTo(first.getCentroid());
    }

    @Test
    public void twoQuaysWithDifferentPublicCodeShouldNotBeMerged() {

        Quay first = new Quay();
        Point firstQuayPoint = geometryFactory.createPoint(new Coordinate(60, 11));
        first.setCentroid(firstQuayPoint);
        first.setPublicCode("X");

        Quay second = new Quay();
        second.setCentroid(firstQuayPoint);
        second.setPublicCode("Y");

        Set<Quay> existingQuays = new HashSet<>();
        existingQuays.add(first);

        Set<Quay> incomingQuays = new HashSet<>();
        incomingQuays.add(second);

        // Add stop place to manually check that we are logging stop place's original ID
        StopPlace stopPlaceForLogging = new StopPlace(new EmbeddableMultilingualString("Asker"));
        stopPlaceForLogging.getOriginalIds().add("12341234");

        Set<Quay> result = quayMerger.mergeQuays(stopPlaceForLogging, incomingQuays, existingQuays, new AtomicInteger(), new AtomicInteger(), false);
        assertThat(result).as("Quay should NOT have been added. Public Code is different").hasSize(1);
    }

    /**
     * Found no match for incoming quay Quay{version=1, centroid=POINT (11.576607 60.19752), publicCode=1, keyValues={
     * imported-id=Value{id=0, items=[RUT:Quay:236040101, NRI:Quay:762010606, RUT:Quay:0236040101]}}}. Looking in list of quays: [Quay{id=18775, netexId=NSR:Quay:21454, version=1, centroid=POINT (11.577104 60.197563), b
     * earing=1.0, publicCode=2, keyValues={imported-id=Value{id=32160, items=[RUT:Quay:236040102, NRI:Quay:762010607, HED:Quay:0419020102, RUT:Quay:0236040102]}}}, Quay{id=18776, netexId=NSR:Quay:21455, version=1, cent
     * roid=POINT (11.57647 60.197437), publicCode=1, keyValues={imported-id=Value{id=32161, items=[HED:Quay:0419020101]}}}]
     */
    @Test
    public void dysterudBru() {
        Quay incomingQuay = new Quay();
        incomingQuay.setCentroid(geometryFactory.createPoint(new Coordinate(11.576007, 60.19752)));
        incomingQuay.setPublicCode("1");
        incomingQuay.getOriginalIds().addAll(Arrays.asList("RUT:Quay:236040101", "NRI:Quay:762010606", "RUT:Quay:0236040101"));

        // Adding compass bearing, because the extended matching limit must be used to match incoming quay and existingQuay2 (~27 meters from each other)
        incomingQuay.setCompassBearing(4.0f);

        Set<Quay> incomingQuays = new HashSet<>(Sets.newHashSet(incomingQuay));

        Quay existingQuay1 = new Quay();
        existingQuay1.setNetexId("NSR:Quay:21454");
        existingQuay1.setCentroid(geometryFactory.createPoint(new Coordinate(11.577104, 60.197563)));
        existingQuay1.setCompassBearing(1.0f);
        existingQuay1.setPublicCode("2");
        existingQuay1.getOriginalIds().addAll(new ArrayList<>(Arrays.asList("RUT:Quay:236040102", "NRI:Quay:762010607", "HED:Quay:0419020102", "RUT:Quay:0236040102")));

        Quay existingQuay2 = new Quay();
        existingQuay2.setNetexId("NSR:Quay:21455");
        existingQuay2.setCentroid(geometryFactory.createPoint(new Coordinate(11.57647, 60.197437)));
        existingQuay2.setPublicCode("1");
        existingQuay2.getOriginalIds().addAll(new ArrayList<>(Arrays.asList("HED:Quay:0419020101")));
        existingQuay2.setCompassBearing(14.0f);

        Set<Quay> existingQuays = new HashSet<>(Sets.newHashSet(existingQuay1, existingQuay2));

        Set<Quay> result = quayMerger.mergeQuays(incomingQuays, existingQuays, new AtomicInteger(), new AtomicInteger(), false);
        assertThat(result).hasSize(2);
        assertThat(result
                .stream()
                .filter(quay -> quay.getNetexId().equals(existingQuay2.getNetexId()))
                .anyMatch(quay -> quay.getOriginalIds().containsAll(incomingQuay.getOriginalIds())))
                .as("new quay original IDs should have been appended " + incomingQuay.getOriginalIds())
                .isTrue();

    }

    @Test
    public void matchQuaysIfMissingPublicCode() {
        Quay existingQuay = new Quay();
        existingQuay.setCentroid(geometryFactory.createPoint(new Coordinate(16.502, 68.59)));
        existingQuay.setPublicCode("01");
        existingQuay.getOriginalIds().addAll(new ArrayList<>(Arrays.asList("TRO:Quay:1903208101")));

        Quay incomingQuay = new Quay();
        incomingQuay.setCentroid(geometryFactory.createPoint(new Coordinate(16.502, 68.59)));
        incomingQuay.setCompassBearing(353.0f);
        incomingQuay.getOriginalIds().addAll(Arrays.asList("NOR:Quay:2001208101"));

        Set<Quay> result = quayMerger.mergeQuays(Sets.newHashSet(incomingQuay), Sets.newHashSet(existingQuay), new AtomicInteger(), new AtomicInteger(), false);

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isEqualTo(existingQuay);
        assertThat(result.iterator().next().getOriginalIds()).contains(incomingQuay.getOriginalIds().iterator().next());
    }

    /**
     * NRP-1556 Even if there is an id match, prefer closer quays...
     * Reason: Inconsistency in stop place data
     */
    @Test
    public void matchCloseQuaysBeforeIdMatch() {

        // Existing
        Quay existingQuay = new Quay();
        existingQuay.setCentroid(geometryFactory.createPoint(new Coordinate(12.315012269, 64.637640437)));
        existingQuay.setPublicCode("01");
        existingQuay.getOriginalIds().addAll(new ArrayList<>(Arrays.asList("NTR:Quay:1743901001")));

        Quay existingQuay2 = new Quay();
        existingQuay2.setCentroid(geometryFactory.createPoint(new Coordinate(12.315137008, 64.638370312)));
        existingQuay2.setPublicCode("02");
        existingQuay2.getOriginalIds().addAll(new ArrayList<>(Arrays.asList("NTR:Quay:1743901002")));


        // Incoming
        Quay incomingQuay = new Quay();
        incomingQuay.setCentroid(geometryFactory.createPoint(new Coordinate(12.315004, 64.637639)));
        incomingQuay.setCompassBearing(16.0f);
        incomingQuay.getOriginalIds().addAll(Arrays.asList("NOR:Quay:21439010", "NOR:Quay:2143901001"));

        Quay incomingQuay2 = new Quay();
        incomingQuay2.setCentroid(geometryFactory.createPoint(new Coordinate(12.315136, 64.638373)));
        incomingQuay2.setCompassBearing(191.0f);
        incomingQuay2.getOriginalIds().addAll(Arrays.asList("NOR:Quay:17439010", "NOR:Quay:1743901001"));

        // Merge
        Set<Quay> result = quayMerger.mergeQuays(Sets.newHashSet(incomingQuay2, incomingQuay ), Sets.newHashSet(existingQuay2, existingQuay), new AtomicInteger(), new AtomicInteger(), false);

        assertThat(result).hasSize(2);
        assertThat(existingQuay.getOriginalIds()).containsAll(incomingQuay.getOriginalIds());
        assertThat(existingQuay.getPublicCode()).isEqualTo("01");
        assertThat(existingQuay.getCompassBearing()).isEqualTo(incomingQuay.getCompassBearing());
        assertThat(existingQuay.getOriginalIds()).doesNotContainAnyElementsOf(incomingQuay2.getOriginalIds());

        assertThat(existingQuay2.getOriginalIds()).containsAll(incomingQuay2.getOriginalIds());
        assertThat(existingQuay2.getPublicCode()).isEqualTo("02");
        assertThat(existingQuay2.getCompassBearing()).isEqualTo(incomingQuay2.getCompassBearing());
        assertThat(existingQuay2.getOriginalIds()).doesNotContainAnyElementsOf(incomingQuay.getOriginalIds());

    }

    private Point getOffsetPoint(Point point, int offsetMeters, int azimuth) {
        GeodeticCalculator calc = new GeodeticCalculator();
        calc.setStartingGeographicPoint(point.getX(), point.getY());
        calc.setDirection(azimuth, offsetMeters);
        Point2D dest = calc.getDestinationGeographicPoint();
        return geometryFactory.createPoint(new Coordinate(dest.getX(), dest.getY()));
    }

}