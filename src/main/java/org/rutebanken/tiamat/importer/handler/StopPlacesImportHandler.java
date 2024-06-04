/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.importer.handler;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.lock.FencedLock;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ImportType;
import org.rutebanken.tiamat.importer.filter.ZoneTopographicPlaceFilter;
import org.rutebanken.tiamat.importer.merging.TransactionalMergingStopPlacesImporter;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobImportType;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.NetexUtils;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.utils.Importer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.collect;

@Component
public class StopPlacesImportHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StopPlacesImportHandler.class);
    /**
     * Hazelcast lock key for merging stop place import.
     */
    private static final String PARKING_IMPORT_LOCK_KEY = "STOP_PLACE_MERGING_IMPORT_LOCK_KEY";

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Autowired
    private PublicationDeliveryHelper publicationDeliveryHelper;

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    private ZoneTopographicPlaceFilter zoneTopographicPlaceFilter;

    @Autowired
    private TransactionalMergingStopPlacesImporter transactionalMergingStopPlacesImporter;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private JobRepository jobRepository;

    public Collection<org.rutebanken.netex.model.StopPlace> handleStopPlacesGeneralFrame(GeneralFrame generalFrame, ImportParams importParams, AtomicInteger parkingsCreatedOrUpdated, Provider provider, String fileName, String folder, Job job) throws Exception {
        if (publicationDeliveryHelper.hasGeneralFrame(generalFrame)) {
            List<JAXBElement<? extends EntityStructure>> members = generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();

            // Filtrer et obtenir les StopPlace
            List<org.rutebanken.netex.model.StopPlace> tiamatStopPlaces = members.stream()
                    .filter(member -> member.getValue() instanceof org.rutebanken.netex.model.StopPlace)
                    .map(member -> (org.rutebanken.netex.model.StopPlace) member.getValue())
                    .collect(Collectors.toList());

            List<StopPlace> stopPlacesParsed = parseStopPlaces(tiamatStopPlaces, importParams);

            int numberOfStopPlacesBeforeFiltering = stopPlacesParsed.size();
            logger.info("About to filter {} stop places based on topographic references: {}", stopPlacesParsed.size(), importParams.targetTopographicPlaces);
            stopPlacesParsed = zoneTopographicPlaceFilter.filterByTopographicPlaceMatch(importParams.targetTopographicPlaces, stopPlacesParsed);
            logger.info("Got {} stop places (was {}) after filtering by: {}", stopPlacesParsed.size(), numberOfStopPlacesBeforeFiltering, importParams.targetTopographicPlaces);

            if (importParams.onlyMatchOutsideTopographicPlaces != null && !importParams.onlyMatchOutsideTopographicPlaces.isEmpty()) {
                numberOfStopPlacesBeforeFiltering = stopPlacesParsed.size();
                logger.info("Filtering stop places outside given list of topographic places: {}", importParams.onlyMatchOutsideTopographicPlaces);
                stopPlacesParsed = zoneTopographicPlaceFilter.filterByTopographicPlaceMatch(importParams.onlyMatchOutsideTopographicPlaces, stopPlacesParsed, true);
                logger.info("Got {} stop places (was {}) after filtering", stopPlacesParsed.size(), numberOfStopPlacesBeforeFiltering);
            }

            Collection<org.rutebanken.netex.model.StopPlace> importedStopPlaces;
            if (importParams.importType == null || importParams.importType.equals(ImportType.MERGE)) {
                final FencedLock lock = hazelcastInstance.getCPSubsystem().getLock(PARKING_IMPORT_LOCK_KEY);
                lock.lock();
                try {
                    importedStopPlaces = transactionalMergingStopPlacesImporter.importStopPlaces(stopPlacesParsed, parkingsCreatedOrUpdated);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            } else if (importParams.importType.equals(ImportType.INITIAL)) {
                importedStopPlaces = transactionalMergingStopPlacesImporter.importStopPlaces(stopPlacesParsed, parkingsCreatedOrUpdated);
            } else {
                logger.warn("Import type " + importParams.importType + " not implemented. Will not match stop places.");
                importedStopPlaces = new ArrayList<>(0);
            }

            if (!importedStopPlaces.isEmpty()) {
                NetexUtils.getMembers(org.rutebanken.netex.model.StopPlace.class, members);
            }

            Job jobUpdated = Importer.manageJob(job, JobStatus.FINISHED, importParams, provider, fileName, folder, null, JobImportType.NETEX_STOP_PlACE_QUAY);
            jobRepository.save(jobUpdated);
            logger.info("Mapped {} stop places !!", tiamatStopPlaces.size());
            return importedStopPlaces;
        }
        return null;
    }

    private List<StopPlace> parseStopPlaces(List<org.rutebanken.netex.model.StopPlace> netexStopPlacesInFrame, ImportParams importParams) throws Exception {
        if (netexStopPlacesInFrame.isEmpty())
            return null;

        List<StopPlace_VersionStructure> stopPlaces = new ArrayList<>();
        stopPlaces.addAll(netexStopPlacesInFrame);
        return convertVersionStructureToStopPlaces(stopPlaces, importParams);
    }

    public List<StopPlace> convertVersionStructureToStopPlaces(List<StopPlace_VersionStructure> stopPlaces, ImportParams importParams) throws Exception {
        List<StopPlace> stopPlacesList = new ArrayList<>();
        stopPlaces.stream()
                .filter(serviceStopPlace -> serviceStopPlace instanceof org.rutebanken.netex.model.StopPlace)
                .map(netexStopPlace -> (org.rutebanken.netex.model.StopPlace) netexStopPlace)
                .forEach(netexStopPlace -> {
                    StopPlace stopPlace = new StopPlace();
                    netexMapper.parseToSetStopPlaceGlobalInformations(netexStopPlace, stopPlace);
                    stopPlacesList.add(stopPlace);
                });

        return stopPlacesList;
    }
}
