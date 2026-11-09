package org.rutebanken.tiamat.service.job;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class JobPurgeServiceTest extends TiamatIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobPurgeService jobPurgeService;

    @Value("${async.export.path}")
    private String localExportPath;

    private String subFolder;
    private Path providerDir;

    @BeforeEach
    public void setUp() throws IOException {
        jobRepository.deleteAll();
        subFolder = "purge-" + UUID.randomUUID().toString().substring(0, 8);
        providerDir = Path.of(localExportPath, subFolder);
        Files.createDirectories(providerDir);
        reset(blobStoreService);
        when(blobStoreService.delete(anyString())).thenReturn(true);
    }

    @AfterEach
    public void tearDown() throws IOException {
        jobRepository.deleteAll();
        FileUtils.deleteDirectory(providerDir.toFile());
    }

    @Test
    public void purgeKeepsMostRecentJobsAndJobsYoungerThanKeepDays() throws IOException {
        Instant now = Instant.now();
        // 5 recent + 115 old export jobs: the 100 most recent are kept, the 20 oldest are deleted
        List<Job> recentJobs = createJobs(5, JobType.NETEX_STOP_PLACE_QUAY, JobStatus.FINISHED, now.minus(Duration.ofDays(10)), "recent");
        List<Job> oldJobs = createJobs(115, JobType.NETEX_STOP_PLACE_QUAY, JobStatus.FINISHED, now.minus(Duration.ofDays(200)), "old");
        List<Job> expectedDeleted = oldJobs.subList(95, 115);

        // Another type in the same folder: outside the 100 most recent but younger than keepDays, all kept
        List<Job> youngJobs = createJobs(105, JobType.NETEX_POI, JobStatus.FAILED, now.minus(Duration.ofDays(50)), "young");

        // Old processing jobs are never deleted
        List<Job> processingJobs = createJobs(3, JobType.NETEX_STOP_PLACE_QUAY, JobStatus.PROCESSING, now.minus(Duration.ofDays(300)), "processing");

        // Mapping CSV in the same folder, not referenced by any job
        Path mappingCsv = Files.writeString(providerDir.resolve("quayIdMappings.csv"), "mapping");

        JobPurgeResult result = jobPurgeService.purgeCompletedJobs(100, 100);

        assertThat(result.deletedJobs()).isEqualTo(20);
        assertThat(result.deletedLocalFiles()).isEqualTo(20);
        assertThat(result.deletedBlobs()).isEqualTo(20);
        assertThat(result.failedJobs()).isZero();

        Set<Long> remainingIds = Set.copyOf(jobRepository.findAll().stream().map(Job::getId).toList());
        assertThat(remainingIds).hasSize(5 + 115 - 20 + 105 + 3);
        expectedDeleted.forEach(job -> {
            assertThat(remainingIds).doesNotContain(job.getId());
            assertThat(providerDir.resolve(job.getFileName())).doesNotExist();
            verify(blobStoreService).delete(subFolder + "/" + job.getFileName());
        });
        oldJobs.subList(0, 95).forEach(job -> assertKept(remainingIds, job));
        recentJobs.forEach(job -> assertKept(remainingIds, job));
        youngJobs.forEach(job -> assertKept(remainingIds, job));
        processingJobs.forEach(job -> assertKept(remainingIds, job));

        assertThat(mappingCsv).exists();
        verify(blobStoreService, never()).delete(subFolder + "/quayIdMappings.csv");
    }

    @Test
    public void purgeDoesNotDeleteFilesOfImportJobsOrFilesStillReferenced() throws IOException {
        Instant old = Instant.now().minus(Duration.ofDays(200));
        Job importJob = createJob(JobType.NETEX_STOP_PLACE_QUAY, JobAction.IMPORT, JobStatus.FINISHED, old, "import.zip");
        Job exportJob = createJob(JobType.NETEX_POI, JobAction.EXPORT, JobStatus.FINISHED, old, "shared.zip");
        Job keptJobWithSameFile = createJob(JobType.NETEX_POI, JobAction.EXPORT, JobStatus.PROCESSING, old, "shared.zip");

        JobPurgeResult result = jobPurgeService.purgeCompletedJobs(0, 100);

        assertThat(result.deletedJobs()).isEqualTo(2);
        assertThat(result.deletedLocalFiles()).isZero();
        assertThat(jobRepository.findById(importJob.getId())).isEmpty();
        assertThat(jobRepository.findById(exportJob.getId())).isEmpty();
        assertThat(jobRepository.findById(keptJobWithSameFile.getId())).isPresent();
        assertThat(providerDir.resolve("import.zip")).exists();
        assertThat(providerDir.resolve("shared.zip")).exists();
        verify(blobStoreService, never()).delete(anyString());
    }

    @Test
    public void jobIsKeptWhenBlobDeletionFails() throws IOException {
        Instant old = Instant.now().minus(Duration.ofDays(200));
        Job job = createJob(JobType.NETEX_PARKING, JobAction.EXPORT, JobStatus.FINISHED, old, "parking.zip");
        when(blobStoreService.delete(anyString())).thenThrow(new RuntimeException("blob store unavailable"));

        JobPurgeResult result = jobPurgeService.purgeCompletedJobs(0, 100);

        assertThat(result.deletedJobs()).isZero();
        assertThat(result.failedJobs()).isEqualTo(1);
        assertThat(jobRepository.findById(job.getId())).isPresent();
        verify(blobStoreService, times(1)).delete(subFolder + "/parking.zip");
    }

    @Test
    public void purgeKeepsMostRecentJobsPerAction() throws IOException {
        Instant now = Instant.now();
        // Same subFolder and type: imports are more recent than exports, but must not push exports out of keepJobs
        List<Job> exports = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            exports.add(createJob(JobType.NETEX_STOP_PLACE_QUAY, JobAction.EXPORT, JobStatus.FINISHED,
                    now.minus(Duration.ofDays(200)).minus(Duration.ofHours(i)), "export_" + i + ".zip"));
        }
        List<Job> imports = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            imports.add(createJob(JobType.NETEX_STOP_PLACE_QUAY, JobAction.IMPORT, JobStatus.FINISHED,
                    now.minus(Duration.ofDays(150)).minus(Duration.ofHours(i)), "import_" + i + ".zip"));
        }

        JobPurgeResult result = jobPurgeService.purgeCompletedJobs(2, 100);

        // 2 most recent of each action kept: 1 export and 3 imports deleted
        assertThat(result.deletedJobs()).isEqualTo(4);
        assertThat(result.deletedLocalFiles()).isEqualTo(1);
        assertThat(result.deletedBlobs()).isEqualTo(1);
        assertThat(result.failedJobs()).isZero();

        Set<Long> remainingIds = Set.copyOf(jobRepository.findAll().stream().map(Job::getId).toList());
        assertThat(remainingIds).hasSize(4);
        exports.subList(0, 2).forEach(job -> assertKept(remainingIds, job));
        imports.subList(0, 2).forEach(job -> assertKept(remainingIds, job));

        Job deletedExport = exports.get(2);
        assertThat(remainingIds).doesNotContain(deletedExport.getId());
        assertThat(providerDir.resolve(deletedExport.getFileName())).doesNotExist();
        verify(blobStoreService).delete(subFolder + "/" + deletedExport.getFileName());

        // Import files are never deleted, even when the job is
        imports.subList(2, 5).forEach(job -> {
            assertThat(remainingIds).doesNotContain(job.getId());
            assertThat(providerDir.resolve(job.getFileName())).exists();
        });
    }

    private void assertKept(Set<Long> remainingIds, Job job) {
        assertThat(remainingIds).contains(job.getId());
        assertThat(providerDir.resolve(job.getFileName())).exists();
    }

    /**
     * Create {@code count} export jobs, the first being the most recent (one hour between each job).
     */
    private List<Job> createJobs(int count, JobType type, JobStatus status, Instant mostRecent, String prefix) throws IOException {
        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            jobs.add(createJob(type, JobAction.EXPORT, status, mostRecent.minus(Duration.ofHours(i)), prefix + "_" + i + ".zip"));
        }
        return jobs;
    }

    private Job createJob(JobType type, JobAction action, JobStatus status, Instant date, String fileName) throws IOException {
        Job job = new Job(status);
        job.setType(type);
        job.setAction(action);
        job.setSubFolder(subFolder);
        job.setFileName(fileName);
        job.setStarted(date.minus(Duration.ofMinutes(5)));
        if (status != JobStatus.PROCESSING) {
            job.setFinished(date);
        }
        job.getOperators().add("OP_" + fileName);
        Path file = providerDir.resolve(fileName);
        if (!Files.exists(file)) {
            Files.writeString(file, "content");
        }
        return jobRepository.save(job);
    }
}
