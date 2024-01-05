package org.rutebanken.tiamat.service.batch;

import com.hazelcast.core.HazelcastInstance;
import org.rutebanken.tiamat.netex.id.GaplessIdGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
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
            Executors.newScheduledThreadPool(3, (runnable) -> new Thread(runnable, "background-job-"+threadNumber.incrementAndGet()));

    private final GaplessIdGeneratorService gaplessIdGeneratorService;

    private final StopPlaceRefUpdaterService stopPlaceRefUpdaterService;



    @Autowired
    private HazelcastInstance hazelcastInstance;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public BackgroundJobs(GaplessIdGeneratorService gaplessIdGeneratorService, StopPlaceRefUpdaterService stopPlaceRefUpdaterService) {
        this.gaplessIdGeneratorService = gaplessIdGeneratorService;
        this.stopPlaceRefUpdaterService = stopPlaceRefUpdaterService;
    }

    @PostConstruct
    public void scheduleBackgroundJobs() {
        logger.info("Scheduling background job for gaplessIdGeneratorService");
        backgroundJobExecutor.scheduleAtFixedRate(gaplessIdGeneratorService::persistClaimedIds, 15, 15, TimeUnit.SECONDS);

        // Initial delay for the background stop place reference updater service can be good to avoid conflicts when running tests
        logger.info("Scheduling background job for updating stop places");
//        backgroundJobExecutor.scheduleAtFixedRate(stopPlaceRefUpdaterService::updateAllStopPlaces, 30, 280, TimeUnit.MINUTES);

        syncIdGenerator();
    }

    /**
     * Synchronize table idGenerator and clear all queues stored in hazelcast
     */
    private void syncIdGenerator() {

        Query query = entityManager.createNativeQuery("SELECT sync_id_generator_table()");
        query.getSingleResult();
        logger.info("Id generator table has been synchronized");

        List<String> queueList = Arrays.asList("StopPlace", "AccessibilityLimitation", "Quay","AccessibilityAssessment");

        for (String queueName : queueList) {
            BlockingQueue<Long> queue = hazelcastInstance.getQueue(queueName);
            if(queue != null){
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
