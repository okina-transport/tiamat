package org.rutebanken.tiamat.service.stopplace;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeRequestDto;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StopPlaceMergeJobServiceTest {

    private final JobRepository jobRepository = mock(JobRepository.class);
    private final StopPlaceMerger stopPlaceMerger = mock(StopPlaceMerger.class);
    private final UsernameFetcher usernameFetcher = mock(UsernameFetcher.class);
    private final StopPlaceMergeJobService service = new StopPlaceMergeJobService(jobRepository, stopPlaceMerger, usernameFetcher);

    @Test
    void triggerMergeRejectsEmptyBody() {
        assertThatThrownBy(() -> service.triggerMerge(List.of()))
                .isInstanceOf(WebApplicationException.class)
                .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400));
    }

    @Test
    void triggerMergeRejectsCoupleWithSameTargetAndOrigin() {
        StopPlaceMergeRequestDto couple = new StopPlaceMergeRequestDto();
        couple.setTarget("PROV:StopPlace:1");
        couple.setOrigin("PROV:StopPlace:1");

        Throwable thrown = catchThrowable(() -> service.triggerMerge(List.of(couple)));
        assertThat(thrown)
                .isInstanceOf(WebApplicationException.class)
                .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(400));
    }

    @Test
    void triggerMergeRejectsWhenJobAlreadyRunning() {
        StopPlaceMergeRequestDto couple = new StopPlaceMergeRequestDto();
        couple.setTarget("PROV:StopPlace:1");
        couple.setOrigin("PROV:StopPlace:2");

        when(jobRepository.findByTypeAndStatus(JobType.STOP_PLACE_MERGE, JobStatus.PROCESSING))
                .thenReturn(List.of(new Job(JobStatus.PROCESSING)));

        Throwable thrown = catchThrowable(() -> service.triggerMerge(List.of(couple)));
        assertThat(thrown)
                .isInstanceOf(WebApplicationException.class)
                .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(409));
    }

    @Test
    void triggerMergeCreatesProcessingJobAndRunsToCompletion() {
        StopPlaceMergeRequestDto couple = new StopPlaceMergeRequestDto();
        couple.setTarget("PROV:StopPlace:1");
        couple.setOrigin("PROV:StopPlace:2");

        when(jobRepository.findByTypeAndStatus(JobType.STOP_PLACE_MERGE, JobStatus.PROCESSING)).thenReturn(List.of());
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(1L);
            }
            return job;
        });
        when(usernameFetcher.getUserNameForAuthenticatedUser()).thenReturn("bob");
        when(stopPlaceMerger.mergeStopPlaces(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(new StopPlace());

        Job createdJob = service.triggerMerge(List.of(couple));

        assertThat(createdJob.getType()).isEqualTo(JobType.STOP_PLACE_MERGE);
        assertThat(createdJob.getTotalCount()).isEqualTo(1);
        assertThat(createdJob.getUserName()).isEqualTo("bob");

        await().untilAsserted(() -> assertThat(createdJob.getStatus()).isEqualTo(JobStatus.FINISHED));
        assertThat(createdJob.getRemainingCount()).isZero();
        verify(stopPlaceMerger)
                .mergeStopPlaces(eq("PROV:StopPlace:2"), eq("PROV:StopPlace:1"), anyString(), anyString(), eq(false));
    }

    @Test
    void getMergeJobProgressThrowsNotFoundForUnknownOrWrongTypeJob() {
        when(jobRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMergeJobProgress(42L))
                .isInstanceOf(WebApplicationException.class)
                .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    void getMergeJobProgressReturnsJob() {
        Job job = new Job(JobStatus.PROCESSING);
        job.setId(7L);
        job.setType(JobType.STOP_PLACE_MERGE);
        job.setTotalCount(3);
        job.setRemainingCount(2);
        when(jobRepository.findById(7L)).thenReturn(Optional.of(job));

        Job result = service.getMergeJobProgress(7L);

        assertThat(result.getRemainingCount()).isEqualTo(2);
        assertThat(result.getTotalCount()).isEqualTo(3);
    }
}
