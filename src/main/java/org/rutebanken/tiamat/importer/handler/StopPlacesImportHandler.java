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
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ImportType;
import org.rutebanken.tiamat.importer.filter.ZoneTopographicPlaceFilter;
import org.rutebanken.tiamat.importer.merging.TransactionalMergingStopPlacesImporter;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.netex.NetexUtils;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class StopPlacesImportHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StopPlacesImportHandler.class);
    /**
     * Hazelcast lock key for merging stop place import.
     */
    private static final String PARKING_IMPORT_LOCK_KEY = "STOP_PLACE_MERGING_IMPORT_LOCK_KEY";

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    private ZoneTopographicPlaceFilter zoneTopographicPlaceFilter;

    @Autowired
    private TransactionalMergingStopPlacesImporter transactionalMergingStopPlacesImporter;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    public void handleStopPlacesGeneralFrame(List<org.rutebanken.netex.model.StopPlace> tiamatStopPlaces, ImportParams importParams,
                                             List<JAXBElement<? extends EntityStructure>> members, AtomicInteger atomicInteger,
                                             List<org.rutebanken.tiamat.model.Quay> quaysParsed) {
        List<StopPlace> stopPlacesParsed = mapStopPlacesToTiamatModel(tiamatStopPlaces, quaysParsed);

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
                importedStopPlaces = transactionalMergingStopPlacesImporter.importStopPlaces(stopPlacesParsed, atomicInteger);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        } else if (importParams.importType.equals(ImportType.INITIAL)) {
            importedStopPlaces = transactionalMergingStopPlacesImporter.importStopPlaces(stopPlacesParsed, atomicInteger);
        } else {
            logger.warn("Import type " + importParams.importType + " not implemented. Will not match stop places.");
            importedStopPlaces = new ArrayList<>(0);
        }

        if (!importedStopPlaces.isEmpty()) {
            NetexUtils.getMembers(org.rutebanken.netex.model.StopPlace.class, members);
        }

        logger.info("Mapped {} stop places !!", tiamatStopPlaces.size());
    }

    private List<StopPlace> mapStopPlacesToTiamatModel(List<org.rutebanken.netex.model.StopPlace> netexStopPlacesInFrame, List<org.rutebanken.tiamat.model.Quay> quaysParsed) {
        if (netexStopPlacesInFrame.isEmpty())
            return null;

        List<StopPlace> stopPlacesList = new ArrayList<>();
        netexStopPlacesInFrame.forEach(netexStopPlace -> {
            stopPlacesList.add(netexMapper.parseToTiamatStopPlace(netexStopPlace, quaysParsed));
        });
        return stopPlacesList;
    }
}
