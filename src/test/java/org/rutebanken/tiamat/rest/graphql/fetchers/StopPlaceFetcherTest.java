package org.rutebanken.tiamat.rest.graphql.fetchers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.tag.Tag;
import org.rutebanken.tiamat.repository.TagRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StopPlaceFetcherTest {

    private static final String STOP_PLACE_IDENTIFIER_PATTERN = "MOBIITI:StopPlace:";

    private static final String STOP_PLACE_IDENTIFIER_1 = "MOBIITI:StopPlace:169980";

    private static final String STOP_PLACE_IDENTIFIER_2 = "MOBIITI:StopPlace:169982";

    private static final String NAME = "name";

    @InjectMocks
    private StopPlaceFetcher stopPlaceFetcher;

    @Mock
    private TagRepository tagRepository;

    @Captor
    private ArgumentCaptor<Set<String>> stopPlaceIdentifierCaptor;

    @Test
    void fetchTags_emptyStopPlaceCollection_test() {
        stopPlaceFetcher.fetchTags(null);

        verify(tagRepository, never()).findAllByIdReference(anySet());
    }

    @Test
    void fetchTags_someStopPlace_test() {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setNetexId(STOP_PLACE_IDENTIFIER_1);
        StopPlace stopPlace2 = new StopPlace();
        stopPlace2.setNetexId(STOP_PLACE_IDENTIFIER_2);
        Tag tag1 = buildTag(STOP_PLACE_IDENTIFIER_1, NAME);
        Tag tag2 = buildTag(STOP_PLACE_IDENTIFIER_2, NAME);
        when(tagRepository.findAllByIdReference(anySet())).thenReturn(List.of(tag1, tag2));

        stopPlaceFetcher.fetchTags(List.of(stopPlace, stopPlace2));

        verify(tagRepository).findAllByIdReference(stopPlaceIdentifierCaptor.capture());
        assertThat(stopPlaceIdentifierCaptor.getValue())
                .isNotNull().isEmpty();
        assertThat(stopPlace.getTags()).hasSize(1).containsExactlyInAnyOrder(tag1);
        assertThat(stopPlace2.getTags()).hasSize(1).containsExactlyInAnyOrder(tag2);
    }

    @Test
    void fetchTags_moreThan1000StopPlace_test() {
        List<StopPlace> stopPlaceList = new ArrayList<>(1002);
        for (int i = 0; i < 1002; i++) {
            StopPlace stopPlace = new StopPlace();
            stopPlace.setNetexId(STOP_PLACE_IDENTIFIER_PATTERN + i);
            stopPlaceList.add(stopPlace);
        }

        Tag tag0 = buildTag(STOP_PLACE_IDENTIFIER_PATTERN + "0", NAME);
        Tag tag1 = buildTag(STOP_PLACE_IDENTIFIER_PATTERN + "1000", NAME);
        Tag tag2 = buildTag(STOP_PLACE_IDENTIFIER_PATTERN + "1001", NAME);
        when(tagRepository.findAllByIdReference(anySet()))
                .thenReturn(List.of(tag0))
                .thenReturn(List.of(tag1, tag2));

        stopPlaceFetcher.fetchTags(stopPlaceList);

        verify(tagRepository, times(2)).findAllByIdReference(stopPlaceIdentifierCaptor.capture());
        assertThat(stopPlaceIdentifierCaptor.getValue())
                .isNotNull().isEmpty();
        assertThat(stopPlaceList.get(0).getTags()).hasSize(1).containsExactlyInAnyOrder(tag0);
        assertThat(stopPlaceList.get(1000).getTags()).hasSize(1).containsExactlyInAnyOrder(tag1);
        assertThat(stopPlaceList.get(1001).getTags()).hasSize(1).containsExactlyInAnyOrder(tag2);
    }

    private static Tag buildTag(String identifier, String name) {
        Tag tag1 = new Tag();
        tag1.setName(name);
        tag1.setIdreference(identifier);
        return tag1;
    }
}