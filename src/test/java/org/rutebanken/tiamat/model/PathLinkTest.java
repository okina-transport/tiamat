package org.rutebanken.tiamat.model;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rutebanken.tiamat.CommonSpringBootTest;
import org.rutebanken.tiamat.TiamatApplication;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.PathJunctionRepository;
import org.rutebanken.tiamat.repository.PathLinkRepository;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class PathLinkTest extends CommonSpringBootTest {

    @Autowired
    private PathLinkRepository pathLinkRepository;

    @Autowired
    private PathJunctionRepository pathJunctionRepository;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private QuayRepository quayRepository;

    @Autowired
    private GeometryFactory geometryFactory;

    @Test
    public void simplePersistTest() {
        PathLink pathLink = new PathLink();
        pathLinkRepository.save(pathLink);
        assertThat(pathLink.getId()).describedAs("Path link should get ID when saved").isNotNull();
    }

    @Test
    public void persistPathLinkWithPathLinkEnd() {
        PathLink pathLink = new PathLink();
        PathLinkEnd from = new PathLinkEnd(createAndSaveStop("A stop place that is referenced to by a path link"));

        pathLink.setFrom(from);

        pathLinkRepository.save(pathLink);

        PathLink actualPathLink = pathLinkRepository.findOne(pathLink.getId());

        assertThat(actualPathLink.getFrom()).isNotNull();
    }

    @Test
    public void pathLinkBetweenQuays() {
        Quay quay1 = new Quay();
        Quay quay2 = new Quay();
        quayRepository.save(quay1);
        quayRepository.save(quay2);


        PathLinkEnd from = new PathLinkEnd(quay1);
        PathLinkEnd to = new PathLinkEnd(quay2);

        PathLink pathLink = new PathLink(from, to);
        pathLinkRepository.save(pathLink);

        PathLink actualPathLink = pathLinkRepository.findOne(pathLink.getId());

        assertThat(actualPathLink.getFrom().getQuay().getId()).isEqualTo(quay1.getId());
        assertThat(actualPathLink.getTo().getQuay().getId()).isEqualTo(quay2.getId());
    }

    @Test
    public void pathLinkWithQuaysAndPathJunction() {
        Quay fromQuay = new Quay();
        Quay toQuay = new Quay();
        fromQuay = quayRepository.save(fromQuay);
        toQuay = quayRepository.save(toQuay);

        PathJunction pathJunction = new PathJunction();
        pathJunction = pathJunctionRepository.save(pathJunction);

        PathLinkEnd pathLinkEndFromQuay = new PathLinkEnd(fromQuay);
        PathLinkEnd pathLinkEndToPathJunction = new PathLinkEnd(pathJunction);
        PathLinkEnd pathLinkEndFromPathJunction = new PathLinkEnd(pathJunction);
        PathLinkEnd pathLinkEndToQuay = new PathLinkEnd(toQuay);

        PathLink pathLinkToPathJunction = new PathLink(pathLinkEndFromQuay, pathLinkEndToPathJunction);
        PathLink pathLinkToQuay = new PathLink(pathLinkEndFromPathJunction, pathLinkEndToQuay);

        pathLinkRepository.save(pathLinkToPathJunction);
        pathLinkRepository.save(pathLinkToQuay);

        PathLink actualPathLinkToPathJunction = pathLinkRepository.findOne(pathLinkToPathJunction.getId());
        PathLink actualPathLinkToQuay = pathLinkRepository.findOne(pathLinkToQuay.getId());

        assertThat(actualPathLinkToPathJunction.getFrom().getQuay().getId()).isEqualTo(fromQuay.getId());
        assertThat(actualPathLinkToPathJunction.getTo().getPathJunction().getId()).isEqualTo(pathJunction.getId());

        assertThat(actualPathLinkToQuay.getFrom().getPathJunction().getId()).isEqualTo(pathJunction.getId());
        assertThat(actualPathLinkToQuay.getTo().getQuay().getId()).isEqualTo(pathLinkToQuay.getTo().getQuay().getId());
    }

    @Test
    public void pathLinkWithLineString() {

        Coordinate[] coordinates = new Coordinate[2];
        coordinates[0] = new Coordinate(11, 60);
        coordinates[1] = new Coordinate(11.1, 60.1);

        CoordinateSequence points = new CoordinateArraySequence(coordinates);

        LineString lineString = new LineString(points, geometryFactory);

        PathLink pathLink = new PathLink();
        pathLink.setLineString(lineString);

        pathLinkRepository.save(pathLink);

        PathLink actual = pathLinkRepository.findOne(pathLink.getId());
        assertThat(actual.getLineString()).isNotNull();
        assertThat(actual.getLineString().getCoordinates()).isEqualTo(coordinates);

    }

    @Test
    public void pathLinkWithTransferDuration() {

        PathLink pathLink = new PathLink();

        TransferDuration transferDuration = new TransferDuration();
        transferDuration.setDefaultDuration(Duration.ofMillis(10000));
        transferDuration.setOccasionalTravellerDuration(Duration.ofMillis(20000));
        transferDuration.setMobilityRestrictedTravellerDuration(Duration.ofMillis(30000));
        transferDuration.setFrequentTravellerDuration(Duration.ofMillis(5000));

        pathLink.setTransferDuration(transferDuration);

        pathLinkRepository.save(pathLink);

        PathLink actual = pathLinkRepository.findOne(pathLink.getId());


        assertThat(actual.getTransferDuration()).isNotNull();
        assertThat(actual.getTransferDuration().getDefaultDuration()).isEqualTo(transferDuration.getDefaultDuration());
        assertThat(actual.getTransferDuration().getFrequentTravellerDuration()).isEqualTo(transferDuration.getFrequentTravellerDuration());
        assertThat(actual.getTransferDuration().getOccasionalTravellerDuration()).isEqualTo(transferDuration.getOccasionalTravellerDuration());
        assertThat(actual.getTransferDuration().getMobilityRestrictedTravellerDuration()).isEqualTo(transferDuration.getMobilityRestrictedTravellerDuration());
    }

    @Test
    public void testKeyValueStructure() throws Exception {
        PathLink pathLink = new PathLink();
        List<String> ids = Arrays.asList("OPP:PathLink:123123", "TEL:PathLink:3251321");
        Value value = new Value(ids);
        pathLink.getKeyValues().put("ORIGINAL_ID", value);

        pathLinkRepository.save(pathLink);
        PathLink actual = pathLinkRepository.findOne(pathLink.getId());

        assertThat(actual.getKeyValues().get("ORIGINAL_ID").getItems().containsAll(ids));
    }

    private StopPlace createAndSaveStop(String name) {
        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString(name));
        stopPlaceRepository.save(stopPlace);
        return stopPlace;
    }
}