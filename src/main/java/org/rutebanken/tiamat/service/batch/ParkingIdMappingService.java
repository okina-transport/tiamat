package org.rutebanken.tiamat.service.batch;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.rutebanken.tiamat.lock.LockException;
import org.rutebanken.tiamat.lock.TimeoutMaxLeaseTimeLock;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.ParkingRepository;
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
import java.util.Set;

/**
 * Service to create parking id mapping file
 */
@Service
@Transactional
public class ParkingIdMappingService {

    private static final int MAX_LEASE_TIME_SECONDS = 7200;
    private static final int WAIT_TIMEOUT_SECONDS = 10;
    private static final String BACKGROUND_UPDATE_PARKINGS_LOCK = "background-parking-id-mapping-file-creation";
    private static final Logger logger = LoggerFactory.getLogger(ParkingIdMappingService.class);

    private final TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock;
    private final ParkingRepository parkingRepository;
    private final NetexMapper netexMapper;
    private final String administrationSpaceName;
    private final String localExportPath;
    private final String parkingIdMappingFilename;

    @Autowired
    public ParkingIdMappingService(ParkingRepository parkingRepository, TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock, NetexMapper netexMapper, @Value("${administration.space.name}") String administrationSpaceName, @Value("${async.export.path:/deployments/data/}") String localExportPath, @Value("${parking.id.mapping.filename:parkingIdMappings.csv}") String parkingIdMappingFilename) {
        this.timeoutMaxLeaseTimeLock = timeoutMaxLeaseTimeLock;
        this.parkingRepository = parkingRepository;
        this.netexMapper = netexMapper;
        this.administrationSpaceName = administrationSpaceName;
        this.localExportPath = localExportPath;
        this.parkingIdMappingFilename = parkingIdMappingFilename;
    }

    public void createIdMappingFile() {
        try {
            // To avoid multiple hazelcast instances doing the same job
            timeoutMaxLeaseTimeLock.executeInLock(() -> {

                try {
                    launchFileCreation();
                } catch (Exception e) {
                    logger.error("Error while creating parking id mapping file", e);
                }

                return null;
            }, BACKGROUND_UPDATE_PARKINGS_LOCK, WAIT_TIMEOUT_SECONDS, MAX_LEASE_TIME_SECONDS);
        } catch (LockException lockException) {
            logger.info(lockException.getMessage());
        } catch (RuntimeException e) {
            logger.warn("Background job stopped because of exception", e);
        }
    }

    public void launchFileCreation() {
        logger.info("Starting parking id mapping file creation");
        Set<Long> parkingIds = parkingRepository.scrollParkings();
        if (CollectionUtils.isEmpty(parkingIds)) {
            logger.info("No parking found, abort");
            return;
        }
        List<Parking> parkingsForExport =
                parkingRepository.getParkingsInitializedForExport(parkingIds);
        // Map to netex parking to get netex ids
        List<org.rutebanken.netex.model.Parking> netexParkings = parkingsForExport.stream().map(netexMapper::mapToNetexModel).toList();


        // Create parent directory if it does not exist
        String absolutePath = localExportPath + "/" + administrationSpaceName + "/" + parkingIdMappingFilename;
        File f = new File(absolutePath);
        f.getParentFile().mkdirs();

        try (var printer = CSVFormat.RFC4180.print(f, StandardCharsets.UTF_8)) {

                printer.printRecord("operator", "originalId", "netexId");

            for (int i = 0; i < netexParkings.size(); i++) {
                var netexParking = netexParkings.get(i);
                var dbParking = parkingsForExport.get(i);
                String originalId = netexParking.getKeyList().getKeyValue()
                        .stream()
                        .filter(kvs -> "imported-id".equals(kvs.getKey()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Parking with netexId" + netexParking.getId() + " has no imported-id"))
                        .getValue();
                printer.printRecord(dbParking.getOperator(), originalId, netexParking.getId());
            }

            printer.flush();
        } catch (IOException e) {
            logger.error("Error while generating parking id mapping file", e);
        }
    }
}
