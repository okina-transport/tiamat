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

package org.rutebanken.tiamat.importer;

import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.exporter.PublicationDeliveryExporter;
import org.rutebanken.tiamat.importer.handler.ParkingsImportHandler;
import org.rutebanken.tiamat.importer.handler.StopPlaceImportHandler;
import org.rutebanken.tiamat.importer.log.ImportLogger;
import org.rutebanken.tiamat.importer.log.ImportLoggerTask;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PublicationDeliveryImporter {

    private static final Logger logger = LoggerFactory.getLogger(PublicationDeliveryImporter.class);

    public static final String IMPORT_CORRELATION_ID = "importCorrelationId";

    private final PublicationDeliveryHelper publicationDeliveryHelper;
    private final PublicationDeliveryExporter publicationDeliveryExporter;
    private final NetexMapper netexMapper;
    private final PathLinksImporter pathLinksImporter;
    private final TopographicPlaceImporter topographicPlaceImporter;
    private final TariffZoneImporter tariffZoneImporter;
    private final StopPlaceImportHandler stopPlaceImportHandler;
    private final ParkingsImportHandler parkingsImportHandler;

    @Autowired
    public PublicationDeliveryImporter(PublicationDeliveryHelper publicationDeliveryHelper, NetexMapper netexMapper,
                                       PublicationDeliveryExporter publicationDeliveryExporter,
                                       PathLinksImporter pathLinksImporter,
                                       TopographicPlaceImporter topographicPlaceImporter,
                                       TariffZoneImporter tariffZoneImporter,
                                       StopPlaceImportHandler stopPlaceImportHandler,
                                       ParkingsImportHandler parkingsImportHandler) {
        this.publicationDeliveryHelper = publicationDeliveryHelper;
        this.netexMapper = netexMapper;
        this.parkingsImportHandler = parkingsImportHandler;
        this.publicationDeliveryExporter = publicationDeliveryExporter;
        this.pathLinksImporter = pathLinksImporter;
        this.topographicPlaceImporter = topographicPlaceImporter;
        this.tariffZoneImporter = tariffZoneImporter;
        this.stopPlaceImportHandler = stopPlaceImportHandler;
    }


    public PublicationDeliveryStructure importPublicationDelivery(PublicationDeliveryStructure incomingPublicationDelivery) {
        return importPublicationDelivery(incomingPublicationDelivery, null);
    }

    @SuppressWarnings("unchecked")
    public PublicationDeliveryStructure importPublicationDelivery(PublicationDeliveryStructure incomingPublicationDelivery, ImportParams importParams) {
        if (incomingPublicationDelivery.getDataObjects() == null) {
            String responseMessage = "Received publication delivery but it does not contain any data objects.";
            logger.warn(responseMessage);
            throw new RuntimeException(responseMessage);
        }

        if (importParams == null) {
            importParams = new ImportParams();
        } else {
            validate(importParams);
        }

        logger.info("Got publication delivery with {} site frames", incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame().size());

        AtomicInteger stopPlacesCreatedOrUpdated = new AtomicInteger(0);
        AtomicInteger parkingsCreatedOrUpdated = new AtomicInteger(0);
        AtomicInteger topographicPlacesCounter = new AtomicInteger(0);
        SiteFrame netexSiteFrame = publicationDeliveryHelper.findSiteFrame(incomingPublicationDelivery);

        String requestId = netexSiteFrame.getId();

        Timer loggerTimer = new ImportLogger(new ImportLoggerTask(stopPlacesCreatedOrUpdated, publicationDeliveryHelper.numberOfStops(netexSiteFrame), topographicPlacesCounter, netexSiteFrame.getId()));

        try {
            SiteFrame responseSiteframe = new SiteFrame();

            MDC.put(IMPORT_CORRELATION_ID, requestId);
            logger.info("Publication delivery contains site frame created at {}", netexSiteFrame.getCreated());

            responseSiteframe.withId(requestId + "-response").withVersion("1");

            if (publicationDeliveryHelper.hasTopographicPlaces(netexSiteFrame)) {
                logger.info("Publication delivery contains {} topographic places for import.", netexSiteFrame.getTopographicPlaces().getTopographicPlace().size());

                logger.info("About to map {} topographic places to internal model", netexSiteFrame.getTopographicPlaces().getTopographicPlace().size());
                List<org.rutebanken.tiamat.model.TopographicPlace> mappedTopographicPlaces = netexMapper.getFacade()
                        .mapAsList(netexSiteFrame.getTopographicPlaces().getTopographicPlace(),
                                org.rutebanken.tiamat.model.TopographicPlace.class);
                logger.info("Mapped {} topographic places to internal model", mappedTopographicPlaces.size());
                List<TopographicPlace> importedTopographicPlaces = topographicPlaceImporter.importTopographicPlaces(mappedTopographicPlaces, topographicPlacesCounter);
                responseSiteframe.withTopographicPlaces(new TopographicPlacesInFrame_RelStructure().withTopographicPlace(importedTopographicPlaces));
                logger.info("Finished importing topographic places");
            }

            if (publicationDeliveryHelper.hasTariffZones(netexSiteFrame) && importParams.importType != ImportType.ID_MATCH) {
                List<org.rutebanken.tiamat.model.TariffZone> tiamatTariffZones = netexMapper.getFacade().mapAsList(netexSiteFrame.getTariffZones().getTariffZone(), org.rutebanken.tiamat.model.TariffZone.class);
                logger.debug("Mapped {} tariff zones from netex to internal model", tiamatTariffZones.size());
                List<TariffZone> importedTariffZones = tariffZoneImporter.importTariffZones(tiamatTariffZones);
                logger.debug("Got {} imported tariffZones ", importedTariffZones.size());
                if (!importedTariffZones.isEmpty()) {
                    responseSiteframe.withTariffZones(new TariffZonesInFrame_RelStructure().withTariffZone(importedTariffZones));
                }
            }

            stopPlaceImportHandler.handleStops(netexSiteFrame, importParams, stopPlacesCreatedOrUpdated, responseSiteframe);
            parkingsImportHandler.handleParkings(netexSiteFrame, importParams, parkingsCreatedOrUpdated, responseSiteframe);

            if (netexSiteFrame.getPathLinks() != null && netexSiteFrame.getPathLinks().getPathLink() != null) {
                List<org.rutebanken.tiamat.model.PathLink> tiamatPathLinks = netexMapper.mapPathLinksToTiamatModel(netexSiteFrame.getPathLinks().getPathLink());
                tiamatPathLinks.forEach(tiamatPathLink -> logger.debug("Received path link: {}", tiamatPathLink));

                List<org.rutebanken.netex.model.PathLink> pathLinks = pathLinksImporter.importPathLinks(tiamatPathLinks);
                responseSiteframe.withPathLinks(new PathLinksInFrame_RelStructure().withPathLink(pathLinks));
            }

            return publicationDeliveryExporter.createPublicationDelivery(responseSiteframe);
        } finally {
            MDC.remove(IMPORT_CORRELATION_ID);
            loggerTimer.cancel();
        }
    }

    private void validate(ImportParams importParams) {
        if (importParams.targetTopographicPlaces != null && importParams.onlyMatchOutsideTopographicPlaces != null) {
            if (!importParams.targetTopographicPlaces.isEmpty() && !importParams.onlyMatchOutsideTopographicPlaces.isEmpty()) {
                throw new IllegalArgumentException("targetTopographicPlaces and targetTopographicPlaces cannot be specified at the same time!");
            }
        }
    }

}
