package org.rutebanken.tiamat.service.batch;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang.StringUtils;
import org.rutebanken.tiamat.lock.LockException;
import org.rutebanken.tiamat.lock.TimeoutMaxLeaseTimeLock;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.repository.PointOfInterestRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Service to create a POI id mapping file
 */
@Service
@Transactional
public class PoiIdMappingService {

    private static final int MAX_LEASE_TIME_SECONDS = 7200;
    private static final int WAIT_TIMEOUT_SECONDS = 10;
    private static final String BACKGROUND_UPDATE_POIS_LOCK = "background-poi-id-mapping-file-creation";
    private static final Logger logger = LoggerFactory.getLogger(PoiIdMappingService.class);

    private final TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock;
    private final PointOfInterestRepositoryImpl poiRepository;
    private final String administrationSpaceName;
    private final String localExportPath;
    private final String poiIdMappingFilename;

    @Autowired
    public PoiIdMappingService(PointOfInterestRepositoryImpl poiRepository, TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock, @Value("${administration.space.name}") String administrationSpaceName, @Value("${async.export.path:/deployments/data/}") String localExportPath, @Value("${poi.id.mapping.filename:poiIdMappings.csv}") String poiIdMappingFilename) {
        this.timeoutMaxLeaseTimeLock = timeoutMaxLeaseTimeLock;
        this.poiRepository = poiRepository;
        this.administrationSpaceName = administrationSpaceName;
        this.localExportPath = localExportPath;
        this.poiIdMappingFilename = poiIdMappingFilename;
    }

    public void createIdMappingFile() {
        try {
            // To avoid multiple hazelcast instances doing the same job
            timeoutMaxLeaseTimeLock.executeInLock(() -> {
                try {
                    launchFileCreation();
                } catch (Exception e) {
                    logger.error("Error while creating POI id mapping file", e);
                }
                return null;
            }, BACKGROUND_UPDATE_POIS_LOCK, WAIT_TIMEOUT_SECONDS, MAX_LEASE_TIME_SECONDS);
        } catch (LockException lockException) {
            logger.info(lockException.getMessage());
        } catch (RuntimeException e) {
            logger.error("Background job stopped because of exception", e);
        }
    }

    public void launchFileCreation() {
        logger.info("Starting POI id mapping file creation");
        List<PointOfInterest> POIs = poiRepository.findAllPOILastVersionAndValid();
        if (CollectionUtils.isEmpty(POIs)) {
            logger.info("No POI found, abort");
            return;
        }

        // Create parent directory if it does not exist
        String absolutePath = localExportPath + "/" + administrationSpaceName + "/" + poiIdMappingFilename;
        File f = new File(absolutePath);
        f.getParentFile().mkdirs();

        try (var printer = CSVFormat.RFC4180.print(f, StandardCharsets.UTF_8)) {
                printer.printRecord("operator", "originalId", "netexId");

            for (var POI : POIs) {
                if (StringUtils.isBlank(POI.getOperator())) {
                    logger.error("POI with id {} has no operator, discard it", POI.getId());
                    continue;
                }
                String originalId = POI.getOriginalIds().stream().findFirst().orElse(null);
                if (StringUtils.isBlank(originalId)) {
                    logger.error("POI with id {} has no originalId, discard it", POI.getId());
                    continue;
                }
                if (StringUtils.isBlank(POI.getNetexId())) {
                    logger.error("POI with id {} has no netexId, discard it", POI.getId());
                    continue;
                }
                printer.printRecord(POI.getOperator(), originalId, POI.getNetexId());
            }

            printer.flush();
        } catch (IOException e) {
            logger.error("Error while generating POI id mapping file", e);
        }
    }
}
