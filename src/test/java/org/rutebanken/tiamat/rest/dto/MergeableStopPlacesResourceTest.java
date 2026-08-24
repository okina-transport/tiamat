package org.rutebanken.tiamat.rest.dto;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.dtoassembling.dto.MergeMode;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeCandidatePageDto;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeCandidatePairDto;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MergeableStopPlacesResourceTest {

    private final StopPlaceRepository stopPlaceRepository = mock(StopPlaceRepository.class);
    private final MergeableStopPlacesResource resource = new MergeableStopPlacesResource(stopPlaceRepository);

    @Test
    void returnsPairsForValidMode() {
        Object[] row = new Object[] {
                "NSR:StopPlace:1", "Gare A", 10.5, 59.9, "bus", "prov1",
                "NSR:StopPlace:2", "Gare B", 10.50001, 59.90001, "bus", "prov1"
        };
        Page<StopPlaceMergeCandidatePairDto> page = new PageImpl<>(List.of(new StopPlaceMergeCandidatePairDto(row)), PageRequest.of(0, 100), 1);
        when(stopPlaceRepository.findMergeableStopPlaces(eq(MergeMode.SAME_PROVIDER), isNull(), any(Pageable.class))).thenReturn(page);

        Response response = resource.getMergeableStopPlaces("SAME_PROVIDER", null, 0, 100);

        assertThat(response.getStatus()).isEqualTo(200);
        StopPlaceMergeCandidatePageDto body = (StopPlaceMergeCandidatePageDto) response.getEntity();
        assertThat(body.content).hasSize(1);
        assertThat(body.content.getFirst().getBase().getNetexId()).isEqualTo("NSR:StopPlace:1");
    }

    @Test
    void rejectsMissingMode() {
        assertThatThrownBy(() -> resource.getMergeableStopPlaces(null, null, 0, 100))
                .isInstanceOf(WebApplicationException.class)
                .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400));
    }

    @Test
    void rejectsInvalidMode() {
        assertThatThrownBy(() -> resource.getMergeableStopPlaces("NOT_A_MODE", null, 0, 100))
                .isInstanceOf(WebApplicationException.class)
                .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400));
    }

    @Test
    void emptyResultReturns200WithEmptyArray() {
        when(stopPlaceRepository.findMergeableStopPlaces(eq(MergeMode.MULTI_PROVIDER), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        Response response = resource.getMergeableStopPlaces("MULTI_PROVIDER", null, 0, 100);

        assertThat(response.getStatus()).isEqualTo(200);
        StopPlaceMergeCandidatePageDto body = (StopPlaceMergeCandidatePageDto) response.getEntity();
        assertThat(body.content).isEmpty();
        assertThat(body.hasMore).isFalse();
    }

    @Test
    void passesProviderThroughForSameProviderMode() {
        when(stopPlaceRepository.findMergeableStopPlaces(eq(MergeMode.SAME_PROVIDER), eq("prov1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        Response response = resource.getMergeableStopPlaces("SAME_PROVIDER", "prov1", 0, 100);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(stopPlaceRepository).findMergeableStopPlaces(MergeMode.SAME_PROVIDER, "prov1", PageRequest.of(0, 100));
    }

    @Test
    void passesProviderThroughForMultiProviderMode() {
        when(stopPlaceRepository.findMergeableStopPlaces(eq(MergeMode.MULTI_PROVIDER), eq("prov1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        Response response = resource.getMergeableStopPlaces("MULTI_PROVIDER", "prov1", 0, 100);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(stopPlaceRepository).findMergeableStopPlaces(MergeMode.MULTI_PROVIDER, "prov1", PageRequest.of(0, 100));
    }

    @Test
    void omittedProviderKeepsExistingBehaviorForBothModes() {
        when(stopPlaceRepository.findMergeableStopPlaces(any(MergeMode.class), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        resource.getMergeableStopPlaces("SAME_PROVIDER", null, 0, 100);
        resource.getMergeableStopPlaces("MULTI_PROVIDER", null, 0, 100);

        verify(stopPlaceRepository).findMergeableStopPlaces(MergeMode.SAME_PROVIDER, null, PageRequest.of(0, 100));
        verify(stopPlaceRepository).findMergeableStopPlaces(MergeMode.MULTI_PROVIDER, null, PageRequest.of(0, 100));
    }
}
