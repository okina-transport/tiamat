package org.rutebanken.tiamat.exporter;

import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.Parking;
import org.rutebanken.netex.model.ParkingsInFrame_RelStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopPlacesInFrame_RelStructure;
import org.rutebanken.tiamat.exporter.async.NetexMappingIterator;
import org.rutebanken.tiamat.exporter.async.NetexMappingIteratorList;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static javax.xml.bind.JAXBContext.newInstance;

/**
 * Stream data objects inside already serialized publication delivery.
 * To be able to export many stop places wihtout keeping them all in memory.
 */
@Transactional
@Component
public class StreamingPublicationDelivery {

    private static final Logger logger = LoggerFactory.getLogger(StreamingPublicationDelivery.class);

    private static final JAXBContext publicationDeliveryContext = createContext(PublicationDeliveryStructure.class);
    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    private final PublicationDeliveryHelper publicationDeliveryHelper;


    private final StopPlaceRepository stopPlaceRepository;
    private final ParkingRepository parkingRepository;
    private final PublicationDeliveryExporter publicationDeliveryExporter;
    private final TiamatSiteFrameExporter tiamatSiteFrameExporter;
    private final TopographicPlacesExporter topographicPlacesExporter;
    private final NetexMapper netexMapper;
    private final TariffZoneRepository tariffZoneRepository;
    private final TopographicPlaceRepository topographicPlaceRepository;
    private final PathLinkRepository pathLinkRepository;


    @Autowired
    public StreamingPublicationDelivery(PublicationDeliveryHelper publicationDeliveryHelper,
                                        StopPlaceRepository stopPlaceRepository,
                                        ParkingRepository parkingRepository,
                                        PublicationDeliveryExporter publicationDeliveryExporter,
                                        TiamatSiteFrameExporter tiamatSiteFrameExporter,
                                        TopographicPlacesExporter topographicPlacesExporter,
                                        NetexMapper netexMapper,
                                        TariffZoneRepository tariffZoneRepository, TopographicPlaceRepository topographicPlaceRepository, PathLinkRepository pathLinkRepository) {
        this.publicationDeliveryHelper = publicationDeliveryHelper;
        this.stopPlaceRepository = stopPlaceRepository;
        this.parkingRepository = parkingRepository;
        this.publicationDeliveryExporter = publicationDeliveryExporter;
        this.tiamatSiteFrameExporter = tiamatSiteFrameExporter;
        this.topographicPlacesExporter = topographicPlacesExporter;
        this.netexMapper = netexMapper;
        this.tariffZoneRepository = tariffZoneRepository;
        this.topographicPlaceRepository = topographicPlaceRepository;
        this.pathLinkRepository = pathLinkRepository;
    }
    public void stream(ExportParams exportParams, OutputStream outputStream) throws JAXBException, XMLStreamException, IOException, InterruptedException {

        org.rutebanken.tiamat.model.SiteFrame siteFrame = tiamatSiteFrameExporter.createTiamatSiteFrame("Site frame "+exportParams);

        logger.info("Async export initiated. Export params: {}", exportParams);

        // We need to know these IDs before marshalling begins.
        // To avoid marshalling empty parking element and to be able to gather relevant topographic places
        // The primary ID represents a stop place with a certain version

        final Set<Long> stopPlacePrimaryIds = stopPlaceRepository.getDatabaseIds(exportParams);
        logger.info("Got {} stop place IDs from stop place search", stopPlacePrimaryIds.size());

        if(exportParams.getTopographicPlaceExportMode() == null || exportParams.getTopographicPlaceExportMode().equals(ExportParams.ExportMode.ALL)) {
            topographicPlacesExporter.addTopographicPlacesToTiamatSiteFrame(ExportParams.ExportMode.ALL, siteFrame);
        } else if(exportParams.getTopographicPlaceExportMode().equals(ExportParams.ExportMode.RELEVANT)) {
            List<TopographicPlace> relevantTopographicPlaces = topographicPlaceRepository.getTopographicPlacesFromStopPlaceIds(stopPlacePrimaryIds);
            Set<TopographicPlace> target = new HashSet<>();
            for(TopographicPlace topographicPlace : relevantTopographicPlaces) {
                topographicPlacesExporter.gatherTopographicPlaceTree(topographicPlace, target);
            }
            topographicPlacesExporter.addTopographicPlacesToTiamatSiteFrame(target, siteFrame);
        }

        List<org.rutebanken.tiamat.model.TariffZone> tariffZones;
        if(exportParams.getTariffZoneExportMode() == null || exportParams.getTariffZoneExportMode().equals(ExportParams.ExportMode.ALL)) {
            tariffZones = tariffZoneRepository.findAll();
            logger.info("Added all tariff zones, regardless of version: {}", tariffZones.size());

        } else {
            tariffZones = tariffZoneRepository.getTariffZonesFromStopPlaceIds(stopPlacePrimaryIds);
            if (tariffZones != null) {
                logger.info("Got {} tariff zones from {} stop place ids", tariffZones.size(), stopPlacePrimaryIds.size());
                tiamatSiteFrameExporter.addTariffZones(siteFrame, tariffZones);
            }
        }

        tiamatSiteFrameExporter.addRelevantPathLinks(stopPlacePrimaryIds, siteFrame);

        logger.info("Mapping site frame to netex model");
        org.rutebanken.netex.model.SiteFrame netexSiteFrame = netexMapper.mapToNetexModel(siteFrame);

        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryExporter.createPublicationDelivery(netexSiteFrame);

        // Override lists with custom iterator to be able to scroll database results on the fly.
        if(!stopPlacePrimaryIds.isEmpty()) {
            final Iterator<org.rutebanken.tiamat.model.StopPlace> stopPlaceIterator = stopPlaceRepository.scrollStopPlaces(exportParams);
            logger.info("There are stop places to export");
            StopPlacesInFrame_RelStructure stopPlacesInFrame_relStructure = new StopPlacesInFrame_RelStructure();

            // Use Listening iterator to collect stop place IDs.
            List<StopPlace> stopPlaces = new NetexMappingIteratorList<>(() -> new NetexMappingIterator<>(netexMapper, stopPlaceIterator, StopPlace.class));
            setField(StopPlacesInFrame_RelStructure.class, "stopPlace", stopPlacesInFrame_relStructure, stopPlaces);
            netexSiteFrame.setStopPlaces(stopPlacesInFrame_relStructure);
        } else {
            logger.info("No stop places to export");
        }

        int parkingsCount = parkingRepository.countResult(stopPlacePrimaryIds);
        if(parkingsCount > 0) {
            // Only set parkings if they will exist during marshalling.
            logger.info("Parking count is {}, will create parking in publication delivery", parkingsCount);
            ParkingsInFrame_RelStructure parkingsInFrame_relStructure = new ParkingsInFrame_RelStructure();
            List<Parking> parkings = new NetexMappingIteratorList<>(() -> new NetexMappingIterator<>(netexMapper, parkingRepository.scrollParkings(stopPlacePrimaryIds), Parking.class));

            setField(ParkingsInFrame_RelStructure.class, "parking", parkingsInFrame_relStructure, parkings);
            netexSiteFrame.setParkings(parkingsInFrame_relStructure);
        } else {
            logger.info("No parkings to export based on stop places");
        }

        Marshaller marshaller = createMarshaller();

        marshaller.marshal(netexObjectFactory.createPublicationDelivery(publicationDeliveryStructure), outputStream);
    }

    /**
     * Set field value with reflection.
     * Used for setting list values in netex model.
     */
    private void setField(Class clazz, String fieldName, Object instance, Object fieldValue) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, fieldValue);
        } catch (IllegalAccessException|NoSuchFieldException e) {
            throw new RuntimeException("Cannot set field "+fieldName +" of "+instance, e);
        }
    }

    private static JAXBContext createContext(Class clazz) {
        try {
            return newInstance(clazz);
        } catch (JAXBException e) {
            logger.warn("Could not create instance of jaxb context for class " + clazz, e);
            throw new RuntimeException(e);
        }
    }

    private Marshaller createMarshaller() throws JAXBException {
        Marshaller stopPlaceMarshaller = publicationDeliveryContext.createMarshaller();
        stopPlaceMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        stopPlaceMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        stopPlaceMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "");
        return stopPlaceMarshaller;
    }
}
