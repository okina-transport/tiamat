package org.rutebanken.tiamat.importer;

import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.CompositeFrame;
import org.rutebanken.netex.model.PathLinksInFrame_RelStructure;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.StopPlacesInFrame_RelStructure;
import org.rutebanken.netex.model.TopographicPlace;
import org.rutebanken.tiamat.exporter.PublicationDeliveryExporter;
import org.rutebanken.tiamat.exporter.TopographicPlacesExporter;
import org.rutebanken.tiamat.importer.log.ImportLogger;
import org.rutebanken.tiamat.importer.log.ImportLoggerTask;
import org.rutebanken.tiamat.importer.modifier.StopPlacePreSteps;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class PublicationDeliveryImporter {

    private static final Logger logger = LoggerFactory.getLogger(PublicationDeliveryImporter.class);

    public static final String IMPORT_CORRELATION_ID = "importCorrelationId";
    private static final Object STOP_PLACE_IMPORT_LOCK = new Object();

    private final TransactionalStopPlacesImporter transactionalStopPlacesImporter;
    private final PublicationDeliveryExporter publicationDeliveryExporter;
    private final NetexMapper netexMapper;
    private final StopPlacePreSteps stopPlacePreSteps;
    private final PathLinksImporter pathLinksImporter;
    private final TopographicPlacesExporter topographicPlacesExporter;
    private final TopographicPlaceImporter topographicPlaceImporter;


    @Autowired
    public PublicationDeliveryImporter(NetexMapper netexMapper,
                                       TransactionalStopPlacesImporter transactionalStopPlacesImporter,
                                       PublicationDeliveryExporter publicationDeliveryExporter,
                                       StopPlacePreSteps stopPlacePreSteps,
                                       PathLinksImporter pathLinksImporter,
                                       TopographicPlacesExporter topographicPlacesExporter, TopographicPlaceImporter topographicPlaceImporter) {
        this.netexMapper = netexMapper;
        this.transactionalStopPlacesImporter = transactionalStopPlacesImporter;
        this.publicationDeliveryExporter = publicationDeliveryExporter;
        this.stopPlacePreSteps = stopPlacePreSteps;
        this.pathLinksImporter = pathLinksImporter;
        this.topographicPlacesExporter = topographicPlacesExporter;
        this.topographicPlaceImporter = topographicPlaceImporter;
    }


    @SuppressWarnings("unchecked")
    public PublicationDeliveryStructure importPublicationDelivery(PublicationDeliveryStructure incomingPublicationDelivery) {
        if (incomingPublicationDelivery.getDataObjects() == null) {
            String responseMessage = "Received publication delivery but it does not contain any data objects.";
            logger.warn(responseMessage);
            throw new RuntimeException(responseMessage);
        }
        logger.info("Got publication delivery with {} site frames", incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame().size());

        AtomicInteger stopPlacesCreated = new AtomicInteger(0);
        AtomicInteger topographicPlacesCounter = new AtomicInteger(0);
        SiteFrame netexSiteFrame = findSiteFrame(incomingPublicationDelivery);

        String requestId = netexSiteFrame.getId();

        Timer loggerTimer = new ImportLogger(new ImportLoggerTask(stopPlacesCreated, numberOfStops(netexSiteFrame), topographicPlacesCounter, netexSiteFrame.getId()));

        try {
            SiteFrame responseSiteframe = new SiteFrame();

            MDC.put(IMPORT_CORRELATION_ID, requestId);
            logger.info("Publication delivery contains site frame created at {}", netexSiteFrame.getCreated());

            responseSiteframe.withId(requestId + "-response").withVersion("1");

            if(hasTopographicPlaces(netexSiteFrame)) {
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


            if(hasStops(netexSiteFrame)) {
                List<org.rutebanken.tiamat.model.StopPlace> tiamatStops = netexMapper.mapStopsToTiamatModel(netexSiteFrame.getStopPlaces().getStopPlace());
                tiamatStops = stopPlacePreSteps.run(tiamatStops, topographicPlacesCounter);

                Collection<org.rutebanken.netex.model.StopPlace> stopPlaces;
                synchronized (STOP_PLACE_IMPORT_LOCK) {
                    stopPlaces = transactionalStopPlacesImporter.importStopPlaces(tiamatStops, stopPlacesCreated);
                }
                logger.info("Saved {} stop places", stopPlacesCreated);

                responseSiteframe.withStopPlaces(
                        new StopPlacesInFrame_RelStructure()
                                .withStopPlace(stopPlaces));

                // Find topographic places from imported stops
                List<Pair<String, Long>> topographicPlaceRefs = tiamatStops
                        .stream()
                        .filter(stopPlace -> stopPlace.getTopographicPlace() != null)
                        .map(org.rutebanken.tiamat.model.StopPlace::getTopographicPlace)
                        .map(topographicPlace -> Pair.of(topographicPlace.getNetexId(), topographicPlace.getVersion()))
                        .distinct()
                        .collect(Collectors.toList());

                List<TopographicPlace> netexTopographicPlaces = topographicPlacesExporter.export(topographicPlaceRefs);

                if (!netexTopographicPlaces.isEmpty()) {
                    responseSiteframe.withTopographicPlaces(
                            new TopographicPlacesInFrame_RelStructure()
                                    .withTopographicPlace(netexTopographicPlaces));
                }
            }

            if(netexSiteFrame.getPathLinks() != null && netexSiteFrame.getPathLinks().getPathLink() != null) {
                List<org.rutebanken.tiamat.model.PathLink> tiamatPathLinks = netexMapper.mapPathLinksToTiamatModel(netexSiteFrame.getPathLinks().getPathLink());
                tiamatPathLinks.forEach(tiamatPathLink -> logger.debug("Received path link: {}", tiamatPathLink));

                List<org.rutebanken.netex.model.PathLink> pathLinks = pathLinksImporter.importPathLinks(tiamatPathLinks);
                responseSiteframe.withPathLinks(new PathLinksInFrame_RelStructure().withPathLink(pathLinks));
            }


            return publicationDeliveryExporter.exportSiteFrame(responseSiteframe);
        } finally {
            MDC.remove(IMPORT_CORRELATION_ID);
            loggerTimer.cancel();
        }
    }

    private boolean hasTopographicPlaces(SiteFrame netexSiteFrame) {
        return netexSiteFrame.getTopographicPlaces() != null
                && netexSiteFrame.getTopographicPlaces().getTopographicPlace() != null
                && !netexSiteFrame.getTopographicPlaces().getTopographicPlace().isEmpty();
    }

    private boolean hasStops(SiteFrame siteFrame) {
        return siteFrame.getStopPlaces() != null && siteFrame.getStopPlaces().getStopPlace() != null;
    }

    private int numberOfStops(SiteFrame netexSiteFrame) {
        return hasStops(netexSiteFrame) ? netexSiteFrame.getStopPlaces().getStopPlace().size() : 0;
    }

    public SiteFrame findSiteFrame(PublicationDeliveryStructure incomingPublicationDelivery) {

        List<JAXBElement<? extends Common_VersionFrameStructure>> compositeFrameOrCommonFrame = incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();

        Optional<SiteFrame> optionalSiteframe = compositeFrameOrCommonFrame
                .stream()
                .filter(element -> element.getValue() instanceof SiteFrame)
                .map(element -> (SiteFrame) element.getValue())
                .findFirst();

        if (optionalSiteframe.isPresent()) {
            return optionalSiteframe.get();
        }

        return compositeFrameOrCommonFrame
                .stream()
                .filter(element -> element.getValue() instanceof CompositeFrame)
                .map(element -> (CompositeFrame) element.getValue())
                .map(compositeFrame -> compositeFrame.getFrames())
                .flatMap(frames -> frames.getCommonFrame().stream())
                .filter(jaxbElement -> jaxbElement.getValue() instanceof SiteFrame)
                .map(jaxbElement -> (SiteFrame) jaxbElement.getValue())
                .findAny().get();
    }
}
