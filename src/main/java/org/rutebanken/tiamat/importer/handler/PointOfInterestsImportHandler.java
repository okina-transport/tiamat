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
import org.rutebanken.tiamat.importer.finder.PointOfInterestFromOriginalIdFinder;
import org.rutebanken.tiamat.importer.initial.ParallelInitialPointOfInterestImporter;
import org.rutebanken.tiamat.importer.merging.TransactionalMergingPointOfInterestssImporter;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobImportType;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.rest.utils.Importer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PointOfInterestsImportHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(PointOfInterestsImportHandler.class);
    /**
     * Hazelcast lock key for merging stop place import.
     */
    private static final String POI_IMPORT_LOCK_KEY = "STOP_PLACE_MERGING_IMPORT_LOCK_KEY";

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    private ZoneTopographicPlaceFilter zoneTopographicPlaceFilter;

    @Autowired
    private TransactionalMergingPointOfInterestssImporter transactionalMergingPointOfInterestssImporter;

    @Autowired
    private ParallelInitialPointOfInterestImporter parallelInitialPointOfInterestImporter;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PointOfInterestFromOriginalIdFinder pointOfInterestRepository;

    public void handlePointOfInterests(SiteFrame netexSiteFrame, ImportParams importParams, AtomicInteger poisCreatedOrUpdated, SiteFrame responseSiteframe, Provider provider, String fileName, String folder, Job job, List<GeneralOrganisation> generalOrganisations, List<ResponsibilitySet> responsibilitySets) {
        List<org.rutebanken.tiamat.model.PointOfInterest> tiamatPointOfInterests = netexMapper.mapPointsOfInterestToTiamatModel(netexSiteFrame.getPointsOfInterest().getPointOfInterest());

        setOperatorPoiForGeneralFrame(generalOrganisations, responsibilitySets, tiamatPointOfInterests);

        parseToSynchronizeKeyValues(netexSiteFrame, tiamatPointOfInterests);

        int numberOfPoisBeforeFiltering = tiamatPointOfInterests.size();
        logger.info("About to filter {} point of interests based on topographic references: {}", tiamatPointOfInterests.size(), importParams.targetTopographicPlaces);
        tiamatPointOfInterests = zoneTopographicPlaceFilter.filterByTopographicPlaceMatch(importParams.targetTopographicPlaces, tiamatPointOfInterests);
        logger.info("Got {} point of interests (was {}) after filtering by: {}", tiamatPointOfInterests.size(), numberOfPoisBeforeFiltering, importParams.targetTopographicPlaces);

        if (importParams.onlyMatchOutsideTopographicPlaces != null && !importParams.onlyMatchOutsideTopographicPlaces.isEmpty()) {
            numberOfPoisBeforeFiltering = tiamatPointOfInterests.size();
            logger.info("Filtering point of interests outside given list of topographic places: {}", importParams.onlyMatchOutsideTopographicPlaces);
            tiamatPointOfInterests = zoneTopographicPlaceFilter.filterByTopographicPlaceMatch(importParams.onlyMatchOutsideTopographicPlaces, tiamatPointOfInterests, true);
            logger.info("Got {} point of interests (was {}) after filtering", tiamatPointOfInterests.size(), numberOfPoisBeforeFiltering);
        }

        Collection<PointOfInterest> importedPointOfInterests;

        if (importParams.importType == null || importParams.importType.equals(ImportType.MERGE)) {
            final FencedLock lock = hazelcastInstance.getCPSubsystem().getLock(POI_IMPORT_LOCK_KEY);
            lock.lock();
            try {
                importedPointOfInterests = transactionalMergingPointOfInterestssImporter.importPointOfInterests(tiamatPointOfInterests, poisCreatedOrUpdated);
            } finally {
                lock.unlock();
            }
        } else if (importParams.importType.equals(ImportType.INITIAL)) {
            importedPointOfInterests = parallelInitialPointOfInterestImporter.importPointOfInterests(tiamatPointOfInterests, poisCreatedOrUpdated);
        } else {
            logger.warn("Import type " + importParams.importType + " not implemented. Will not match point of interest.");
            importedPointOfInterests = new ArrayList<>(0);
        }

        if (!importedPointOfInterests.isEmpty()) {
            responseSiteframe.withPointsOfInterest(
                    new PointsOfInterestInFrame_RelStructure()
                            .withPointOfInterest(importedPointOfInterests));
        }

        Job jobUpdated = Importer.manageJob(job, JobStatus.FINISHED, importParams, provider, fileName, folder,  null, JobImportType.NETEX_POI);
        jobRepository.save(jobUpdated);
        logger.info("Mapped {} point of interests!!", tiamatPointOfInterests.size());
    }

    private void setOperatorPoiForGeneralFrame(List<GeneralOrganisation> generalOrganisations, List<ResponsibilitySet> responsibilitySets, List<org.rutebanken.tiamat.model.PointOfInterest> tiamatPointOfInterests) {
        Map<String, ResponsibilitySet> responsibilitySetMap = responsibilitySets.stream()
                .collect(Collectors.toMap(ResponsibilitySet::getId, rs -> rs));

        Map<String, GeneralOrganisation> generalOrganisationMap = generalOrganisations.stream()
                .collect(Collectors.toMap(GeneralOrganisation::getId, go -> go));

        tiamatPointOfInterests.forEach(poi -> {
            // Ajouter la logique pour mapper l'opérateur
            String responsibilitySetRef = poi.getResponsibilitySetRef();
            if (responsibilitySetRef != null) {
                ResponsibilitySet responsibilitySet = responsibilitySetMap.get(responsibilitySetRef);
                if (responsibilitySet != null) {
                    ResponsibilityRoleAssignment_VersionedChildStructure responsibilityRoleAssignment = responsibilitySet.getRoles().getResponsibilityRoleAssignment().get(0);
                    String organisationRef = responsibilityRoleAssignment.getResponsibleOrganisationRef().getRef();
                    GeneralOrganisation generalOrganisation = generalOrganisationMap.get(organisationRef);
                    if (generalOrganisation != null && generalOrganisation.getName() != null) {
                        // Mettre à jour l'opérateur pour le point of interest
                        poi.setOperator(generalOrganisation.getName().getValue());
                    }
                }
            }
        });
    }

    private void parseToSynchronizeKeyValues(SiteFrame netexSiteFrame, List<org.rutebanken.tiamat.model.PointOfInterest> tiamatPointOfInterests) {
        // Map Netex POIs by ID for quick access
        Map<String, PointOfInterest> netexPoiMap = netexSiteFrame.getPointsOfInterest().getPointOfInterest().stream()
                .collect(Collectors.toMap(PointOfInterest::getId, Function.identity()));

        // Process each Tiamat POI
        tiamatPointOfInterests.forEach(tiamatPoi -> {
            org.rutebanken.tiamat.model.PointOfInterest tiamat = pointOfInterestRepository.find(tiamatPoi);

            // Check if there's a corresponding Netex POI
            if (tiamat != null && netexPoiMap.containsKey(tiamat.getNetexId())) {
                PointOfInterest correspondingNetexPoi = netexPoiMap.get(tiamat.getNetexId());
                correspondingNetexPoi.getKeyList().getKeyValue().forEach(entry -> {
                    tiamatPoi.getOrCreateValues(entry.getKey()).add(entry.getValue());
                });
            }
        });
    }
}
