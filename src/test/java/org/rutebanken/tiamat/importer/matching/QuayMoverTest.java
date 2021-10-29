package org.rutebanken.tiamat.importer.matching;

import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.importer.merging.MergingStopPlaceImporter;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.stopplace.StopPlaceQuayMover;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class QuayMoverTest extends TiamatIntegrationTest {

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private StopPlaceQuayMover stopPlaceQuayMover;

    @Autowired
    private VersionCreator versionCreator;

    @Autowired
    private MergingStopPlaceImporter mergingStopPlaceImporter;

    private QuayMover quayMover = new QuayMover();

    @Test
    public void moveQuayWithOneImportedIdToExistingStopPlace() throws ExecutionException {
        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.setVersion(1L);
        fromStopPlace.getOriginalIds().add("ORGA1:Stopplace:1");

        Quay quayToMove = new Quay(new EmbeddableMultilingualString("Quay to move"));
        quayToMove.setVersion(1L);
        quayToMove.getOriginalIds().add("ORGA1:Quay:1");

        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace = stopPlaceRepository.save(fromStopPlace);


        StopPlace incomingStopPlace = new StopPlace();
        incomingStopPlace.getQuays().add(quayToMove);
        incomingStopPlace.getOriginalIds().add("ORGA1:Stopplace:2");

        StopPlace targetStopPlace = new StopPlace();
        targetStopPlace.setVersion(1L);
        targetStopPlace.getOriginalIds().add("ORGA1:Stopplace:2");
        targetStopPlace = stopPlaceRepository.save(targetStopPlace);

        List<StopPlace> foundStopPlaces = new ArrayList<>();
        foundStopPlaces.add(fromStopPlace);
        foundStopPlaces.add(targetStopPlace);

        ReflectionTestUtils.setField(quayMover, "stopPlaceQuayMover", stopPlaceQuayMover);
        quayMover.doMove(foundStopPlaces, incomingStopPlace);

        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        targetStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(targetStopPlace.getNetexId());

        assertThat(fromStopPlace.getQuays()).isEmpty();
        assertThat(targetStopPlace.getQuays()).hasSize(1);
    }

    @Test
    public void moveSharedQuayWithTwoImportedIdToExistingStopPlace() throws ExecutionException {
        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.setVersion(1L);
        fromStopPlace.getOriginalIds().add("ORGA1:Stopplace:1");

        Quay quayToMove = new Quay(new EmbeddableMultilingualString("Quay to move"));
        quayToMove.setVersion(1L);
        quayToMove.getOriginalIds().add("ORGA1:Quay:1");
        quayToMove.getOriginalIds().add("ORGA2:Quay:1");
        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace = stopPlaceRepository.save(fromStopPlace);

        Quay incomingQuay = new Quay(new EmbeddableMultilingualString("Incoming Quay"));
        incomingQuay.getOriginalIds().add("ORGA1:Quay:1");

        StopPlace incomingStopPlace = new StopPlace();
        incomingStopPlace.getOriginalIds().add("ORGA1:Stopplace:2");
        incomingStopPlace.getQuays().add(incomingQuay);


        StopPlace targetStopPlace = new StopPlace();
        targetStopPlace.setVersion(1L);
        targetStopPlace.getOriginalIds().add("ORGA1:Stopplace:2");
        targetStopPlace = stopPlaceRepository.save(targetStopPlace);

        List<StopPlace> foundStopPlaces = new ArrayList<>();
        foundStopPlaces.add(fromStopPlace);
        foundStopPlaces.add(targetStopPlace);

        ReflectionTestUtils.setField(quayMover, "stopPlaceQuayMover", stopPlaceQuayMover);
        ReflectionTestUtils.setField(quayMover, "versionCreator", versionCreator);
        ReflectionTestUtils.setField(quayMover, "quayRepository", quayRepository);
        quayMover.doMove(foundStopPlaces, incomingStopPlace);

        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        targetStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(targetStopPlace.getNetexId());

        assertThat(fromStopPlace.getQuays()).hasSize(1);
        assertThat(targetStopPlace.getQuays()).hasSize(1);

        assertThat(fromStopPlace.getQuays().stream().findFirst().get().getOriginalIds().contains("ORGA2:Quay:1")).isTrue();
        assertThat(targetStopPlace.getQuays().stream().findFirst().get().getOriginalIds().contains("ORGA1:Quay:1")).isTrue();
    }

    @Test
    public void moveQuayWithOneImportedIdToNewStopPlace() throws ExecutionException {
        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.setVersion(1L);
        fromStopPlace.getOriginalIds().add("ORGA1:Stopplace:1");

        Quay quayToMove = new Quay(new EmbeddableMultilingualString("Quay to move"));
        quayToMove.setVersion(1L);
        quayToMove.getOriginalIds().add("ORGA1:Quay:1");

        fromStopPlace.getQuays().add(quayToMove);
        fromStopPlace = stopPlaceRepository.save(fromStopPlace);


        StopPlace incomingStopPlace = new StopPlace();
        incomingStopPlace.getQuays().add(quayToMove);
        incomingStopPlace.getOriginalIds().add("ORGA1:Stopplace:2");

        List<StopPlace> foundStopPlaces = new ArrayList<>();
        foundStopPlaces.add(fromStopPlace);

        ReflectionTestUtils.setField(quayMover, "stopPlaceQuayMover", stopPlaceQuayMover);
        ReflectionTestUtils.setField(quayMover, "versionCreator", versionCreator);
        ReflectionTestUtils.setField(quayMover, "mergingStopPlaceImporter", mergingStopPlaceImporter);
        quayMover.doMove(foundStopPlaces, incomingStopPlace);

        fromStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(fromStopPlace.getNetexId());
        List<String> stopPlaceNetexIds = stopPlaceRepository.findStopPlaceFromQuayOriginalId("ORGA1:Quay:1", Instant.now());
        StopPlace destinationStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(stopPlaceNetexIds.get(0));

        assertThat(fromStopPlace.getQuays()).isEmpty();
        assertThat(destinationStopPlace.getQuays()).hasSize(1);
        assertThat(destinationStopPlace.getQuays().stream().findFirst().get().getOriginalIds().contains("ORGA1:Quay:1")).isTrue();
    }
}
