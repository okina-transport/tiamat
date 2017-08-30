package org.rutebanken.tiamat.model;

import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.repository.GroupOfStopPlacesRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;


public class GroupOfStopPlacesTest extends TiamatIntegrationTest {


    @Test
    public void addStopPlacesToGroupOfStopPlaces() {

        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("Stop place 1"));
        stopPlace.setVersion(1L);
        stopPlace = stopPlaceRepository.save(stopPlace);

        StopPlace stopPlace2 = new StopPlace(new EmbeddableMultilingualString("Stop place 2"));
        stopPlace2.setVersion(1L);
        stopPlace2 = stopPlaceRepository.save(stopPlace2);


        String groupName = "group of stop places";
        GroupOfStopPlaces groupOfStopPlaces = new GroupOfStopPlaces(new EmbeddableMultilingualString(groupName));

        groupOfStopPlaces.getMembers().add(stopPlace);
        groupOfStopPlaces.getMembers().add(stopPlace2);

        groupOfStopPlaces = groupOfStopPlacesRepository.save(groupOfStopPlaces);

        assertThat(groupOfStopPlaces.getName().getValue()).isEqualTo(groupName);

        assertThat(groupOfStopPlaces.getMembers()).extracting(StopPlace::getNetexId).contains(stopPlace.getNetexId(), stopPlace2.getNetexId());

    }

    @Test
    public void stopPlaceCouldBelongToMultipleGroups() {

        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("Stop place 1"));
        stopPlace.setVersion(1L);
        stopPlace = stopPlaceRepository.save(stopPlace);

        String groupName = "group of stop places 1";
        GroupOfStopPlaces groupOfStopPlaces = new GroupOfStopPlaces(new EmbeddableMultilingualString(groupName));
        groupOfStopPlaces.getMembers().add(stopPlace);
        groupOfStopPlaces = groupOfStopPlacesRepository.save(groupOfStopPlaces);

        String groupName2 = "group of stop places 2";
        GroupOfStopPlaces groupOfStopPlaces2 = new GroupOfStopPlaces(new EmbeddableMultilingualString(groupName2));
        groupOfStopPlaces2.getMembers().add(stopPlace);
        groupOfStopPlaces2 = groupOfStopPlacesRepository.save(groupOfStopPlaces2);

        assertThat(groupOfStopPlaces.getMembers()).extracting(StopPlace::getNetexId).contains(stopPlace.getNetexId());
        assertThat(groupOfStopPlaces2.getMembers()).extracting(StopPlace::getNetexId).contains(stopPlace.getNetexId());
    }

}