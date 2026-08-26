package org.rutebanken.tiamat.rest.merge;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.dtoassembling.dto.JobDto;
import org.rutebanken.tiamat.dtoassembling.dto.MergeMode;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeCandidatePageDto;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeCandidatePairDto;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeRequestDto;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.stopplace.StopPlaceMergeJobService;
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
    private final StopPlaceMergeJobService stopPlaceMergeJobService = mock(StopPlaceMergeJobService.class);
    private final MergeableStopPlacesResource resource = new MergeableStopPlacesResource(stopPlaceRepository, stopPlaceMergeJobService);

    @Test
    void returnsPairsForValidMode() {
        Object[] row = new Object[] {
                "NSR:StopPlace:1", "Gare A", 10.5, 59.9, "bus", "prov1",
                "NSR:StopPlace:2", "Gare B", 10.50001, 59.90001, "bus", "prov1"
        };
        Page<StopPlaceMergeCandidatePairDto> page = new PageImpl<>(List.of(new StopPlaceMergeCandidatePairDto(row)), PageRequest.of(0, 100), 1);
        when(stopPlaceRepository.findMergeableStopPlaces(eq(MergeMode.SAME_PROVIDER), isNull(), any(Pageable.class))).thenReturn(page);

        StopPlaceMergeCandidatePageDto body = resource.getMergeableStopPlaces("SAME_PROVIDER", null, 0, 100);

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

        StopPlaceMergeCandidatePageDto body = resource.getMergeableStopPlaces("MULTI_PROVIDER", null, 0, 100);

        assertThat(body.content).isEmpty();
        assertThat(body.hasMore).isFalse();
    }

    @Test
    void passesProviderThroughForSameProviderMode() {
        when(stopPlaceRepository.findMergeableStopPlaces(eq(MergeMode.SAME_PROVIDER), eq("prov1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        resource.getMergeableStopPlaces("SAME_PROVIDER", "prov1", 0, 100);

        verify(stopPlaceRepository).findMergeableStopPlaces(MergeMode.SAME_PROVIDER, "prov1", PageRequest.of(0, 100));
    }

    @Test
    void passesProviderThroughForMultiProviderMode() {
        when(stopPlaceRepository.findMergeableStopPlaces(eq(MergeMode.MULTI_PROVIDER), eq("prov1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        resource.getMergeableStopPlaces("MULTI_PROVIDER", "prov1", 0, 100);

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

    @Test
    void triggerMergeDelegatesToService() {
        StopPlaceMergeRequestDto couple = new StopPlaceMergeRequestDto();
        couple.setTarget("PROV:StopPlace:1");
        couple.setOrigin("PROV:StopPlace:2");
        Job job = new Job(JobStatus.PROCESSING);
        job.setId(1L);
        job.setType(JobType.STOP_PLACE_MERGE);
        job.setTotalCount(1);
        job.setRemainingCount(1);
        when(stopPlaceMergeJobService.triggerMerge(List.of(couple))).thenReturn(job);

        JobDto result = resource.triggerMerge(List.of(couple));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(JobStatus.PROCESSING);
        assertThat(result.getType()).isEqualTo(JobType.STOP_PLACE_MERGE);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRemainingCount()).isEqualTo(1);
    }

    @Test
    void getMergeJobProgressDelegatesToService() {
        Job job = new Job(JobStatus.FINISHED);
        job.setId(7L);
        job.setType(JobType.STOP_PLACE_MERGE);
        job.setTotalCount(3);
        job.setRemainingCount(0);
        when(stopPlaceMergeJobService.getMergeJobProgress(7L)).thenReturn(job);

        JobDto result = resource.getMergeJobProgress(7L);

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getStatus()).isEqualTo(JobStatus.FINISHED);
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getRemainingCount()).isZero();
    }
}
