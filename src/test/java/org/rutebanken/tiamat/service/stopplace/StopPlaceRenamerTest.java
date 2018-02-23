package org.rutebanken.tiamat.service.stopplace;

import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class StopPlaceRenamerTest extends TiamatIntegrationTest {

    private static final String DEFAULT_LANG = "fr";
    private int counter;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private StopPlaceRenamer stopPlaceRenamer;

    @Test
    public void healthTest() {
        // GIVEN
        StopPlace stopPlace = createAndSaveStopPlace("bar");

        // WHEN
        List<StopPlace> all = stopPlaceRepository.findAll();

        // THEN
        assertThat(all.size()).isEqualTo(1);
        assertThat(all.get(0)).isEqualTo(stopPlace);
    }

    @Test
    public void onlyLastVersionOfStopPlaceIsRenamed() {
        // GIVEN

        // TODO : créer deux versions d'un stop place à renommer.

        // WHEN
        List<StopPlace> renamedStopPlaces = stopPlaceRenamer.checkAllAndRename(true);

        // THEN
        assertThat(renamedStopPlaces.size()).isOne();
    }


    @Test
    public void testRenamingRules() {
        // GIVEN
        // TODO : créer une liste de points à renommer et vérifier l'application des règles.

        // WHEN


        // THEN
        fail("Not implemented yet");
    }


    private StopPlace createAndSaveStopPlace(String name) {
        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString(name, DEFAULT_LANG));
        stopPlace.setNetexId("Foo:StopPlace:" + counter++);
        return stopPlaceRepository.save(stopPlace);
    }

}