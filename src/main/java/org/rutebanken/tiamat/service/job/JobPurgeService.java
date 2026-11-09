package org.rutebanken.tiamat.service.job;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.service.BlobStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Purge of old completed jobs, with the same semantics as Chouette's
 * {@code DELETE /chouette_iev/admin/completed_jobs?keepJobs&keepDays}.
 * <p>
 * Jobs are grouped by (subFolder, type, action). In each group the {@code keepJobs} most recent completed jobs are kept,
 * and among the others only those completed more than {@code keepDays} days ago are deleted.
 * Jobs still PROCESSING are never touched.
 * <p>
 * For deleted EXPORT jobs, the export file referenced by the job is deleted locally
 * ({@code async.export.path/<subFolder>/<fileName>}) and in the blob store ({@code <subFolder>/<fileName>}).
 * Files not referenced by a deleted job (e.g. mapping CSV files) are never touched.
 */
@Service
public class JobPurgeService {

    private static final Logger logger = LoggerFactory.getLogger(JobPurgeService.class);

    /**
     * Terminal statuses: every status except PROCESSING.
     */
    static final Set<JobStatus> COMPLETED_STATUSES = EnumSet.complementOf(EnumSet.of(JobStatus.PROCESSING));

    private static final int BATCH_SIZE = 500;

    private static final Comparator<JobPurgeCandidate> MOST_RECENT_FIRST = Comparator
            .comparing(JobPurgeCandidate::completedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(JobPurgeCandidate::id, Comparator.reverseOrder());

    private final JobRepository jobRepository;
    private final BlobStoreService blobStoreService;
    private final TransactionTemplate transactionTemplate;
    private final Path localExportPath;
    private final boolean purgeBlobStore;
    private final Set<String> protectedFileNames;

    public JobPurgeService(JobRepository jobRepository,
                           BlobStoreService blobStoreService,
                           TransactionTemplate transactionTemplate,
                           @Value("${async.export.path:/deployments/data/}") String localExportPath,
                           @Value("${tiamat.export.destination:both}") String exportDestination,
                           @Value("${quay.id.mapping.filename:quayIdMappings.csv}") String quayIdMappingFilename,
                           @Value("${stopplace.quay.association.filename:stopPlaceAndQuays.csv}") String stopPlaceQuayAssociationFilename) {
        this.jobRepository = jobRepository;
        this.blobStoreService = blobStoreService;
        this.transactionTemplate = transactionTemplate;
        this.localExportPath = Paths.get(localExportPath).toAbsolutePath().normalize();
        this.purgeBlobStore = !StringUtils.equalsIgnoreCase(exportDestination, "local");
        this.protectedFileNames = new HashSet<>(Arrays.asList(quayIdMappingFilename, stopPlaceQuayAssociationFilename));
    }

    public JobPurgeResult purgeCompletedJobs(int keepJobs, int keepDays) {
        if (keepJobs < 0 || keepDays < 0) {
            throw new IllegalArgumentException("keepJobs and keepDays must be greater than or equal to 0");
        }
        Instant limit = Instant.now().minus(Duration.ofDays(keepDays));
        logger.info("Starting purge of completed jobs: keepJobs={}, keepDays={} (completed before {})", keepJobs, keepDays, limit);

        List<JobPurgeCandidate> completedJobs = jobRepository.findPurgeCandidatesByStatusIn(COMPLETED_STATUSES);
        List<JobPurgeCandidate> toDelete = selectJobsToDelete(completedJobs, keepJobs, limit);
        if (toDelete.isEmpty()) {
            logger.info("Purge of completed jobs: nothing to delete");
            return new JobPurgeResult(0, 0, 0, 0);
        }

        Set<Long> deletedIds = toDelete.stream().map(JobPurgeCandidate::id).collect(Collectors.toSet());
        Set<String> filesStillReferenced = findFilesReferencedByRemainingJobs(completedJobs, deletedIds);

        int deletedJobs = 0;
        int deletedLocalFiles = 0;
        int deletedBlobs = 0;
        int failedJobs = 0;

        for (List<JobPurgeCandidate> batch : Lists.partition(toDelete, BATCH_SIZE)) {
            List<Long> idsToDelete = new ArrayList<>(batch.size());
            for (JobPurgeCandidate job : batch) {
                try {
                    if (shouldDeleteFile(job, filesStillReferenced)) {
                        if (deleteLocalFile(job)) {
                            deletedLocalFiles++;
                        }
                        if (purgeBlobStore && blobStoreService.delete(job.subFolder() + "/" + job.fileName())) {
                            deletedBlobs++;
                        }
                    }
                    idsToDelete.add(job.id());
                } catch (Exception e) {
                    // Keep the job row so that the file deletion is retried during the next purge
                    failedJobs++;
                    logger.warn("Could not delete files of job {} ({}/{}), job kept: {}", job.id(), job.subFolder(), job.fileName(), e.getMessage());
                }
            }
            if (!idsToDelete.isEmpty()) {
                Integer deleted = transactionTemplate.execute(status -> {
                    jobRepository.deleteOperatorsByJobIdIn(idsToDelete);
                    jobRepository.deleteJobIdListByJobIdIn(idsToDelete);
                    return jobRepository.deleteByIdIn(idsToDelete);
                });
                deletedJobs += deleted == null ? 0 : deleted;
            }
        }

        JobPurgeResult result = new JobPurgeResult(deletedJobs, deletedLocalFiles, deletedBlobs, failedJobs);
        logger.info("Purge of completed jobs done: {} jobs deleted, {} local files deleted, {} blobs deleted, {} jobs in error",
                deletedJobs, deletedLocalFiles, deletedBlobs, failedJobs);
        return result;
    }

    /**
     * Group by (subFolder, type, action), keep the {@code keepJobs} most recent of each group, and select among the others
     * those completed before {@code limit}.
     */
    static List<JobPurgeCandidate> selectJobsToDelete(List<JobPurgeCandidate> completedJobs, int keepJobs, Instant limit) {
        Map<GroupKey, List<JobPurgeCandidate>> groups = completedJobs.stream()
                .collect(Collectors.groupingBy(j -> new GroupKey(j.subFolder(), j.type(), j.action()), LinkedHashMap::new, Collectors.toList()));

        List<JobPurgeCandidate> toDelete = new ArrayList<>();
        groups.forEach((key, jobs) -> {
            List<JobPurgeCandidate> deletable = jobs.stream()
                    .sorted(MOST_RECENT_FIRST)
                    .skip(keepJobs)
                    .filter(j -> j.completedAt() != null && j.completedAt().isBefore(limit))
                    .toList();
            if (!deletable.isEmpty()) {
                logger.info("Purge of completed jobs: {} of {} jobs to delete for subFolder={}, type={}, action={}",
                        deletable.size(), jobs.size(), key.subFolder(), key.type(), key.action());
            }
            toDelete.addAll(deletable);
        });
        return toDelete;
    }

    private Set<String> findFilesReferencedByRemainingJobs(List<JobPurgeCandidate> completedJobs, Set<Long> deletedIds) {
        Set<String> referenced = new HashSet<>();
        // Completed jobs that are kept
        completedJobs.stream()
                .filter(j -> !deletedIds.contains(j.id()))
                .forEach(j -> referenced.add(fileKey(j)));
        // Jobs still processing (or without status)
        jobRepository.findPurgeCandidatesByStatusNotIn(COMPLETED_STATUSES)
                .forEach(j -> referenced.add(fileKey(j)));
        return referenced;
    }

    private boolean shouldDeleteFile(JobPurgeCandidate job, Set<String> filesStillReferenced) {
        // Only export jobs produce a file under async.export.path / blob store. Import jobs only carry the name of the uploaded file.
        return job.action() == JobAction.EXPORT
                && isSafePathElement(job.subFolder())
                && isSafePathElement(job.fileName())
                && !protectedFileNames.contains(job.fileName())
                && !filesStillReferenced.contains(fileKey(job));
    }

    private boolean deleteLocalFile(JobPurgeCandidate job) throws IOException {
        Path file = localExportPath.resolve(job.subFolder()).resolve(job.fileName()).normalize();
        if (!file.startsWith(localExportPath) || !Files.isRegularFile(file)) {
            return false;
        }
        return Files.deleteIfExists(file);
    }

    private static boolean isSafePathElement(String value) {
        return StringUtils.isNotBlank(value)
                && !value.contains("/")
                && !value.contains("\\")
                && !value.contains("..");
    }

    private static String fileKey(JobPurgeCandidate job) {
        return job.subFolder() + "/" + job.fileName();
    }

    private record GroupKey(String subFolder, JobType type, JobAction action) {
    }
}
