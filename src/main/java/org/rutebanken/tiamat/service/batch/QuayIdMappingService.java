package org.rutebanken.tiamat.service.batch;

import org.hibernate.Session;
import org.hibernate.internal.SessionImpl;
import org.rutebanken.tiamat.dtoassembling.dto.JbvCodeMappingDto;
import org.rutebanken.tiamat.exporter.async.ParentStopFetchingIterator;
import org.rutebanken.tiamat.exporter.eviction.SessionEntitiesEvictor;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.lock.LockException;
import org.rutebanken.tiamat.lock.TimeoutMaxLeaseTimeLock;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.TariffZonesLookupService;
import org.rutebanken.tiamat.service.TopographicPlaceLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to create id mapping file
 */
@Service
@Transactional
public class QuayIdMappingService {

    private static final Logger logger = LoggerFactory.getLogger(QuayIdMappingService.class);

    private final TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock;
    public static final int MAX_LEASE_TIME_SECONDS = 7200;
    public static final int WAIT_TIMEOUT_SECONDS = 10;
    public static final String BACKGROUND_UPDATE_STOPS_LOCK = "background-quay-id-mapping-file-creation";

    private final QuayRepository quayRepository;

    @Value("${async.export.path:/deployments/data/}")
    private String localExportPath;

    @Value("${quay.id.mapping.filename:quayIdMappings.csv}")
    private String quayIdMappingFilename;

    @Value("${administration.space.name}")
    protected String administrationSpaceName;


    @Autowired
    public QuayIdMappingService(QuayRepository quayRepository, TimeoutMaxLeaseTimeLock timeoutMaxLeaseTimeLock) {
        this.timeoutMaxLeaseTimeLock = timeoutMaxLeaseTimeLock;
        this.quayRepository = quayRepository;

    }

    public void createIdMappingFile() {
        try {
            // To avoid multiple hazelcast instances doing the same job
            timeoutMaxLeaseTimeLock.executeInLock(() -> {

                try {
                    launchFileCreation();
                } catch (Exception e) {
                    logger.error("Error while creating quay id mapping file", e);
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

        List<JbvCodeMappingDto> mappings = quayRepository.findIdMappingsForQuay();
        List<JbvCodeMappingDto> selectedIdMappings = quayRepository.findSelectedIdMappingsForQuay();

        Map<String, Map<String, List<JbvCodeMappingDto>>> mapByNetex = buildNetexIdByOrg(mappings);

        replaceBySelectedIds(mapByNetex, selectedIdMappings);

        String absolutePath = localExportPath + "/" + administrationSpaceName + "/" + quayIdMappingFilename;

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
            logger.info("quay id mapping file creation completed successfully");

        }catch (IOException e) {
            logger.error("Error while writing quay id mapping file", e);
        }
    }

    /**
     * Replace mapping by selected id mapping, if existing
     * @param mapByNetex
     * @param selectedIdMappings
     */
    private void replaceBySelectedIds(Map<String, Map<String, List<JbvCodeMappingDto>>> mapByNetex, List<JbvCodeMappingDto> selectedIdMappings){

        for (JbvCodeMappingDto selectedIdMapping : selectedIdMappings) {
            String currentProv = selectedIdMapping.originalId.split(":")[0];

            if (!mapByNetex.containsKey(selectedIdMapping.netexId)){
                Map<String, List<JbvCodeMappingDto>> currentNetexMap = new HashMap<>();
                currentNetexMap.put(currentProv,Arrays.asList(selectedIdMapping));
                mapByNetex.put(selectedIdMapping.netexId,currentNetexMap);
                continue;
            }

            Map<String, List<JbvCodeMappingDto>> currentNetexMap = mapByNetex.get(selectedIdMapping.netexId);
            currentNetexMap.put(currentProv, Arrays.asList(selectedIdMapping));
        }

    }


    /**
     * Build a map that contains all mappings classified by netex_id and by provider
     *
     * @param mappings
     *      the complete lisst of mappings
     * @return
     *      the map
     */
    private Map<String, Map<String, List<JbvCodeMappingDto>>> buildNetexIdByOrg( List<JbvCodeMappingDto> mappings){

        // Key : netex id (MOBIITI:Quay:123)
        // Value :  Map with:
        //                 Key = provider(PROV1)
        //                 Value = the list of mappings for this netex id / provider
        Map<String, Map<String, List<JbvCodeMappingDto>>> resultMap = new HashMap<>();

        for (JbvCodeMappingDto mapping : mappings) {

            Map<String, List<JbvCodeMappingDto>> netexMap;
            if (resultMap.containsKey(mapping.netexId)){
                netexMap = resultMap.get(mapping.netexId);
            }else{
                netexMap = new HashMap<>();
                resultMap.put(mapping.netexId, netexMap);
            }

            String provider = mapping.originalId.split(":")[0];
            List<JbvCodeMappingDto> mappingList;
            if (netexMap.containsKey(provider)){
                mappingList = netexMap.get(provider);
            }else{
                mappingList = new ArrayList<>();
                netexMap.put(provider, mappingList);
            }
            mappingList.add(mapping);
        }
        return resultMap;
    }

}
