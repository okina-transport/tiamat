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
import org.rutebanken.tiamat.importer.ImportType;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ParkingsImporter;
import org.rutebanken.tiamat.importer.merging.TransactionalMergingParkingsImporter;
import org.rutebanken.tiamat.importer.filter.ZoneTopographicPlaceFilter;
import org.rutebanken.tiamat.importer.initial.ParallelInitialParkingImporter;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.NetexUtils;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ParkingsImportHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ParkingsImportHandler.class);
    /**
     * Hazelcast lock key for merging stop place import.
     */
    private static final String PARKING_IMPORT_LOCK_KEY = "STOP_PLACE_MERGING_IMPORT_LOCK_KEY";

    @Autowired
    private PublicationDeliveryHelper publicationDeliveryHelper;

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    private ZoneTopographicPlaceFilter zoneTopographicPlaceFilter;

    @Autowired
    private TransactionalMergingParkingsImporter transactionalMergingParkingsImporter;

    @Autowired
    private ParallelInitialParkingImporter parallelInitialParkingImporter;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private JobRepository jobRepository;

    public void handleParkings(SiteFrame netexSiteFrame, ImportParams importParams, AtomicInteger parkingsCreatedOrUpdated, SiteFrame responseSiteframe) {

        if (publicationDeliveryHelper.hasParkings(netexSiteFrame)) {

            List<Parking> tiamatParking = netexMapper.mapParkingsToTiamatModel(netexSiteFrame.getParkings().getParking());

            int numberOfParkingsBeforeFiltering = tiamatParking.size();
            logger.info("About to filter {} parkings based on topographic references: {}", tiamatParking.size(), importParams.targetTopographicPlaces);
            tiamatParking = zoneTopographicPlaceFilter.filterByTopographicPlaceMatch(importParams.targetTopographicPlaces, tiamatParking);
            logger.info("Got {} parkings (was {}) after filtering by: {}", tiamatParking.size(), numberOfParkingsBeforeFiltering, importParams.targetTopographicPlaces);

            if (importParams.onlyMatchOutsideTopographicPlaces != null && !importParams.onlyMatchOutsideTopographicPlaces.isEmpty()) {
                numberOfParkingsBeforeFiltering = tiamatParking.size();
                logger.info("Filtering parkings outside given list of topographic places: {}", importParams.onlyMatchOutsideTopographicPlaces);
                tiamatParking = zoneTopographicPlaceFilter.filterByTopographicPlaceMatch(importParams.onlyMatchOutsideTopographicPlaces, tiamatParking, true);
                logger.info("Got {} parkings (was {}) after filtering", tiamatParking.size(), numberOfParkingsBeforeFiltering);
            }


            Collection<org.rutebanken.netex.model.Parking> importedParkings;

            if (importParams.importType == null || importParams.importType.equals(ImportType.MERGE)) {
                final FencedLock lock = hazelcastInstance.getCPSubsystem().getLock(PARKING_IMPORT_LOCK_KEY);
                lock.lock();
                try {
                    importedParkings = transactionalMergingParkingsImporter.importParkings(tiamatParking, parkingsCreatedOrUpdated);
                } finally {
                    lock.unlock();
                }
            } else if (importParams.importType.equals(ImportType.INITIAL)) {
                importedParkings = parallelInitialParkingImporter.importParkings(tiamatParking, parkingsCreatedOrUpdated);
            } else {
                logger.warn("Import type " + importParams.importType + " not implemented. Will not match parking.");
                importedParkings = new ArrayList<>(0);
            }

            if (!importedParkings.isEmpty()) {
                responseSiteframe.withParkings(
                        new ParkingsInFrame_RelStructure()
                                .withParking(importedParkings));
            }

            logger.info("Mapped {} parkings!!", tiamatParking.size());

        }
    }

    public void handleParkingsGeneralFrame(GeneralFrame generalFrame, ImportParams importParams, AtomicInteger parkingsCreatedOrUpdated, GeneralFrame responseGeneralframe, Provider provider, Job exportJob) throws Exception {
        if (publicationDeliveryHelper.hasParkingsGeneralFrame(generalFrame)) {
            List<JAXBElement<? extends EntityStructure>> members = generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();
            List<org.rutebanken.netex.model.Parking> tiamatParking = NetexUtils.getMembers(org.rutebanken.netex.model.Parking.class, members);
            List<Parking> parkingsParsed = parseParkings(tiamatParking);

            int numberOfParkingsBeforeFiltering = parkingsParsed.size();
            logger.info("About to filter {} parkings based on topographic references: {}", parkingsParsed.size(), importParams.targetTopographicPlaces);
            parkingsParsed = zoneTopographicPlaceFilter.filterByTopographicPlaceMatch(importParams.targetTopographicPlaces, parkingsParsed);
            logger.info("Got {} parkings (was {}) after filtering by: {}", parkingsParsed.size(), numberOfParkingsBeforeFiltering, importParams.targetTopographicPlaces);

            if (importParams.onlyMatchOutsideTopographicPlaces != null && !importParams.onlyMatchOutsideTopographicPlaces.isEmpty()) {
                numberOfParkingsBeforeFiltering = parkingsParsed.size();
                logger.info("Filtering parkings outside given list of topographic places: {}", importParams.onlyMatchOutsideTopographicPlaces);
                parkingsParsed = zoneTopographicPlaceFilter.filterByTopographicPlaceMatch(importParams.onlyMatchOutsideTopographicPlaces, parkingsParsed, true);
                logger.info("Got {} parkings (was {}) after filtering", parkingsParsed.size(), numberOfParkingsBeforeFiltering);
            }

            Collection<org.rutebanken.netex.model.Parking> importedParkings;
            if (importParams.importType == null || importParams.importType.equals(ImportType.MERGE)) {
                final FencedLock lock = hazelcastInstance.getCPSubsystem().getLock(PARKING_IMPORT_LOCK_KEY);
                lock.lock();
                try {
                    importedParkings = transactionalMergingParkingsImporter.importParkings(parkingsParsed, parkingsCreatedOrUpdated);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            } else if (importParams.importType.equals(ImportType.INITIAL)) {
                importedParkings = parallelInitialParkingImporter.importParkings(parkingsParsed, parkingsCreatedOrUpdated);
            } else {
                logger.warn("Import type " + importParams.importType + " not implemented. Will not match parking.");
                importedParkings = new ArrayList<>(0);
            }

            if (!importedParkings.isEmpty()) {
                NetexUtils.getMembers(org.rutebanken.netex.model.Parking.class, members);
            }

            Job job = ParkingsImporter.manageJob(exportJob, JobStatus.FINISHED, importParams, provider, null);
            jobRepository.save(job);
            logger.info("Mapped {} parkings !!", tiamatParking.size());
        }
    }

    private List<Parking> parseParkings(List<org.rutebanken.netex.model.Parking> netexParkingsInFrame) throws Exception {
        if (netexParkingsInFrame.isEmpty())
            return null;

        List<Parking_VersionStructure> parkings = new ArrayList<>();
        parkings.addAll(netexParkingsInFrame);
        return parse(parkings);
    }

    public List<Parking> parse(List<Parking_VersionStructure> parkings) throws Exception {
        List<Parking> parkingsList = new ArrayList<>();
        parkings.stream()
                .filter(serviceParking -> serviceParking instanceof org.rutebanken.netex.model.Parking)
                .map(netexParking -> (org.rutebanken.netex.model.Parking)netexParking)
                .forEach(netexParking -> {
                    Parking parking = new Parking();
                    netexMapper.parseToSetParkingGlobalInformations(netexParking, parking);
                    if (netexParking.getParkingPaymentProcess() != null) netexMapper.parseToSetParkingPaymentProcess(netexParking, parking);
                    if (netexParking.getParkingProperties() != null) netexMapper.parseToSetParkingProperties(netexParking, parking);
                    if (netexParking.getParkingAreas() != null) netexMapper.parseToSetParkingAreas(netexParking, parking);
                    if (netexParking.getPlaceEquipments() != null) netexMapper.parseToSetPlaceEquipments(netexParking, parking);
                    parkingsList.add(parking);
                });
        return parkingsList;
    }
}
