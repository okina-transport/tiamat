package org.rutebanken.tiamat.service.stopplace;

import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

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
        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("test", DEFAULT_LANG));
        stopPlace.setNetexId("Foo:StopPlace:" + 0);
        stopPlace.setVersion(0);
        stopPlace.setParentStopPlace(false);
        stopPlaceRepository.save(stopPlace);

        StopPlace stopPlace1 = new StopPlace(new EmbeddableMultilingualString("test", DEFAULT_LANG));
        stopPlace1.setNetexId("Foo:StopPlace:" + 0);
        stopPlace1.setVersion(1);
        stopPlace1.setParentStopPlace(false);
        stopPlaceRepository.save(stopPlace1);

        StopPlace stopPlace2 = new StopPlace(new EmbeddableMultilingualString("already alternative name", DEFAULT_LANG));
        stopPlace2.setNetexId("Foo:StopPlace:" + 1);
        stopPlace2.setVersion(0);
        stopPlace2.setParentStopPlace(false);
        AlternativeName otherAlternativeName = new AlternativeName();
        otherAlternativeName.setName(new EmbeddableMultilingualString("bobby", "fr"));
        otherAlternativeName.setNameType(NameTypeEnumeration.OTHER);
        stopPlace2.getAlternativeNames().add(otherAlternativeName);
        stopPlaceRepository.save(stopPlace2);

        StopPlace stopPlace3 = new StopPlace(new EmbeddableMultilingualString("Done", DEFAULT_LANG));
        stopPlace3.setNetexId("Foo:StopPlace:" + 2);
        stopPlace3.setVersion(0);
        stopPlace3.setParentStopPlace(false);
        stopPlaceRepository.save(stopPlace3);

        StopPlace parentStopPlace = new StopPlace(new EmbeddableMultilingualString("parentTest", DEFAULT_LANG));
        parentStopPlace.setNetexId("Foo:StopPlace:" + 3);
        parentStopPlace.setVersion(0);
        parentStopPlace.setParentStopPlace(true);

        StopPlace childStopPlace = new StopPlace(new EmbeddableMultilingualString("childTest", DEFAULT_LANG));
        childStopPlace.setNetexId("Foo:StopPlace:" + 4);
        childStopPlace.setVersion(0);
        childStopPlace.setParentStopPlace(false);
        childStopPlace.setParentSiteRef(new SiteRefStructure(parentStopPlace.getNetexId()));


        Set<StopPlace> listChildStopPlace = new HashSet<>();
        listChildStopPlace.add(childStopPlace);
        parentStopPlace.setChildren(listChildStopPlace);
        stopPlaceRepository.save(parentStopPlace);
        stopPlaceRepository.save(childStopPlace);


        // WHEN
        Set<StopPlace> renamedStopPlaces = stopPlaceRenamer.checkAllAndRename(true);

        // THEN
        List<String> lan = new ArrayList<>();
        Iterator rsp = renamedStopPlaces.iterator();
        while (rsp.hasNext()){
            StopPlace sp = (StopPlace) rsp.next();
            lan.add(sp.getAlternativeNames().get(0).getName().getValue());
        }
        assertThat(renamedStopPlaces.size()).isEqualTo(3);
        assertThat(lan.contains("bobby")).isEqualTo(true);
        assertThat(lan.contains("test")).isEqualTo(true);
        assertThat(lan.contains("childTest")).isEqualTo(true);
        assertThat(lan.contains("parentTest")).isEqualTo(false);
    }


    @Test
    public void testRenamingRules() {
        // GIVEN
        createAndSaveStopPlace("Collége");

        // WHEN
        List<StopPlace> allStopPlaces = stopPlaceRepository.findAll();
        allStopPlaces.forEach(stopPlace -> {
            stopPlace.setName(new EmbeddableMultilingualString(stopPlaceRenamer.renameIfNeeded(stopPlace.getName().getValue()), DEFAULT_LANG));
        });

        // THEN
        assertThat(allStopPlaces.get(0).getName().getValue()).isEqualTo("Collège");
    }


    private StopPlace createAndSaveStopPlace(String name) {
        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString(name, DEFAULT_LANG));
        stopPlace.setNetexId("Foo:StopPlace:" + counter++);
        return stopPlaceRepository.save(stopPlace);
    }

}