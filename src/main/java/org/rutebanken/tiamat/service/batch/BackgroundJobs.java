package org.rutebanken.tiamat.service.batch;

import com.hazelcast.core.HazelcastInstance;
import org.rutebanken.tiamat.netex.id.GaplessIdGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Jobs that run periodically in the background
 */
@Service
public class BackgroundJobs {

    private static final Logger logger = LoggerFactory.getLogger(BackgroundJobs.class);

    private static final AtomicLong threadNumber = new AtomicLong();

    private final ScheduledExecutorService backgroundJobExecutor =
            Executors.newScheduledThreadPool(6, (runnable) -> new Thread(runnable, "background-job-" + threadNumber.incrementAndGet()));

    private final GaplessIdGeneratorService gaplessIdGeneratorService;

    private final StopPlaceRefUpdaterService stopPlaceRefUpdaterService;

    private final QuayIdMappingService quayIdMappingService;

    private final StopPlaceIdMappingService stopPlaceIdMappingService;

    private final ParkingIdMappingService parkingIdMappingService;

    private final PoiIdMappingService poiIdMappingService;

    private final StopPlaceQuayAssociationService stopPlaceQuayAssociationService;

    @Value("${id.synchronization.enabled:true}")
    boolean shoundSyncIds;
    @Autowired
    private HazelcastInstance hazelcastInstance;
    @PersistenceContext
    private EntityManager entityManager;


    @Autowired
    public BackgroundJobs(GaplessIdGeneratorService gaplessIdGeneratorService, StopPlaceRefUpdaterService stopPlaceRefUpdaterService, QuayIdMappingService quayIdMappingService,
                          StopPlaceIdMappingService stopPlaceIdMappingService, ParkingIdMappingService parkingIdMappingService, PoiIdMappingService poiIdMappingService,
                          StopPlaceQuayAssociationService stopPlaceQuayAssociationService) {
        this.gaplessIdGeneratorService = gaplessIdGeneratorService;
        this.stopPlaceRefUpdaterService = stopPlaceRefUpdaterService;
        this.quayIdMappingService = quayIdMappingService;
        this.stopPlaceIdMappingService = stopPlaceIdMappingService;
        this.parkingIdMappingService = parkingIdMappingService;
        this.poiIdMappingService = poiIdMappingService;
        this.stopPlaceQuayAssociationService = stopPlaceQuayAssociationService;
    }

    @PostConstruct
    public void scheduleBackgroundJobs() {
        logger.info("Scheduling background job for gaplessIdGeneratorService");
        backgroundJobExecutor.scheduleAtFixedRate(gaplessIdGeneratorService::persistClaimedIds, 15, 15, TimeUnit.SECONDS);

        // Initial delay for the background stop place reference updater service can be good to avoid conflicts when running tests
        logger.info("Scheduling background job for updating stop places");
        backgroundJobExecutor.scheduleAtFixedRate(stopPlaceRefUpdaterService::updateAllStopPlaces, 30, 280, TimeUnit.MINUTES);

        // File creation jobs are staggered so that they never start at the same time:
        // each one holds a DB connection (long transaction scrolling a whole table) and running
        // them together can exhaust the connection pool.
        // 2h jobs start at minutes 0, 20, 40, 60 (mod 120); the 3h job starts at minute 90,
        // so its runs fall at minutes 30 or 90 (mod 120), never overlapping the start of a 2h job.
        logger.info("Scheduling background job for quay id mapping file creation");
        backgroundJobExecutor.scheduleAtFixedRate(quayIdMappingService::createIdMappingFile, 0, 120, TimeUnit.MINUTES);

        logger.info("Scheduling background job for stopPlace id mapping file creation");
        backgroundJobExecutor.scheduleAtFixedRate(stopPlaceIdMappingService::createIdMappingFile, 20, 120, TimeUnit.MINUTES);

        logger.info("Scheduling background job for parking id mapping file creation");
        backgroundJobExecutor.scheduleAtFixedRate(parkingIdMappingService::createIdMappingFile, 40, 120, TimeUnit.MINUTES);

        logger.info("Scheduling background job for POI id mapping file creation");
        backgroundJobExecutor.scheduleAtFixedRate(poiIdMappingService::createIdMappingFile, 60, 120, TimeUnit.MINUTES);

        logger.info("Scheduling background job for stopPlace/quay association file creation");
        backgroundJobExecutor.scheduleAtFixedRate(stopPlaceQuayAssociationService::createSpQuayFile, 90, 180, TimeUnit.MINUTES);

        if (shoundSyncIds) {
            syncIdGenerator();
        }

    }

    /**
     * Synchronize table idGenerator and clear all queues stored in hazelcast
     */
    private void syncIdGenerator() {

        Query query = entityManager.createNativeQuery("SELECT sync_id_generator_table()");
        query.getSingleResult();
        logger.info("Id generator table has been synchronized");

        List<String> queueList = Arrays.asList("StopPlace", "AccessibilityLimitation", "Quay", "AccessibilityAssessment");

        for (String queueName : queueList) {
            BlockingQueue<Long> queue = hazelcastInstance.getQueue(queueName);
            if (queue != null) {
                logger.info("Clearing queue : " + queueName);
                queue.clear();
            }
        }
    }

    public void triggerStopPlaceUpdate() {
        logger.info("Job for updating stop place was triggered manually by thread {}. It will not be executed if a job is already running", Thread.currentThread().getName());
        backgroundJobExecutor.submit(stopPlaceRefUpdaterService::updateAllStopPlaces);
    }
}
