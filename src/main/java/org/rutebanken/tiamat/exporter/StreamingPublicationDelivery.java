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

package org.rutebanken.tiamat.exporter;

import org.hibernate.Session;
import org.hibernate.internal.SessionImpl;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.validation.NeTExValidator;
import org.rutebanken.tiamat.exporter.async.*;
import org.rutebanken.tiamat.exporter.eviction.EntitiesEvictor;
import org.rutebanken.tiamat.exporter.eviction.SessionEntitiesEvictor;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.IDFMNetexIdentifiants;
import org.rutebanken.tiamat.exporter.params.IDFMVehicleModeStopPlacetypeMapping;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Provider;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.model.GroupOfStopPlaces;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static javax.xml.bind.JAXBContext.newInstance;

/**
 * Stream data objects inside already serialized publication delivery.
 * To be able to export many stop places wihtout keeping them all in memory.
 */
@Transactional(readOnly = true)
@Component
public class StreamingPublicationDelivery {

    private static final Logger logger = LoggerFactory.getLogger(StreamingPublicationDelivery.class);

    private static final JAXBContext publicationDeliveryContext = createContext(PublicationDeliveryStructure.class);
    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    private final StopPlaceRepository stopPlaceRepository;
    private final ParkingRepository parkingRepository;
    private final PublicationDeliveryExporter publicationDeliveryExporter;
    private final TiamatSiteFrameExporter tiamatSiteFrameExporter;
    private final TiamatGeneralFrameExporter tiamatGeneralFrameExporter;
    private final NetexMapper netexMapper;
    private final TariffZoneRepository tariffZoneRepository;
    private final TopographicPlaceRepository topographicPlaceRepository;
    private final GroupOfStopPlacesRepository groupOfStopPlacesRepository;
    private final NeTExValidator neTExValidator = new NeTExValidator();
    /**
     * Validate against netex schema using the {@link NeTExValidator}
     * Enabling this for large xml files can lead to high memory consumption and/or massive performance impact.
     */
    private final boolean validateAgainstSchema;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public StreamingPublicationDelivery(StopPlaceRepository stopPlaceRepository,
                                        ParkingRepository parkingRepository,
                                        PublicationDeliveryExporter publicationDeliveryExporter,
                                        TiamatSiteFrameExporter tiamatSiteFrameExporter,
                                        TiamatGeneralFrameExporter tiamatGeneralFrameExporter,
                                        NetexMapper netexMapper,
                                        TariffZoneRepository tariffZoneRepository,
                                        TopographicPlaceRepository topographicPlaceRepository,
                                        GroupOfStopPlacesRepository groupOfStopPlacesRepository,
                                        @Value("${asyncNetexExport.validateAgainstSchema:false}") boolean validateAgainstSchema) throws IOException, SAXException {
        this.stopPlaceRepository = stopPlaceRepository;
        this.parkingRepository = parkingRepository;
        this.publicationDeliveryExporter = publicationDeliveryExporter;
        this.tiamatSiteFrameExporter = tiamatSiteFrameExporter;
        this.tiamatGeneralFrameExporter = tiamatGeneralFrameExporter;
        this.netexMapper = netexMapper;
        this.tariffZoneRepository = tariffZoneRepository;
        this.topographicPlaceRepository = topographicPlaceRepository;
        this.groupOfStopPlacesRepository = groupOfStopPlacesRepository;
        this.validateAgainstSchema = validateAgainstSchema;
    }

    public void stream(ExportParams exportParams, OutputStream outputStream, Provider provider) throws JAXBException, XMLStreamException, IOException, InterruptedException, SAXException {
        stream(exportParams, outputStream, false, provider);
    }

    public void stream(ExportParams exportParams, OutputStream outputStream, boolean ignorePaging, Provider provider) throws JAXBException, XMLStreamException, IOException, InterruptedException, SAXException {

        IDFMNetexIdentifiants idfmNetexIdentifiants = new IDFMNetexIdentifiants();

        org.rutebanken.tiamat.model.GeneralFrame generalFrame = tiamatGeneralFrameExporter.createTiamatGeneralFrame(idfmNetexIdentifiants.getNameSite(provider.getName()));

        AtomicInteger mappedStopPlaceCount = new AtomicInteger();
        AtomicInteger mappedParkingCount = new AtomicInteger();
        AtomicInteger mappedTariffZonesCount = new AtomicInteger();
        AtomicInteger mappedTopographicPlacesCount = new AtomicInteger();
        AtomicInteger mappedGroupOfStopPlacesCount = new AtomicInteger();


        EntitiesEvictor entitiesEvictor = instantiateEvictor();

        logger.info("Streaming export initiated. Export params: {}", exportParams);

        // We need to know these IDs before marshalling begins.
        // To avoid marshalling empty parking element and to be able to gather relevant topographic places
        // The primary ID represents a stop place with a certain version

        final Set<Long> stopPlacePrimaryIds = stopPlaceRepository.getDatabaseIds(exportParams, ignorePaging, provider);
        logger.info("Got {} stop place IDs from stop place search", stopPlacePrimaryIds.size());

//        //TODO: stream path links, handle export mode
//        tiamatSiteFrameExporter.addRelevantPathLinks(stopPlacePrimaryIds, siteFrame);

        logger.info("Mapping general frame to netex model");
        org.rutebanken.netex.model.GeneralFrame netexGeneralFrame = netexMapper.mapToNetexModel(generalFrame);

        // On surcharge la version du generalFrame car il accepte des strings (pas dans le tiamatGeneralFrame) voir si il faut ré-adapter toute la gestion de la version
        netexGeneralFrame.setVersion("any");

        logger.info("Preparing scrollable iterators");
        // TODO vérifier l'intérêt de garder le code commenté ci-dessous dans le cas du netex IFDM (ou l'adapter si nécessaire)
//        prepareTopographicPlaces(exportParams, stopPlacePrimaryIds, mappedTopographicPlacesCount, netexSiteFrame, entitiesEvictor);
//        prepareTariffZones(exportParams, stopPlacePrimaryIds, mappedTariffZonesCount, netexSiteFrame, entitiesEvictor);
//        prepareStopPlaces(exportParams, stopPlacePrimaryIds, mappedStopPlaceCount, netexGeneralFrame, entitiesEvictor);
//        prepareParkings(exportParams, stopPlacePrimaryIds, mappedParkingCount, netexSiteFrame, entitiesEvictor);
//        prepareGroupOfStopPlaces(exportParams, stopPlacePrimaryIds, mappedGroupOfStopPlacesCount, netexSiteFrame, entitiesEvictor);
        prepareQuays(exportParams, stopPlacePrimaryIds, mappedStopPlaceCount, netexGeneralFrame, entitiesEvictor, idfmNetexIdentifiants.getNameSite(provider.getName()));

        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryExporter.createPublicationDelivery(netexGeneralFrame, idfmNetexIdentifiants.getIdSite(provider.getName()));

        Marshaller marshaller = createMarshaller();

        logger.info("Start marshalling publication delivery");
        marshaller.marshal(netexObjectFactory.createPublicationDelivery(publicationDeliveryStructure), outputStream);
        logger.info("Mapped {} stop places, {} parkings, {} topographic places, {} group of stop places and {} tariff zones to netex",
                mappedStopPlaceCount.get(),
                mappedParkingCount.get(),
                mappedTopographicPlacesCount,
                mappedGroupOfStopPlacesCount,
                mappedTariffZonesCount);
    }

    private void prepareQuays(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedStopPlaceCount, GeneralFrame netexGeneralFrame, EntitiesEvictor evicter, String providerName) {
        // Override lists with custom iterator to be able to scroll database results on the fly.
        if (!stopPlacePrimaryIds.isEmpty()) {
            logger.info("There are stop places to export");

            final Iterator<org.rutebanken.tiamat.model.StopPlace> netexStopPlaceIterator = stopPlaceRepository.scrollStopPlaces(stopPlacePrimaryIds);
            final Iterator<org.rutebanken.tiamat.model.StopPlace> tiamatStopPlaceIterator = stopPlaceRepository.scrollStopPlaces(stopPlacePrimaryIds);

            List<org.rutebanken.tiamat.model.StopPlace> tiamatStopPlaces = new ArrayList<>();

            boolean quaysExportEmpty = false;
            quaysExportEmpty = prepareTiamatQuays(stopPlacePrimaryIds, tiamatStopPlaceIterator, tiamatStopPlaces, quaysExportEmpty);

            if(!quaysExportEmpty){

                // Use Listening iterator to collect stop place IDs.
                ParentStopFetchingIterator parentStopFetchingIterator = new ParentStopFetchingIterator(netexStopPlaceIterator, stopPlaceRepository);
                NetexMappingIterator<org.rutebanken.tiamat.model.StopPlace, StopPlace> netexMappingIterator = new NetexMappingIterator<>(netexMapper, parentStopFetchingIterator, StopPlace.class, mappedStopPlaceCount, evicter);

                prepareNetexQuays(netexMappingIterator, providerName, netexGeneralFrame);

                // Code commenté car ne fonctionne que dans la génération du netex norvégien
                // List<StopPlace> stopPlaces = new NetexMappingIteratorList<>(() -> new NetexReferenceRemovingIterator(netexMappingIterator, exportParams));
            }
            else{
                netexGeneralFrame.withMembers(null);
            }

        } else {
            logger.info("No stop places to export");
        }
    }

    /**
     * Traitements sur les quays Tiamat (avant mapping avec les quays Netex)
     * @return
     */
    private boolean prepareTiamatQuays(Set<Long> stopPlacePrimaryIds, Iterator<org.rutebanken.tiamat.model.StopPlace> tiamatStopPlaceIterator, List<org.rutebanken.tiamat.model.StopPlace> tiamatStopPlaces, boolean quaysExportEmpty) {

        while (tiamatStopPlaceIterator.hasNext()) {
            tiamatStopPlaces.add(tiamatStopPlaceIterator.next());
        }

        IDFMVehicleModeStopPlacetypeMapping idfmVehicleModeStopPlacetypeMapping = new IDFMVehicleModeStopPlacetypeMapping();

        //Traitement pour valoriser correctement les données
        // Si pas de zdep on supprime le quay
        List<org.rutebanken.tiamat.model.Quay> tiamatQuays = tiamatStopPlaces
                .stream()
                .flatMap(tiamatStopPlace -> tiamatStopPlace
                        .getQuays()
                        .stream()
                        .filter(quay -> !quay.getOriginalZDEP().isEmpty())
                        .peek(quay -> quay
                                .setTransportMode(idfmVehicleModeStopPlacetypeMapping
                                        .getVehicleModeEnumeration(tiamatStopPlace.getStopPlaceType())))
                )
                .collect(Collectors.toList());

        if(!tiamatQuays.isEmpty()){
            tiamatQuays.forEach(tiamatQuay -> {
                String name = tiamatQuay
                        .getOriginalNames()
                        .stream()
                        .findFirst()
                        .orElse("");

                tiamatQuay.setName(new EmbeddableMultilingualString(name));

                String zdep = tiamatQuay
                        .getOriginalZDEP()
                        .stream()
                        .findFirst()
                        .orElse(String.valueOf(tiamatQuay.getId()));

                tiamatQuay.setNetexId("FR::Quay:" + zdep + ":FR1");

                String stopCode = tiamatQuay
                        .getOriginalStopCodes()
                        .stream()
                        .findFirst()
                        .orElse(null);

                if (stopCode != null) {
                    org.rutebanken.tiamat.model.PrivateCodeStructure privateCodeStructure = new org.rutebanken.tiamat.model.PrivateCodeStructure();
                    privateCodeStructure.setValue(stopCode);
                    tiamatQuay.setPrivateCode(privateCodeStructure);
                }
            });
        }
        else{
            quaysExportEmpty = true;
        }
        return quaysExportEmpty;
    }

    /**
     * Traitements sur les quays Netex
     */
    private void prepareNetexQuays(NetexMappingIterator<org.rutebanken.tiamat.model.StopPlace, StopPlace> netexMappingIterator, String providerName, org.rutebanken.netex.model.GeneralFrame netexGeneralFrame) {
        Quays_RelStructure quays_relStructure = new Quays_RelStructure();

        // Utilisé pour le test exportIDFM et pour en integ actuellement
        List<StopPlace> netexStopPlaces = new ArrayList<>();
        List<Quay> netexQuays = new ArrayList<>();

        while (netexMappingIterator.hasNext()) {
            netexStopPlaces.add(netexMappingIterator.next());
        }

        netexStopPlaces.forEach(netexStopPlace -> {
            if (netexStopPlace.getQuays() != null && netexStopPlace.getQuays().getQuayRefOrQuay() != null)
                netexStopPlace.getQuays().getQuayRefOrQuay().forEach(quay -> netexQuays.add((Quay) quay));
        });

        if (!netexQuays.isEmpty()) {
            setField(Quays_RelStructure.class, "quayRefOrQuay", quays_relStructure, netexQuays);

            List<JAXBElement<? extends EntityStructure>> listOfJaxbQuays = netexQuays.stream()
                    .map(quay -> {
                        quay.setVersion("any");
                        quay.withKeyList(null);

                        // TODO Conversion en lambert 2 étendu ne fonctionne pas, en discuter avec IDFM pour voir les solutions possibles
//                        LocationStructure locationOrDefault = Optional
//                                .ofNullable(quay.getCentroid().getLocation())
//                                .orElse(new LocationStructure()
//                                        .withLatitude(BigDecimal.ZERO)
//                                        .withLongitude(BigDecimal.ZERO));

//                            LambertPoint lambertPoint = Lambert.convertToLambert(locationOrDefault.getLatitude().doubleValue(), locationOrDefault.getLongitude().doubleValue(), LambertZone.LambertII);
//
//                            quay.getCentroid().setLocation(new LocationStructure()
//                                    .withPos(
//                                            new DirectPositionType()
//                                                    .withValue(lambertPoint.getX(), lambertPoint.getY())
//                                                    .withSrsName("EPSG:27572")));

                        PostalAddress postalAddress = new PostalAddress();
                        String[] zdep = quay.getId().split(":");
                        postalAddress.setId(providerName + ":Postal:" + zdep[3] + ":");
                        postalAddress.setPostalRegion("75000");
                        postalAddress.setVersion("any");
                        quay.setPostalAddress(postalAddress);

                        quay.setModification(null);

                        return netexObjectFactory.createQuay(quay);
                    })
                    .collect(Collectors.toList());

            netexGeneralFrame.getMembers().withGeneralFrameMemberOrDataManagedObjectOrEntity_Entity(listOfJaxbQuays);
        } else {
            logger.info("No quays to export");
        }
    }

    private void prepareTariffZones(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedTariffZonesCount, SiteFrame netexSiteFrame, EntitiesEvictor evicter) {


        Iterator<org.rutebanken.tiamat.model.TariffZone> tariffZoneIterator;
        if (exportParams.getTariffZoneExportMode() == null || exportParams.getTariffZoneExportMode().equals(ExportParams.ExportMode.ALL)) {

            logger.info("Preparing to scroll all tariff zones, regardless of version");
            tariffZoneIterator = tariffZoneRepository.scrollTariffZones();
        } else if (exportParams.getTariffZoneExportMode().equals(ExportParams.ExportMode.RELEVANT)) {

            logger.info("Preparing to scroll relevant tariff zones from stop place ids");
            tariffZoneIterator = tariffZoneRepository.scrollTariffZones(stopPlacePrimaryIds);
        } else {
            logger.info("Tariff zone export mode is {}. Will not export tariff zones", exportParams.getTariffZoneExportMode());
            tariffZoneIterator = Collections.emptyIterator();
        }

        if (tariffZoneIterator.hasNext()) {
            NetexMappingIterator<org.rutebanken.tiamat.model.TariffZone, TariffZone> tariffZoneMappingIterator =
                    new NetexMappingIterator<>(netexMapper, tariffZoneIterator, TariffZone.class, mappedTariffZonesCount, evicter);

            List<TariffZone> tariffZones = new NetexMappingIteratorList<>(() -> tariffZoneMappingIterator);

            TariffZonesInFrame_RelStructure tariffZonesInFrame_relStructure = new TariffZonesInFrame_RelStructure();
            setField(TariffZonesInFrame_RelStructure.class, "tariffZone", tariffZonesInFrame_relStructure, tariffZones);
            netexSiteFrame.setTariffZones(tariffZonesInFrame_relStructure);
        } else {
            logger.info("No tariff zones to export");
            netexSiteFrame.setTariffZones(null);
        }

    }

    private void prepareParkings(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedParkingCount, SiteFrame netexSiteFrame, EntitiesEvictor evicter) {

        // ExportParams could be used for parkingExportMode.

        int parkingsCount = parkingRepository.countResult(stopPlacePrimaryIds);
        if (parkingsCount > 0) {
            // Only set parkings if they will exist during marshalling.
            logger.info("Parking count is {}, will create parking in publication delivery", parkingsCount);
            ParkingsInFrame_RelStructure parkingsInFrame_relStructure = new ParkingsInFrame_RelStructure();
            List<Parking> parkings = new NetexMappingIteratorList<>(() -> new NetexMappingIterator<>(netexMapper, parkingRepository.scrollParkings(stopPlacePrimaryIds),
                    Parking.class, mappedParkingCount, evicter));

            setField(ParkingsInFrame_RelStructure.class, "parking", parkingsInFrame_relStructure, parkings);
            netexSiteFrame.setParkings(parkingsInFrame_relStructure);
        } else {
            logger.info("No parkings to export based on stop places");
        }
    }

    private void prepareStopPlaces(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedStopPlaceCount, GeneralFrame netexGeneralFrame, EntitiesEvictor evicter) {
        // Override lists with custom iterator to be able to scroll database results on the fly.
        if (!stopPlacePrimaryIds.isEmpty()) {
            logger.info("There are stop places to export");

            final Iterator<org.rutebanken.tiamat.model.StopPlace> stopPlaceIterator = stopPlaceRepository.scrollStopPlaces(stopPlacePrimaryIds);
            StopPlacesInFrame_RelStructure stopPlacesInFrame_relStructure = new StopPlacesInFrame_RelStructure();

            // Use Listening iterator to collect stop place IDs.
            ParentStopFetchingIterator parentStopFetchingIterator = new ParentStopFetchingIterator(stopPlaceIterator, stopPlaceRepository);
            NetexMappingIterator<org.rutebanken.tiamat.model.StopPlace, StopPlace> netexMappingIterator = new NetexMappingIterator<>(netexMapper, parentStopFetchingIterator, StopPlace.class, mappedStopPlaceCount, evicter);

            List<StopPlace> stopPlaces = new ArrayList<>();
            while (netexMappingIterator.hasNext()) {
                stopPlaces.add(netexMappingIterator.next());
            }
//            List<StopPlace> stopPlaces = new NetexMappingIteratorList<>(() -> new NetexReferenceRemovingIterator(netexMappingIterator, exportParams));


            setField(StopPlacesInFrame_RelStructure.class, "stopPlace", stopPlacesInFrame_relStructure, stopPlaces);
            List<JAXBElement<? extends EntityStructure>> listOfJaxbStopPlaces = stopPlaces.stream().map(sp -> netexObjectFactory.createStopPlace(sp)).collect(Collectors.toList());
            netexGeneralFrame.getMembers().withGeneralFrameMemberOrDataManagedObjectOrEntity_Entity(listOfJaxbStopPlaces);
        } else {
            logger.info("No stop places to export");
        }
    }

    private void prepareTopographicPlaces(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedTopographicPlacesCount, SiteFrame netexSiteFrame, EntitiesEvictor evicter) {

        Iterator<TopographicPlace> relevantTopographicPlacesIterator;

        if (exportParams.getTopographicPlaceExportMode() == null || exportParams.getTopographicPlaceExportMode().equals(ExportParams.ExportMode.ALL)) {
            logger.info("Prepare scrolling for all topographic places");
            relevantTopographicPlacesIterator = topographicPlaceRepository.scrollTopographicPlaces();

        } else if (exportParams.getTopographicPlaceExportMode().equals(ExportParams.ExportMode.RELEVANT)) {
            logger.info("Prepare scrolling relevant topographic places");
            relevantTopographicPlacesIterator = new ParentTreeTopographicPlaceFetchingIterator(topographicPlaceRepository.scrollTopographicPlaces(stopPlacePrimaryIds), topographicPlaceRepository);
        } else {
            logger.info("Topographic export mode is {}. Will not export topographic places", exportParams.getTopographicPlaceExportMode());
            relevantTopographicPlacesIterator = Collections.emptyIterator();
        }

        if (relevantTopographicPlacesIterator.hasNext()) {

            NetexMappingIterator<TopographicPlace, org.rutebanken.netex.model.TopographicPlace> topographicPlaceNetexMappingIterator = new NetexMappingIterator<>(
                    netexMapper, relevantTopographicPlacesIterator, org.rutebanken.netex.model.TopographicPlace.class, mappedTopographicPlacesCount, evicter);

            List<org.rutebanken.netex.model.TopographicPlace> topographicPlaces = new NetexMappingIteratorList<>(() -> topographicPlaceNetexMappingIterator);

            TopographicPlacesInFrame_RelStructure topographicPlacesInFrame_relStructure = new TopographicPlacesInFrame_RelStructure();
            setField(TopographicPlacesInFrame_RelStructure.class, "topographicPlace", topographicPlacesInFrame_relStructure, topographicPlaces);
            netexSiteFrame.setTopographicPlaces(topographicPlacesInFrame_relStructure);
        } else {
            netexSiteFrame.setTopographicPlaces(null);
        }
    }

    private void prepareGroupOfStopPlaces(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedGroupOfStopPlacesCount, SiteFrame netexSiteFrame, EntitiesEvictor evicter) {

        Iterator<GroupOfStopPlaces> groupOfStopPlacesIterator;

        if (exportParams.getGroupOfStopPlacesExportMode() == null || exportParams.getGroupOfStopPlacesExportMode().equals(ExportParams.ExportMode.ALL)) {
            logger.info("Prepare scrolling for all group of stop places");
            groupOfStopPlacesIterator = groupOfStopPlacesRepository.scrollGroupOfStopPlaces();

        } else if (exportParams.getGroupOfStopPlacesExportMode().equals(ExportParams.ExportMode.RELEVANT)) {
            logger.info("Prepare scrolling relevant group of stop places");
            groupOfStopPlacesIterator = groupOfStopPlacesRepository.scrollGroupOfStopPlaces(stopPlacePrimaryIds);
        } else {
            logger.info("Group of stop places export mode is {}. Will not export group of stop places", exportParams.getGroupOfStopPlacesExportMode());
            groupOfStopPlacesIterator = Collections.emptyIterator();
        }

        if (groupOfStopPlacesIterator.hasNext()) {

            NetexMappingIterator<GroupOfStopPlaces, org.rutebanken.netex.model.GroupOfStopPlaces> netexMappingIterator = new NetexMappingIterator<>(
                    netexMapper, groupOfStopPlacesIterator, org.rutebanken.netex.model.GroupOfStopPlaces.class, mappedGroupOfStopPlacesCount, evicter);

            List<org.rutebanken.netex.model.GroupOfStopPlaces> groupOfStopPlacesList = new NetexMappingIteratorList<>(() -> netexMappingIterator);

            GroupsOfStopPlacesInFrame_RelStructure groupsOfStopPlacesInFrame_relStructure = new GroupsOfStopPlacesInFrame_RelStructure();
            setField(GroupsOfStopPlacesInFrame_RelStructure.class, "groupOfStopPlaces", groupsOfStopPlacesInFrame_relStructure, groupOfStopPlacesList);
            netexSiteFrame.setGroupsOfStopPlaces(groupsOfStopPlacesInFrame_relStructure);
        } else {
            netexSiteFrame.setGroupsOfStopPlaces(null);
        }
    }

    private EntitiesEvictor instantiateEvictor() {
        if (entityManager != null) {
            Session currentSession = entityManager.unwrap(Session.class);
            return new SessionEntitiesEvictor((SessionImpl) currentSession);
        } else {
            return new EntitiesEvictor() {
                @Override
                public void evictKnownEntitiesFromSession(Object entity) {
                    // Intentionally left blank
                }
            };
        }
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
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("Cannot set field " + fieldName + " of " + instance, e);
        }
    }

    private static JAXBContext createContext(Class clazz) {
        try {
            JAXBContext jaxbContext = newInstance(clazz);
            logger.info("Created context {}", jaxbContext.getClass());
            return jaxbContext;
        } catch (JAXBException e) {
            String message = "Could not create instance of jaxb context for class " + clazz;
            logger.warn(message, e);
            throw new RuntimeException("Could not create instance of jaxb context for class " + clazz, e);
        }
    }

    private Marshaller createMarshaller() throws JAXBException, IOException, SAXException {
        Marshaller marshaller = publicationDeliveryContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "");

        if (validateAgainstSchema) {
            marshaller.setSchema(neTExValidator.getSchema());
        }

        return marshaller;
    }
}
