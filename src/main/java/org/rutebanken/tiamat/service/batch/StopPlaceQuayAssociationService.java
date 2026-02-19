package org.rutebanken.tiamat.service.batch;

import org.rutebanken.tiamat.dtoassembling.dto.JbvCodeMappingDto;
import org.rutebanken.tiamat.lock.LockException;
import org.rutebanken.tiamat.lock.TimeoutMaxLeaseTimeLock;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class StopPlaceQuayAssociationService {

    @Value("${async.export.path:/deployments/data/}")
    private String localExportPath;

    @Value("${administration.space.name}")
    protected String administrationSpaceName;

    @Value("${stopplace.quay.association.filename:stopPlaceAndQuays.csv}")
    private String stopPlaceAndQuayAssociationFilename;


    private static final Logger logger = LoggerFactory.getLogger(StopPlaceQuayAssociationService.class);

    private final TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock;

    StopPlaceRepository stopPlaceRepository;

    public static final String BACKGROUND_UPDATE_STOPS_LOCK = "background-sp-quay-association-file-creation";
    public static final int MAX_LEASE_TIME_SECONDS = 7200;
    public static final int WAIT_TIMEOUT_SECONDS = 10;



    @Autowired
    public StopPlaceQuayAssociationService(StopPlaceRepository stopPlaceRepository, TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock) {
        this.timeoutMaxLeaseTimeLock = timeoutMaxLeaseTimeLock;
        this.stopPlaceRepository = stopPlaceRepository;
    }


    public void createSpQuayFile() {
        try {
            // To avoid multiple hazelcast instances doing the same job
            timeoutMaxLeaseTimeLock.executeInLock(() -> {

                try {
                    launchFileCreation();
                } catch (Exception e) {
                    logger.error("Error while creating stopPlace quay association file", e);
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

        Instant validFrom = Instant.now();
        Instant validTo =  null;
        Map<String, Set<String>> stopPlaces = stopPlaceRepository.listStopPlaceIdsAndQuayIds(validFrom, validTo);


        String absolutePath = localExportPath + "/" + administrationSpaceName + "/" + stopPlaceAndQuayAssociationFilename;

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(absolutePath))){


            for (Map.Entry<String, Set<String>> spAndQuayEntry : stopPlaces.entrySet()) {
                String stopRef = spAndQuayEntry.getKey();
                for (String quayRef : spAndQuayEntry.getValue()) {
                    writer.write(quayRef + "," + stopRef + "\n");
                }
            }
            logger.info("stopPlace and quay association file creation completed successfully");

        }catch (IOException e) {
            logger.error("Error while writing stopPlace and quays association file", e);
        }


    }





}
