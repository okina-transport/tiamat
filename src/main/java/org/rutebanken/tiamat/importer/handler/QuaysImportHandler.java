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
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.GeneralFrame;
import org.rutebanken.netex.model.Quay_VersionStructure;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ImportType;
import org.rutebanken.tiamat.importer.filter.ZoneTopographicPlaceFilter;
import org.rutebanken.tiamat.importer.initial.ParallelInitialQuayImporter;
import org.rutebanken.tiamat.importer.merging.TransactionalMergingQuaysImporter;
import org.rutebanken.tiamat.model.Quay;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class QuaysImportHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(QuaysImportHandler.class);
    /**
     * Hazelcast lock key for merging quay import.
     */
    private static final String QUAY_IMPORT_LOCK_KEY = "STOP_PLACE_MERGING_IMPORT_LOCK_KEY";

    @Autowired
    private PublicationDeliveryHelper publicationDeliveryHelper;

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    private ZoneTopographicPlaceFilter zoneTopographicPlaceFilter;

    @Autowired
    private TransactionalMergingQuaysImporter transactionalMergingQuaysImporter;

    @Autowired
    private ParallelInitialQuayImporter parallelInitialQuayImporter;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private JobRepository jobRepository;

    public void handleQuaysGeneralFrame(GeneralFrame generalFrame, ImportParams importParams, AtomicInteger quaysCreatedOrUpdated, Provider provider, String fileName, String folder, Job job) throws Exception {
        if (publicationDeliveryHelper.hasGeneralFrame(generalFrame)) {
            List<JAXBElement<? extends EntityStructure>> members = generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();
            // Filtrer et obtenir les Quay
            List<org.rutebanken.netex.model.Quay> tiamatQuays = members.stream()
                    .filter(member -> member.getValue() instanceof org.rutebanken.netex.model.Quay)
                    .map(member -> (org.rutebanken.netex.model.Quay) member.getValue())
                    .collect(Collectors.toList());

            List<Quay> quaysParsed = parseQuays(tiamatQuays);

            int numberOfQuaysBeforeFiltering = quaysParsed.size();
            logger.info("About to filter {} quays based on topographic references: {}", quaysParsed.size(), importParams.targetTopographicPlaces);
            quaysParsed = zoneTopographicPlaceFilter.filterByTopographicPlaceMatch(importParams.targetTopographicPlaces, quaysParsed);
            logger.info("Got {} quays (was {}) after filtering by: {}", quaysParsed.size(), numberOfQuaysBeforeFiltering, importParams.targetTopographicPlaces);

            if (importParams.onlyMatchOutsideTopographicPlaces != null && !importParams.onlyMatchOutsideTopographicPlaces.isEmpty()) {
                numberOfQuaysBeforeFiltering = quaysParsed.size();
                logger.info("Filtering quays outside given list of topographic places: {}", importParams.onlyMatchOutsideTopographicPlaces);
                quaysParsed = zoneTopographicPlaceFilter.filterByTopographicPlaceMatch(importParams.onlyMatchOutsideTopographicPlaces, quaysParsed, true);
                logger.info("Got {} quays (was {}) after filtering", quaysParsed.size(), numberOfQuaysBeforeFiltering);
            }

            Collection<org.rutebanken.netex.model.Quay> importedQuays;
            if (importParams.importType == null || importParams.importType.equals(ImportType.MERGE)) {
                final FencedLock lock = hazelcastInstance.getCPSubsystem().getLock(QUAY_IMPORT_LOCK_KEY);
                lock.lock();
                try {
                    importedQuays = transactionalMergingQuaysImporter.importQuays(quaysParsed);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            } else if (importParams.importType.equals(ImportType.INITIAL)) {
                importedQuays = parallelInitialQuayImporter.importQuays(quaysParsed, quaysCreatedOrUpdated);
            } else {
                logger.warn("Import type " + importParams.importType + " not implemented. Will not match quays.");
                importedQuays = new ArrayList<>(0);
            }

            if (!importedQuays.isEmpty()) {
                NetexUtils.getMembers(org.rutebanken.netex.model.StopPlace.class, members);
            }

            Job jobUpdated = Importer.manageJob(job, JobStatus.FINISHED, importParams, provider, fileName, folder, null, JobImportType.NETEX_STOP_PlACE_QUAY);
            jobRepository.save(jobUpdated);
            logger.info("Mapped {} quays !!", tiamatQuays.size());
        }
    }

    private List<Quay> parseQuays(List<org.rutebanken.netex.model.Quay> netexQuaysInFrame) throws Exception {
        if (netexQuaysInFrame.isEmpty())
            return null;

        List<Quay_VersionStructure> quays = new ArrayList<>();
        quays.addAll(netexQuaysInFrame);
        return convertVersionStructureToQuays(quays);
    }


    public List<Quay> convertVersionStructureToQuays(List<Quay_VersionStructure> stopPlaces) throws Exception {
        List<Quay> quaysList = new ArrayList<>();
        stopPlaces.stream()
                .filter(serviceQuay -> serviceQuay instanceof org.rutebanken.netex.model.Quay)
                .map(netexQuay -> (org.rutebanken.netex.model.Quay) netexQuay)
                .forEach(netexQuay -> {
                    Quay quay = new Quay();
                    netexMapper.parseToSetQuayGlobalInformations(netexQuay, quay);
                    quaysList.add(quay);
                });
        return quaysList;
    }
}
