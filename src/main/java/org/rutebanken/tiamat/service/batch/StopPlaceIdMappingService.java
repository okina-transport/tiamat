package org.rutebanken.tiamat.service.batch;

import org.rutebanken.tiamat.dtoassembling.dto.JbvCodeMappingDto;
import org.rutebanken.tiamat.lock.LockException;
import org.rutebanken.tiamat.lock.TimeoutMaxLeaseTimeLock;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Service to create id mapping file
 */
@Service
@Transactional
public class StopPlaceIdMappingService {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceIdMappingService.class);

    StopPlaceRepository stopPlaceRepository;

    @Autowired
    IdMappingService idMappingService;

    private final TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock;
    public static final int MAX_LEASE_TIME_SECONDS = 7200;
    public static final int WAIT_TIMEOUT_SECONDS = 10;
    public static final String BACKGROUND_UPDATE_STOPS_LOCK = "background-quay-id-mapping-file-creation";


    @Value("${async.export.path:/deployments/data/}")
    private String localExportPath;

    @Value("${stopplace.id.mapping.filename:stopPlaceIdMappings.csv}")
    private String stopPlaceIdMappingsFileName;

    @Value("${administration.space.name}")
    protected String administrationSpaceName;

    @Autowired
    public StopPlaceIdMappingService(StopPlaceRepository stopPlaceRepository, TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock) {
        this.timeoutMaxLeaseTimeLock = timeoutMaxLeaseTimeLock;
        this.stopPlaceRepository = stopPlaceRepository;
    }

    public void createIdMappingFile() {
        try {
            // To avoid multiple hazelcast instances doing the same job
            timeoutMaxLeaseTimeLock.executeInLock(() -> {

                try {
                    launchFileCreation();
                } catch (Exception e) {
                    logger.error("Error while creating stopPlace id mapping file", e);
                }

                return null;
            }, BACKGROUND_UPDATE_STOPS_LOCK, WAIT_TIMEOUT_SECONDS, MAX_LEASE_TIME_SECONDS);
        } catch (LockException lockException) {
            logger.info(lockException.getMessage());
        } catch (RuntimeException e) {
            logger.warn("Background job stopped because of exception", e);
        }
    }

    public void launchFileCreation() {
        logger.info("Starting quay id mapping file creation");

        List<JbvCodeMappingDto> mappings = stopPlaceRepository.findIdMappingsForStopPlace();
        List<JbvCodeMappingDto> selectedIdMappings = stopPlaceRepository.findSelectedIdMappingsForStopPlace();

        Map<String, Map<String, List<JbvCodeMappingDto>>> mapByNetex = idMappingService.buildNetexIdByOrg(mappings);

        idMappingService.replaceBySelectedIds(mapByNetex, selectedIdMappings);

        String absolutePath = localExportPath + "/" + administrationSpaceName + "/" + stopPlaceIdMappingsFileName;

        File f = new File(absolutePath);
        f.getParentFile().mkdirs();

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(absolutePath))){
            for (Map<String, List<JbvCodeMappingDto>> currentNetexMap : mapByNetex.values()) {
                for (List<JbvCodeMappingDto> currentProvMapings : currentNetexMap.values()) {
                    for (JbvCodeMappingDto currentProvMaping : currentProvMapings) {
                        writer.write(currentProvMaping.toCsvString() + "\n");
                    }
                }
            }
            logger.info("stopPlace id mapping file creation completed successfully");

        }catch (IOException e) {
            logger.error("Error while writing stopPlace id mapping file", e);
        }
    }
}
