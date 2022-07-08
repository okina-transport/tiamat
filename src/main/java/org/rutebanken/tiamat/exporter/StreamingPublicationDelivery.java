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

import net.opengis.gml._3.DirectPositionType;
import org.hibernate.Session;
import org.hibernate.internal.SessionImpl;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.AccessibilityLimitation;
import org.rutebanken.netex.model.AccessibilityLimitations_RelStructure;
import org.rutebanken.netex.model.CountryRef;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.GeneralFrame;
import org.rutebanken.netex.model.GeneralOrganisation;
import org.rutebanken.netex.model.General_VersionFrameStructure;
import org.rutebanken.netex.model.GroupsOfStopPlacesInFrame_RelStructure;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.LimitationStatusEnumeration;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.OrganisationRefStructure;
import org.rutebanken.netex.model.OrganisationTypeEnumeration;
import org.rutebanken.netex.model.Parking;
import org.rutebanken.netex.model.PointOfInterest;
import org.rutebanken.netex.model.PointOfInterestClassificationsInFrame_RelStructure;
import org.rutebanken.netex.model.PointsOfInterestInFrame_RelStructure;
import org.rutebanken.netex.model.PostalAddress;
import org.rutebanken.netex.model.PrivateCodeStructure;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.ResponsibilityRoleAssignment_VersionedChildStructure;
import org.rutebanken.netex.model.ResponsibilityRoleAssignments_RelStructure;
import org.rutebanken.netex.model.ResponsibilitySet;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.SiteRefStructure;
import org.rutebanken.netex.model.Site_VersionFrameStructure;
import org.rutebanken.netex.model.StakeholderRoleTypeEnumeration;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TariffZone;
import org.rutebanken.netex.model.TypeOfParking;
import org.rutebanken.netex.model.TypeOfPlaceRefStructure;
import org.rutebanken.netex.model.TypeOfPlaceRefs_RelStructure;
import org.rutebanken.netex.model.Zone_VersionStructure;
import org.rutebanken.netex.validation.NeTExValidator;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.exporter.async.NetexMappingIterator;
import org.rutebanken.tiamat.exporter.async.NetexMappingIteratorList;
import org.rutebanken.tiamat.exporter.async.ParentStopFetchingIterator;
import org.rutebanken.tiamat.exporter.async.ParentTreeTopographicPlaceFetchingIterator;
import org.rutebanken.tiamat.exporter.eviction.EntitiesEvictor;
import org.rutebanken.tiamat.exporter.eviction.SessionEntitiesEvictor;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.IDFMVehicleModeStopPlacetypeMapping;
import org.rutebanken.tiamat.exporter.params.TiamatVehicleModeStopPlacetypeMapping;
import org.rutebanken.tiamat.geo.geo.Lambert;
import org.rutebanken.tiamat.geo.geo.LambertPoint;
import org.rutebanken.tiamat.geo.geo.LambertZone;
import org.rutebanken.tiamat.model.GroupOfStopPlaces;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.model.VehicleModeEnumeration;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.GroupOfStopPlacesRepository;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.repository.PointOfInterestClassificationRepository;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.repository.TariffZoneRepository;
import org.rutebanken.tiamat.repository.TopographicPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static javax.xml.bind.JAXBContext.newInstance;

/**
 * Stream data objects inside already serialized publication delivery.
 * To be able to export many stop places wihtout keeping them all in memory.
 */
@Component
public class StreamingPublicationDelivery {

    private static final Logger logger = LoggerFactory.getLogger(StreamingPublicationDelivery.class);

    private static final JAXBContext publicationDeliveryContext = createContext(PublicationDeliveryStructure.class);
    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    private final StopPlaceRepository stopPlaceRepository;
    private final ParkingRepository parkingRepository;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final PointOfInterestClassificationRepository pointOfInterestClassificationRepository;
    private final PublicationDeliveryExporter publicationDeliveryExporter;
    private final TiamatSiteFrameExporter tiamatSiteFrameExporter;
    private final TiamatGeneralFrameExporter tiamatGeneralFrameExporter;
    private final NetexMapper netexMapper;
    private final TariffZoneRepository tariffZoneRepository;
    private final TopographicPlaceRepository topographicPlaceRepository;
    private final GroupOfStopPlacesRepository groupOfStopPlacesRepository;
    private final NeTExValidator neTExValidator = NeTExValidator.getNeTExValidator();
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
                                        PointOfInterestRepository pointOfInterestRepository,
                                        PointOfInterestClassificationRepository pointOfInterestClassificationRepository,
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
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.pointOfInterestClassificationRepository = pointOfInterestClassificationRepository;
        this.publicationDeliveryExporter = publicationDeliveryExporter;
        this.tiamatSiteFrameExporter = tiamatSiteFrameExporter;
        this.tiamatGeneralFrameExporter = tiamatGeneralFrameExporter;
        this.netexMapper = netexMapper;
        this.tariffZoneRepository = tariffZoneRepository;
        this.topographicPlaceRepository = topographicPlaceRepository;
        this.groupOfStopPlacesRepository = groupOfStopPlacesRepository;
        this.validateAgainstSchema = validateAgainstSchema;
    }
    @Transactional
    public void stream(ExportParams exportParams, OutputStream outputStream, Provider provider) throws Exception {
        stream(exportParams, outputStream, false, provider);
    }

    @Transactional
    public void stream(ExportParams exportParams, OutputStream outputStream, boolean ignorePaging, Provider provider) throws JAXBException, IOException, SAXException {
        stream(exportParams, outputStream, false, provider, LocalDateTime.now().withNano(0), null);
    }

    @Transactional
    public void stream(ExportParams exportParams, OutputStream outputStream, boolean ignorePaging, Provider provider, LocalDateTime localDateTime, Long exportJobId) throws JAXBException, IOException, SAXException {
        if (exportJobId == null) {
            //streaming launched by abzu queries, irkalla
            streamForAPI(exportParams, outputStream, ignorePaging, provider, localDateTime);
        } else {
            //async export job launched by user
            streamForAsyncExportJob(exportParams, outputStream, ignorePaging, provider, localDateTime, exportJobId);
        }
    }


    /**
     * Launch object stream for API calls (abzu queries, irkalla)
     * @param exportParams
     * @param outputStream
     * @param ignorePaging
     * @param provider
     * @param localDateTime
     * @throws JAXBException
     * @throws IOException
     * @throws SAXException
     */
    public void streamForAPI(ExportParams exportParams, OutputStream outputStream, boolean ignorePaging, Provider provider, LocalDateTime localDateTime ) throws JAXBException, IOException, SAXException {
        org.rutebanken.tiamat.model.GeneralFrame generalFrame = tiamatGeneralFrameExporter.createTiamatGeneralFrame("MOBI-ITI", localDateTime);

        AtomicInteger mappedStopPlaceCount = new AtomicInteger();
        AtomicInteger mappedParkingCount = new AtomicInteger();
        AtomicInteger mappedTariffZonesCount = new AtomicInteger();
        AtomicInteger mappedTopographicPlacesCount = new AtomicInteger();
        AtomicInteger mappedGroupOfStopPlacesCount = new AtomicInteger();

        EntitiesEvictor entitiesEvictor = instantiateEvictor();

        logger.info("Streaming export initiated. Export params: {}", exportParams);

        // The primary ID represents a stop place with a certain version

        final Set<Long> stopPlacePrimaryIds = stopPlaceRepository.getDatabaseIds(exportParams, ignorePaging, provider);
        final Set<Long> stopPlacePrimaryIdsWithParents = stopPlaceRepository.addParentIds(stopPlacePrimaryIds);


        logger.info("Got {} stop place IDs from stop place search", stopPlacePrimaryIds.size());

        //TODO: stream path links, handle export mode
        logger.info("Mapping site frame to netex model");

        GeneralFrame netexGeneralFrame = netexMapper.mapToNetexModel(generalFrame);

        //List that will contain all the members in the General Frame
        List <JAXBElement<? extends EntityStructure>> listMembers = new ArrayList<>();


        logger.info("Preparing scrollable iterators");
        prepareTopographicPlaces(exportParams, stopPlacePrimaryIdsWithParents, mappedTopographicPlacesCount, listMembers, entitiesEvictor,null);
        logger.info("Topographic places preparation completed");
        prepareTariffZones(exportParams, stopPlacePrimaryIds, mappedTariffZonesCount, listMembers, entitiesEvictor);
        logger.info("TariffZones preparation completed");
        prepareStopPlaces(exportParams, stopPlacePrimaryIds, mappedStopPlaceCount, listMembers, entitiesEvictor);
        logger.info("Stop places preparation completed");
        prepareParkings(mappedParkingCount, listMembers, entitiesEvictor,null);
        logger.info("Parking preparation completed");
        //  prepareGroupOfStopPlaces(exportParams, stopPlacePrimaryIds, mappedGroupOfStopPlacesCount, listMembers, entitiesEvictor);

        completeStreamingProcess(outputStream, mappedStopPlaceCount, mappedParkingCount, mappedTariffZonesCount, mappedTopographicPlacesCount, mappedGroupOfStopPlacesCount, netexGeneralFrame, listMembers);

    }


    /**
     * Do the last steps at the end of the export
     * @param outputStream
     * @param mappedStopPlaceCount
     * @param mappedParkingCount
     * @param mappedTariffZonesCount
     * @param mappedTopographicPlacesCount
     * @param mappedGroupOfStopPlacesCount
     * @param netexGeneralFrame
     * @param listMembers
     * @throws JAXBException
     * @throws IOException
     * @throws SAXException
     */
    private void completeStreamingProcess(OutputStream outputStream, AtomicInteger mappedStopPlaceCount, AtomicInteger mappedParkingCount, AtomicInteger mappedTariffZonesCount,
                                          AtomicInteger mappedTopographicPlacesCount, AtomicInteger mappedGroupOfStopPlacesCount,
                                          GeneralFrame netexGeneralFrame, List<JAXBElement<? extends EntityStructure>> listMembers) throws JAXBException, IOException, SAXException {


        List<JAXBElement<? extends EntityStructure>> filteredListMembers = filterDuplicates(listMembers);

        logger.info("Duplicates filtered");

        //adding the members to the general Frame
        General_VersionFrameStructure.Members general_VersionFrameStructure = netexObjectFactory.createGeneral_VersionFrameStructureMembers();
        general_VersionFrameStructure.withGeneralFrameMemberOrDataManagedObjectOrEntity_Entity(filteredListMembers);
        netexGeneralFrame.withMembers(general_VersionFrameStructure);

        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryExporter.createPublicationDelivery(netexGeneralFrame,"idSite",LocalDateTime.now());

        Marshaller marshaller = createMarshaller();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        JAXBElement<PublicationDeliveryStructure> publicationDelivery = netexObjectFactory.createPublicationDelivery(publicationDeliveryStructure);

        logger.info("Start marshalling publication delivery");
        marshaller.marshal(publicationDelivery, byteArrayOutputStream);
        logger.info("marshalling completed. starting last modifications");
        doLastModifications(outputStream, byteArrayOutputStream);

        logger.info("Mapped {} stop places, {} parkings, {} topographic places, {} group of stop places and {} tariff zones to netex",
                mappedStopPlaceCount.get(),
                mappedParkingCount.get(),
                mappedTopographicPlacesCount,
                mappedGroupOfStopPlacesCount,
                mappedTariffZonesCount);
    }

    /**
     * Launch a stream of the object, for netex export launched by user
     * @param exportParams
     * @param outputStream
     * @param ignorePaging
     * @param provider
     * @param localDateTime
     * @param exportJobId
     * @throws JAXBException
     * @throws IOException
     * @throws SAXException
     */
    public void streamForAsyncExportJob(ExportParams exportParams, OutputStream outputStream, boolean ignorePaging, Provider provider, LocalDateTime localDateTime, Long exportJobId) throws JAXBException, IOException, SAXException {
        org.rutebanken.tiamat.model.GeneralFrame generalFrame = tiamatGeneralFrameExporter.createTiamatGeneralFrame("MOBI-ITI", localDateTime);

        AtomicInteger mappedStopPlaceCount = new AtomicInteger();
        AtomicInteger mappedParkingCount = new AtomicInteger();
        AtomicInteger mappedTariffZonesCount = new AtomicInteger();
        AtomicInteger mappedTopographicPlacesCount = new AtomicInteger();
        AtomicInteger mappedGroupOfStopPlacesCount = new AtomicInteger();

        //   EntitiesEvictor entitiesEvictor = instantiateEvictor();

        //List that will contain all the members in the General Frame
        List <JAXBElement<? extends EntityStructure>> listMembers = new ArrayList<>();

        GeneralFrame netexGeneralFrame = netexMapper.mapToNetexModel(generalFrame);

        stopPlaceRepository.initExportJobTable(provider, exportJobId);
        logger.info("Initialization completed for table export_job_id_list. jobId :" + exportJobId);

        stopPlaceRepository.addParentStopPlacesToExportJobTable(exportJobId);
        logger.info("Parent stop places has been added successfully");

        int totalNbOfStops = stopPlaceRepository.countStopsInExport(exportJobId);
        logger.info("Total nb of stops to export:" + totalNbOfStops);

        boolean isDataToExport = true;
        int totalStopsProcessed = 0;

        Set<Long> completeStopPlaceList = new HashSet<>();

        while (isDataToExport){

            Set<Long> batchIdsToExport = stopPlaceRepository.getNextBatchToProcess(exportJobId);
            if (batchIdsToExport == null || batchIdsToExport.size() == 0){
                logger.info("no more stops to export");
                isDataToExport = false;
            }else{
                completeStopPlaceList.addAll(batchIdsToExport);
                launchBatchExport(batchIdsToExport, mappedStopPlaceCount, null, listMembers, false);
                stopPlaceRepository.deleteProcessedIds(exportJobId, batchIdsToExport);

                totalStopsProcessed = totalStopsProcessed + batchIdsToExport.size();
                logger.info("total stops processed:" + totalStopsProcessed);
            }

        }

        desanitizeImportedIds(listMembers);

        logger.info("Preparing scrollable iterators");
        prepareTopographicPlaces(exportParams, completeStopPlaceList, mappedTopographicPlacesCount, listMembers, null, exportJobId);


        prepareParkings(mappedParkingCount, listMembers, null,exportJobId);
        logger.info("Parking preparation completed");


        completeStreamingProcess(outputStream, mappedStopPlaceCount, mappedParkingCount, mappedTariffZonesCount, mappedTopographicPlacesCount, mappedGroupOfStopPlacesCount, netexGeneralFrame, listMembers);

    }

    /**
     * Read objects and replace sanitized code for : (##3A) by : character
     * @param listMembers
     */
    private void desanitizeImportedIds( List <JAXBElement<? extends EntityStructure>> listMembers){

        for (JAXBElement<? extends EntityStructure> listMember : listMembers) {
            EntityStructure entity = listMember.getValue();
            if (entity instanceof Zone_VersionStructure){
                Zone_VersionStructure zone = (Zone_VersionStructure) entity;

                KeyListStructure keyList = zone.getKeyList();
                if (keyList != null && keyList.getKeyValue() != null) {
                    List<KeyValueStructure> keyValue = keyList.getKeyValue();
                    for (KeyValueStructure structure : keyValue) {
                        if (structure != null && "imported-id".equals(structure.getKey())) {
                            structure.setValue(structure.getValue().replace("##3A##",":"));
                        }
                    }
                }
            }
        }
    }



    /**
     * Filter duplicates objects to avoid xsd errors while marshaling the xml file
     * @param originalList
     *  The original list that might contain duplicates
     * @return
     *  The list without duplicates
     */
    private  List <JAXBElement<? extends EntityStructure>> filterDuplicates( List <JAXBElement<? extends EntityStructure>> originalList){
        List <JAXBElement<? extends EntityStructure>> filteredList = new ArrayList<>();
        Set<String> alreadyProcessedMembers = new HashSet<>();
        for (JAXBElement<? extends EntityStructure> jaxbElement : originalList) {
            String key;

            if (jaxbElement.getValue() instanceof StopPlace){
                StopPlace sp = (StopPlace) jaxbElement.getValue();
                key = sp.getId() + "-" + sp.getVersion();
            }else if(jaxbElement.getValue() instanceof Quay){
                Quay quay = (Quay) jaxbElement.getValue();
                key = quay.getId() + "-" + quay.getVersion();
            }else{
                //all other objects are not filtered
                filteredList.add(jaxbElement);
                continue;
            }

            if (!alreadyProcessedMembers.contains(key)){
                alreadyProcessedMembers.add(key);
                filteredList.add(jaxbElement);
            }
        }
        return filteredList;
    }

    public void streamPOI(ExportParams exportParams, OutputStream outputStream, boolean ignorePaging, Provider provider, LocalDateTime localDateTime, Long exportJobId) throws JAXBException, IOException, SAXException {

        org.rutebanken.tiamat.model.SiteFrame siteFrame = tiamatSiteFrameExporter.createTiamatSiteFrame("Site frame " + exportParams);

        AtomicInteger mappedPointOfInterestCount = new AtomicInteger();
        AtomicInteger mappedPointOfInterestClassificationCount = new AtomicInteger();

        pointOfInterestRepository.initExportJobTable(exportJobId);
        logger.info("Initialization completed for table export_job_id_list. jobId :" + exportJobId);

        int totalNbOfPoi = pointOfInterestRepository.countPOIInExport(exportJobId);
        logger.info("Total nb of POI to export:" + totalNbOfPoi);

        logger.info("Streaming POI export initiated. Export params: {}", exportParams);
        logger.info("Mapping site frame to netex model");
        org.rutebanken.netex.model.SiteFrame netexSiteFrame = netexMapper.mapToNetexModel(siteFrame);
        // On n'exporte pas les topographicPlaces
        netexSiteFrame.setTopographicPlaces(null);

        boolean isDataToExport = true;
        int totalPoiProcessed = 0;

        List<org.rutebanken.tiamat.model.PointOfInterest> initializedPoi = new ArrayList<>();

        while (isDataToExport) {
            Set<Long> batchIdsToExport = pointOfInterestRepository.getNextBatchToProcess(exportJobId);
            if (batchIdsToExport == null || batchIdsToExport.size() == 0) {
                logger.info("no more POI to export");
                isDataToExport = false;
            } else {
                initializedPoi.addAll(pointOfInterestRepository.getPOIInitializedForExport(batchIdsToExport));
                pointOfInterestRepository.deleteProcessedIds(exportJobId, batchIdsToExport);
                totalPoiProcessed = totalPoiProcessed + batchIdsToExport.size();
                logger.info("total poi processed:" + totalPoiProcessed);
            }
        }

        logger.info("Preparing scrollable iterators for poi");
        preparePointsOfInterest(mappedPointOfInterestCount, netexSiteFrame, initializedPoi.iterator());

        logger.info("Preparing scrollable iterators for poi class");
        preparePointsOfInterestClassification(mappedPointOfInterestClassificationCount, netexSiteFrame, initializedPoi.iterator());

        logger.info("Publication delivery creation");
        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryExporter.createPublicationDelivery(netexSiteFrame);
        Marshaller marshaller = createMarshaller();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        JAXBElement<PublicationDeliveryStructure> publicationDelivery = netexObjectFactory.createPublicationDelivery(publicationDeliveryStructure);

        logger.info("Start marshalling publication delivery");
        marshaller.marshal(publicationDelivery, byteArrayOutputStream);

        doLastModifications(outputStream, byteArrayOutputStream);


        logger.info("Mapped {} points of interest to netex", mappedPointOfInterestCount);
    }


    /**
     * Moche Workaround : les ns sont générés bizarrement
     *
     * @param outputStreamOut
     * @param byteArrayOutputIn
     * @throws Exception
     */
    // TODO: okina 07/11/19
    private void doLastModifications(OutputStream outputStreamOut, OutputStream byteArrayOutputIn) {
        String s = null;
        try {
            s = new String(((ByteArrayOutputStream) byteArrayOutputIn).toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        s = s.replace("ns2:pos", "gml:pos");
        s = s.replace("ns2:Polygon ns2:id","gml:Polygon gml:id");
        s = s.replace("/ns2:Polygon","/gml:Polygon");
        s = s.replace("ns2:exterior","gml:exterior");
        s = s.replace("ns2:LinearRing","gml:LinearRing");
        s = s.replace("ns3:", "siri:");

        Document document = stringToDocument(s);
        Node item = document.getElementsByTagName("PublicationDelivery").item(0);
        Element element = (Element) item;
        element.removeAttribute("xmlns:ns2");
        element.removeAttribute("xmlns:ns3");
        element.removeAttribute("xmlns:xsi");
        element.removeAttribute("xsi:schemaLocation");

        element.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
        element.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        element.setAttribute("xmlns:ifopt", "http://www.ifopt.org.uk/ifopt");
        element.setAttribute("xmlns:siri", "http://www.siri.org.uk/siri");
        element.setAttribute("xmlns:acbs", "http://www.ifopt.org.uk/acsb");
        element.setAttribute("xmlns:gml", "http://www.opengis.net/gml/3.2");

        String nv = element.getElementsByTagName("PublicationTimestamp").item(0).getChildNodes().item(0).getNodeValue();
        element.getElementsByTagName("PublicationTimestamp").item(0).getChildNodes().item(0).setNodeValue(nv + "Z");

        try {
            outputStreamOut.write(documentToString(document).getBytes("UTF-8"));
        } catch (Exception e) {
            ; // SWALLO
        }
    }

    private void prepareQuays(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedStopPlaceCount, GeneralFrame netexGeneralFrame, EntitiesEvictor evicter, String providerName) {
        // Override lists with custom iterator to be able to scroll database results on the fly.
        if (!stopPlacePrimaryIds.isEmpty()) {
            logger.info("There are stop places to export");

            final Iterator<org.rutebanken.tiamat.model.StopPlace> netexStopPlaceIterator = stopPlaceRepository.scrollStopPlaces(stopPlacePrimaryIds);

            // Use Listening iterator to collect stop place IDs.
            ParentStopFetchingIterator parentStopFetchingIterator = new ParentStopFetchingIterator(netexStopPlaceIterator, stopPlaceRepository);
            NetexMappingIterator<org.rutebanken.tiamat.model.StopPlace, StopPlace> netexMappingIterator = new NetexMappingIterator<>(netexMapper, parentStopFetchingIterator, StopPlace.class, mappedStopPlaceCount, evicter);

            boolean quaysExportEmpty = false;
            prepareNetexQuaysIDFMProfile(netexMappingIterator, providerName, netexGeneralFrame, quaysExportEmpty);

            // Code commenté car ne fonctionne que dans la génération du netex norvégien
            // List<StopPlace> stopPlaces = new NetexMappingIteratorList<>(() -> new NetexReferenceRemovingIterator(netexMappingIterator, exportParams));
            if (quaysExportEmpty) {
                netexGeneralFrame.withMembers(null);
            }

        } else {
            logger.info("No stop places to export");
        }
    }


    private void feedQuayWithParentInfo(Quay childQuay, StopPlace parentStopPlace){
        SiteRefStructure siteRef = new SiteRefStructure();
        siteRef.withRef(parentStopPlace.getId());
        childQuay.setSiteRef(siteRef);

        childQuay.setTransportMode(parentStopPlace.getTransportMode());
    }

    /**
     * Traitements sur les quays Netex pour préparer l'export selon le profil IDFM
     * @param netexMappingIterator
     * @param providerName
     * @param netexGeneralFrame
     * @param quaysExportEmpty
     */
    private void prepareNetexQuaysIDFMProfile(NetexMappingIterator<org.rutebanken.tiamat.model.StopPlace, StopPlace> netexMappingIterator, String providerName, org.rutebanken.netex.model.GeneralFrame netexGeneralFrame, boolean quaysExportEmpty) {
        Quays_RelStructure quays_relStructure = new Quays_RelStructure();

        IDFMVehicleModeStopPlacetypeMapping idfmVehicleModeStopPlacetypeMapping = new IDFMVehicleModeStopPlacetypeMapping();

        Map<String, Set<String>> mapIdStopPlaceIdQuay = stopPlaceRepository.listStopPlaceIdsAndQuayIds(Instant.now(), Instant.now());

        List<StopPlace> netexStopPlaces = new ArrayList<>();
        List<Quay> netexQuays = new ArrayList<>();

        while (netexMappingIterator.hasNext()) {
            netexStopPlaces.add(netexMappingIterator.next());
        }

        netexStopPlaces.forEach(netexStopPlace -> {
            if (netexStopPlace.getQuays() != null && netexStopPlace.getQuays().getQuayRefOrQuay() != null)
                netexStopPlace.getQuays().getQuayRefOrQuay().forEach(quay -> {
                    Quay netexQuay = (Quay) quay.getValue();
                    feedQuayWithParentInfo(netexQuay,netexStopPlace);
                    netexQuays.add(netexQuay);
                }
                    );
        });

        if (!netexQuays.isEmpty()) {
            setField(Quays_RelStructure.class, "quayRefOrQuay", quays_relStructure, netexQuays);




            List<JAXBElement<? extends EntityStructure>> listOfJaxbQuays = netexQuays.stream()
                    .map(quay -> {
                        // Gestion du mode de transport
                        mapIdStopPlaceIdQuay.forEach((s, strings) -> {
                            if (strings.contains(quay.getId())) {
                                netexStopPlaces.forEach(stopPlace -> {
                                    if (s.equals(stopPlace.getId()))
                                        quay.setTransportMode(idfmVehicleModeStopPlacetypeMapping.getVehicleModeEnumeration(stopPlace.getStopPlaceType()));
                                });
                            }
                        });

                        addNameQuay(quay);

                        addPrivateStopCode(quay);

                        convertGeolocation(quay);

                        addPostalAdress(providerName, quay);

                        addAccessibilityAssessment(providerName, quay);

                        quay.setModification(null);

                        return netexObjectFactory.createQuay(quay);
                    })
                    .collect(Collectors.toList());

            netexGeneralFrame.getMembers().withGeneralFrameMemberOrDataManagedObjectOrEntity_Entity(listOfJaxbQuays);
        } else {
            logger.info("No quays to export");
        }
    }


    private void addNameQuay(Quay quay) {
        // Gestion du nom
        String name = quay
                .getKeyList().getKeyValue()
                .stream()
                .filter(keyValueStructure -> keyValueStructure.getKey().equals(NetexIdMapper.ORIGINAL_NAME_KEY))
                .findFirst()
                .map(KeyValueStructure::getValue)
                .orElse("");

        MultilingualString multilingualString = new MultilingualString();
        multilingualString.setValue(name);
        quay.setName(multilingualString);
    }


    private void addPrivateStopCode(Quay quay) {
        PrivateCodeStructure privateCodeStructure = new PrivateCodeStructure();

        if (quay.getPublicCode() != null) {
            privateCodeStructure.setValue(quay.getPublicCode());
        }
        else{
            // Vérifier l'utilité de ce code, le stop code doit être déjà valorisé
            // Modification probable à réaliser public code -> private code
            String stopCode = quay
                    .getKeyList().getKeyValue()
                    .stream()
                    .filter(keyValueStructure -> keyValueStructure.getKey().equals(NetexIdMapper.ORIGINAL_ID_KEY))
                    .findFirst()
                    .map(KeyValueStructure::getValue)
                    .orElse(null);

            if(stopCode != null && !stopCode.isEmpty()){
                String[] splitStopCode = stopCode.split(":");
                privateCodeStructure.setValue(splitStopCode[2]);
            }
            else{
                privateCodeStructure.setValue(null);
            }
        }

        quay.setPrivateCode(privateCodeStructure);
        quay.setPublicCode(null);


        logger.info("Prepare " + quay.getId());
        quay.setVersion("any");
        quay.withKeyList(null);
    }

    private void convertGeolocation(Quay quay) {
        // Conversion en lambert II étendu
        LocationStructure locationOrDefault = Optional
                .ofNullable(quay.getCentroid().getLocation())
                .orElse(new LocationStructure()
                        .withLatitude(BigDecimal.ZERO)
                        .withLongitude(BigDecimal.ZERO));

        LambertPoint lambertPoint = Lambert.convertToLambert(locationOrDefault.getLatitude().doubleValue(), locationOrDefault.getLongitude().doubleValue(), LambertZone.Lambert93);
        double x = round(lambertPoint.getX(), 1);
        double y = round(lambertPoint.getY(), 1);
        quay.getCentroid().setLocation(new LocationStructure()
                .withPos(
                        new DirectPositionType()
                                .withValue(x, y)
                                .withSrsName("EPSG:2154")));
    }

    private void addPostalAdress(String providerName, Quay quay) {
        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setId(providerName + ":PostalAddress:"+quay.getId());
        postalAddress.setPostalRegion(quay.getPostalAddress().getPostalRegion());
        postalAddress.setVersion("any");

        MultilingualString multilingualStringAddressShortName = new MultilingualString();
        multilingualStringAddressShortName.setValue(quay.getId()+"-address");
        multilingualStringAddressShortName.setLang("fr");

        postalAddress.setName(multilingualStringAddressShortName);
        postalAddress.setName(multilingualStringAddressShortName);

        CountryRef cr = new CountryRef();
        cr.setValue("fr");
        postalAddress.setCountryRef(cr);
        TypeOfPlaceRefs_RelStructure placeRefs = new TypeOfPlaceRefs_RelStructure();
        TypeOfPlaceRefStructure typeOfPlace = new TypeOfPlaceRefStructure();
        typeOfPlace.withRef("monomodalStopPlace");
        placeRefs.withTypeOfPlaceRef(typeOfPlace);
        quay.setPlaceTypes(placeRefs);
        postalAddress.setPlaceTypes(placeRefs);


        quay.setPostalAddress(postalAddress);
    }

    private void addAccessibilityAssessment(String providerName, Quay quay) {
        AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();

        if (quay.getAccessibilityAssessment() == null) {
            accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.UNKNOWN);
            AccessibilityLimitations_RelStructure accessibilityLimitations_relStructure = new AccessibilityLimitations_RelStructure();
            AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
            accessibilityLimitations_relStructure.setAccessibilityLimitation(accessibilityLimitation);
            accessibilityAssessment.setLimitations(accessibilityLimitations_relStructure);
            quay.setAccessibilityAssessment(accessibilityAssessment);
        } else {
            accessibilityAssessment = quay.getAccessibilityAssessment();
        }

        accessibilityAssessment.setId(providerName + ":AccessibilityAssessment:"+quay.getId());
        accessibilityAssessment.setVersion("any");

        AccessibilityLimitations_RelStructure limitations = quay.getAccessibilityAssessment().getLimitations();
        AccessibilityLimitation accessibilityLimitation = limitations.getAccessibilityLimitation();
        accessibilityLimitation.withId(null);
        accessibilityLimitation.withModification(null);
        accessibilityLimitation.withVersion(null);

        if (accessibilityLimitation != null) {
            if (accessibilityLimitation.getVisualSignsAvailable() == null) {
                limitations.getAccessibilityLimitation().setVisualSignsAvailable(LimitationStatusEnumeration.UNKNOWN);
            }
            if (accessibilityLimitation.getAudibleSignalsAvailable() == null) {
                limitations.getAccessibilityLimitation().setAudibleSignalsAvailable(LimitationStatusEnumeration.UNKNOWN);
            }
            if (accessibilityLimitation.getWheelchairAccess() == null) {
                limitations.getAccessibilityLimitation().setWheelchairAccess(LimitationStatusEnumeration.UNKNOWN);
            }
        }

        accessibilityAssessment.withLimitations(limitations);

        quay.setAccessibilityAssessment(accessibilityAssessment);
    }

    private void prepareTariffZones(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedTariffZonesCount, List <JAXBElement<? extends EntityStructure>> listMembers, EntitiesEvictor evicter) {


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


            List<TariffZone> tariffZones = new ArrayList<>();
            tariffZoneMappingIterator.forEachRemaining(tariffZones::add);

            tariffZones.stream().forEach(tariffZone -> {
                JAXBElement<org.rutebanken.netex.model.TariffZone> jaxbElementTariffZone= netexObjectFactory.createTariffZone(tariffZone);
                listMembers.add(jaxbElementTariffZone);
            });
        } else {
            logger.info("No tariff zones to export");
        }

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void prepareParkings(AtomicInteger mappedParkingCount, List<JAXBElement<? extends EntityStructure>> listMembers, EntitiesEvictor evicter, Long exportJobId) {


        Iterator<org.rutebanken.tiamat.model.Parking> parkingResultsIterator;

        if (exportJobId == null){
            parkingResultsIterator = parkingRepository.scrollParkings();
        }else{
            parkingResultsIterator =  getIteratorForManualExport(exportJobId);
        }




        // ExportParams could be used for parkingExportMode.

        int parkingsCount = parkingRepository.countResult();
        if (parkingsCount > 0) {
            // Only set parkings if they will exist during marshalling.
            logger.info("Parking count is {}, will create parking in publication delivery", parkingsCount);

            NetexMappingIterator<org.rutebanken.tiamat.model.Parking, Parking> parkingsMappingIterator = new NetexMappingIterator<>(netexMapper, parkingResultsIterator,
                    Parking.class, mappedParkingCount, evicter);


            List<Parking> netexParkings = new ArrayList<>();
            parkingsMappingIterator.forEachRemaining(netexParkings::add);

            netexParkings.forEach(parking->{
                org.rutebanken.tiamat.model.Parking tiamatParking = parkingRepository.findFirstByNetexIdOrderByVersionDesc(parking.getId());
                if (tiamatParking.getSiret() != null) {
                    String organisationId = "MOBIITI:Organisation:" + UUID.randomUUID().toString();
                    String responsabilitySetId = "MOBIITI:ResponsibilitySet:" + UUID.randomUUID().toString();
                    String responsibilityRoleAssignmentId = "MOBIITI:ResponsibilityRoleAssignment:" + UUID.randomUUID().toString();
                    GeneralOrganisation generalOrganisation = new GeneralOrganisation();
                    generalOrganisation.setId(organisationId);
                    generalOrganisation.setVersion("any");
                    generalOrganisation.getOrganisationType().add(OrganisationTypeEnumeration.OTHER);
                    generalOrganisation.setCompanyNumber(tiamatParking.getSiret());
                    listMembers.add(netexObjectFactory.createGeneralOrganisation(generalOrganisation));

                    OrganisationRefStructure organisationRefStructure = new OrganisationRefStructure();
                    organisationRefStructure.setRef(organisationId);

                    ResponsibilityRoleAssignment_VersionedChildStructure responsibilityRoleAssignment = new ResponsibilityRoleAssignment_VersionedChildStructure();
                    responsibilityRoleAssignment.setId(responsibilityRoleAssignmentId);
                    responsibilityRoleAssignment.setVersion("any");
                    responsibilityRoleAssignment.setResponsibleOrganisationRef(organisationRefStructure);
                    responsibilityRoleAssignment.getStakeholderRoleType().add(StakeholderRoleTypeEnumeration.ENTITY_LEGAL_OWNERSHIP);

                    ResponsibilityRoleAssignments_RelStructure responsibilityRoleAssignmentsRelStructure = new ResponsibilityRoleAssignments_RelStructure();
                    responsibilityRoleAssignmentsRelStructure.getResponsibilityRoleAssignment().add(responsibilityRoleAssignment);

                    ResponsibilitySet responsibilitySet = new ResponsibilitySet();
                    responsibilitySet.setId(responsabilitySetId);
                    responsibilitySet.setVersion("any");
                    responsibilitySet.setRoles(responsibilityRoleAssignmentsRelStructure);
                    listMembers.add(netexObjectFactory.createResponsibilitySet(responsibilitySet));

                    parking.setResponsibilitySetRef(responsibilitySet.getId());
                }
                listMembers.add(netexObjectFactory.createParking(parking));
            });

            TypeOfParking typeOfParkingSecureBikeParking = new TypeOfParking();
            typeOfParkingSecureBikeParking.withVersion("any");
            typeOfParkingSecureBikeParking.withId("SecureBikeParking");
            typeOfParkingSecureBikeParking.withName(new MultilingualString().withValue("Secure Bike Parking"));

            TypeOfParking typeOfParkingIndividualBox = new TypeOfParking();
            typeOfParkingIndividualBox.withVersion("any");
            typeOfParkingIndividualBox.withId("IndividualBox");
            typeOfParkingIndividualBox.withName(new MultilingualString().withValue("Individual Box"));


            listMembers.add(netexObjectFactory.createTypeOfParking(typeOfParkingSecureBikeParking));
            listMembers.add(netexObjectFactory.createTypeOfParking(typeOfParkingIndividualBox));
            logger.info("Adding {} typesOfParking in generalFrame");

        } else {
            logger.info("No parkings to export based on stop places");
        }
    }

    private Iterator<org.rutebanken.tiamat.model.Parking> getIteratorForManualExport(Long exportJobId){
        parkingRepository.initExportJobTable(exportJobId);
        int totalNbOfParkings = stopPlaceRepository.countStopsInExport(exportJobId);
        logger.info("Total nb of parkings to export:" + totalNbOfParkings);


        boolean isDataToExport = true;
        int totalparkingProcessed = 0;

        List<org.rutebanken.tiamat.model.Parking> completeParkingList = new ArrayList<>();

        while (isDataToExport){

            Set<Long> batchIdsToExport = stopPlaceRepository.getNextBatchToProcess(exportJobId);
            if (batchIdsToExport == null || batchIdsToExport.size() == 0) {
                logger.info("no more parkings to export");
                isDataToExport = false;
            } else {
                List<org.rutebanken.tiamat.model.Parking> initializedParkings = parkingRepository.getParkingsInitializedForExport(batchIdsToExport);
                completeParkingList.addAll(initializedParkings);
                stopPlaceRepository.deleteProcessedIds(exportJobId, batchIdsToExport);
                totalparkingProcessed = totalparkingProcessed + batchIdsToExport.size();
                logger.info("total parking processed:" + totalparkingProcessed);
            }
        }

        return completeParkingList.iterator();
    }

    private void preparePointsOfInterest(AtomicInteger mappedPointOfInterestCount, SiteFrame netexSiteFrame, Iterator<org.rutebanken.tiamat.model.PointOfInterest> pointOfInterestIterator) {

        int poiCount = pointOfInterestRepository.countResult();
        if (poiCount > 0) {
            logger.info("POI count is {}, will create poi in publication delivery", poiCount);
            PointsOfInterestInFrame_RelStructure pointsOfInterestInFrame_relStructure = new PointsOfInterestInFrame_RelStructure();
            List<PointOfInterest> pointsOfInterest = new NetexMappingIteratorList<>(() ->
                    new NetexMappingIterator<>(netexMapper, pointOfInterestIterator, PointOfInterest.class, mappedPointOfInterestCount));

            setField(PointsOfInterestInFrame_RelStructure.class, "pointOfInterest", pointsOfInterestInFrame_relStructure, pointsOfInterest);
            netexSiteFrame.setPointsOfInterest(pointsOfInterestInFrame_relStructure);
        } else {
            logger.info("No poi to export");
        }
    }

    private void preparePointsOfInterestClassification(AtomicInteger mappedPointOfInterestClassificationCount, SiteFrame netexSiteFrame, Iterator<org.rutebanken.tiamat.model.PointOfInterest> pointOfInterestIterator) {

        int poiClassificationCount = pointOfInterestClassificationRepository.countResult();
        if (poiClassificationCount > 0) {
            logger.info("POI count is {}, will create poi classifications in publication delivery", poiClassificationCount);

            Site_VersionFrameStructure.PointOfInterestClassifications pointOfInterestClassificationsInFrame_relStructure = new Site_VersionFrameStructure.PointOfInterestClassifications();
            List<org.rutebanken.netex.model.PointOfInterestClassification> pointOfInterestClassifications = new NetexMappingIteratorList<>(() -> new NetexMappingIterator<>(netexMapper, pointOfInterestIterator,
                    org.rutebanken.netex.model.PointOfInterestClassification.class, mappedPointOfInterestClassificationCount));

            setField(PointOfInterestClassificationsInFrame_RelStructure.class, "pointOfInterestClassification", pointOfInterestClassificationsInFrame_relStructure, pointOfInterestClassifications);
            netexSiteFrame.setPointOfInterestClassifications(pointOfInterestClassificationsInFrame_relStructure);
        } else {
            logger.info("No poi classifications to export");
        }
    }

    private void prepareStopPlaces(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedStopPlaceCount, List <JAXBElement<? extends EntityStructure>> listMembers, EntitiesEvictor evicter) {
        // Override lists with custom iterator to be able to scroll database results on the fly.
        if (!stopPlacePrimaryIds.isEmpty()) {

            int nbOfItemInBatch = 0;
            int nbOfProcessedItems = 0;
            logger.info("Total nb of items:" + stopPlacePrimaryIds.size());

            Set<Long> tmpSet = new HashSet<>();


            for (Long stopPlacePrimaryId : stopPlacePrimaryIds) {

                tmpSet.add(stopPlacePrimaryId);
                nbOfItemInBatch++;
                nbOfProcessedItems++;

                if (nbOfItemInBatch == 1000){
                    logger.info("processed items:" + nbOfProcessedItems);
                    launchBatchExport(tmpSet, mappedStopPlaceCount, evicter, listMembers, true);
                    tmpSet.clear();
                    nbOfItemInBatch = 0;
                }
            }

            if (tmpSet.size() > 0){
                //last remaining items
                logger.info("processed items:" + nbOfProcessedItems);
                launchBatchExport(tmpSet, mappedStopPlaceCount, evicter, listMembers, true);
            }

        } else {
            logger.info("No stop places to export");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void launchBatchExport(Set<Long> stopPlacePrimaryIds, AtomicInteger mappedStopPlaceCount, EntitiesEvictor evicter, List <JAXBElement<? extends EntityStructure>> listMembers, boolean shouldRecoverParents){
        logger.info("There are stop places to export");

        List<org.rutebanken.tiamat.model.StopPlace> recoveredStopPlaces = stopPlaceRepository.getStopPlaceInitializedForExport(stopPlacePrimaryIds);

        recoveredStopPlaces.forEach(this::addAdditionalInfo);
        logger.info("Feed of addAdditionalInfo completed");

        NetexMappingIterator<org.rutebanken.tiamat.model.StopPlace, StopPlace> netexMappingIterator;

        if (shouldRecoverParents){
            // Use Listening iterator to collect stop place IDs.
            ParentStopFetchingIterator parentStopFetchingIterator = new ParentStopFetchingIterator(recoveredStopPlaces.iterator(), stopPlaceRepository);
            netexMappingIterator = new NetexMappingIterator<>(netexMapper, parentStopFetchingIterator, StopPlace.class, mappedStopPlaceCount, evicter);
        }else{
            logger.info("Not parent iterator");
            netexMappingIterator = new NetexMappingIterator<>(netexMapper, recoveredStopPlaces.iterator(), StopPlace.class, mappedStopPlaceCount, evicter);
        }

        List<StopPlace> netexStopPlaces = new ArrayList<>();
        netexMappingIterator.forEachRemaining(netexStopPlaces::add);
        logger.info("Feed of netexStopPlaces completed");

        netexStopPlaces.forEach(netexStopPlace -> {
            if (netexStopPlace.getQuays() != null && netexStopPlace.getQuays().getQuayRefOrQuay() != null) {

                Quays_RelStructure quays = netexStopPlace.getQuays();
                Quays_RelStructure quaysReference = netexObjectFactory.createQuays_RelStructure();

                quays.getQuayRefOrQuay().forEach(quay -> {
                    //Adding the quay to the memberList
                    Quay netexQuay = (Quay) quay.getValue();
                    listMembers.add(netexObjectFactory.createQuay(netexQuay));

                    //To isolate the reference to set Quay by the QuayRefStructure
                    QuayRefStructure quayRefStructure = new QuayRefStructure();
                    quayRefStructure.setRef(String.valueOf(netexQuay.getId()));
                    quayRefStructure.setVersion(netexQuay.getVersion());

                    quaysReference.getQuayRefOrQuay().add(netexObjectFactory.createQuayRef(quayRefStructure));
                });
                netexStopPlace.setQuays(quaysReference);
            }
            listMembers.add(netexObjectFactory.createStopPlace(netexStopPlace));
        });

        logger.info("Feed of listmembers completed.");
    }

    private void addAdditionalInfo(org.rutebanken.tiamat.model.StopPlace stopPlace){
        VehicleModeEnumeration transportMode = TiamatVehicleModeStopPlacetypeMapping.getVehicleModeEnumeration(stopPlace.getStopPlaceType());
        stopPlace.setTransportMode(transportMode);
        for (org.rutebanken.tiamat.model.Quay quay : stopPlace.getQuays()) {
            if (quay.getSiteRef() == null) {
                org.rutebanken.tiamat.model.SiteRefStructure siteRef = new org.rutebanken.tiamat.model.SiteRefStructure();
                siteRef.setRef(stopPlace.getNetexId());
                quay.setSiteRef(siteRef);
            }

            if (quay.getTransportMode() == null) {
                quay.setTransportMode(transportMode);
            }
        }
    }

    private void prepareTopographicPlaces(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedTopographicPlacesCount, List <JAXBElement<? extends EntityStructure>> listMembers, EntitiesEvictor evicter, Long jobid) {

        Iterator<TopographicPlace> relevantTopographicPlacesIterator;


        if (jobid == null){
            relevantTopographicPlacesIterator = prepareTopographicPlacesIteratorForAPI(exportParams, stopPlacePrimaryIds);
        }else{
            relevantTopographicPlacesIterator = prepareTopographicPlacesIteratorForExportJob(jobid);
        }



        if (relevantTopographicPlacesIterator.hasNext()) {

            NetexMappingIterator<TopographicPlace, org.rutebanken.netex.model.TopographicPlace> topographicPlaceNetexMappingIterator = new NetexMappingIterator<>(
                    netexMapper, relevantTopographicPlacesIterator, org.rutebanken.netex.model.TopographicPlace.class, mappedTopographicPlacesCount, evicter);

            List<org.rutebanken.netex.model.TopographicPlace> netexTopographicPlaces = new ArrayList<>();
            topographicPlaceNetexMappingIterator.forEachRemaining(netexTopographicPlaces::add);

            netexTopographicPlaces.forEach(topographicPlace -> {
                JAXBElement<org.rutebanken.netex.model.TopographicPlace> jaxbElementTopographicPlace = netexObjectFactory.createTopographicPlace(topographicPlace);
                listMembers.add(jaxbElementTopographicPlace);
            });

        }
    }

    private Iterator<TopographicPlace> prepareTopographicPlacesIteratorForExportJob(Long jobid) {

        topographicPlaceRepository.initExportJobTable(jobid);
        topographicPlaceRepository.addParentTopographicPlacesToExportJobTable(jobid);

        int totalNbOfTopographicPlaces = stopPlaceRepository.countStopsInExport(jobid);
        logger.info("Total nb of topographicPlaces to export:" + totalNbOfTopographicPlaces);


        boolean isDataToExport = true;
        int totalTopographicPlacesProcessed = 0;

        List<org.rutebanken.tiamat.model.TopographicPlace> completeTopographicPlacesList = new ArrayList<>();

        while (isDataToExport){
            Set<Long> batchIdsToExport = stopPlaceRepository.getNextBatchToProcess(jobid);
            if (batchIdsToExport == null || batchIdsToExport.size() == 0) {
                logger.info("no more topographic places to export");
                isDataToExport = false;
            } else {
                List<org.rutebanken.tiamat.model.TopographicPlace> initializedTopos = topographicPlaceRepository.getTopoPlacesInitializedForExport(batchIdsToExport);
                completeTopographicPlacesList.addAll(initializedTopos);
                stopPlaceRepository.deleteProcessedIds(jobid, batchIdsToExport);
                totalTopographicPlacesProcessed = totalTopographicPlacesProcessed + batchIdsToExport.size();
                logger.info("total topographic places processed:" + totalTopographicPlacesProcessed);
            }
        }

        return completeTopographicPlacesList.iterator();

    }

    private Iterator<TopographicPlace> prepareTopographicPlacesIteratorForAPI(ExportParams exportParams, Set<Long> stopPlacePrimaryIds) {

        if (exportParams.getTopographicPlaceExportMode() == null || exportParams.getTopographicPlaceExportMode().equals(ExportParams.ExportMode.ALL)) {
            logger.info("Prepare scrolling for all topographic places");
            return topographicPlaceRepository.scrollTopographicPlaces();

        } else if (exportParams.getTopographicPlaceExportMode().equals(ExportParams.ExportMode.RELEVANT)) {
            logger.info("Prepare scrolling relevant topographic places");
            return new ParentTreeTopographicPlaceFetchingIterator(topographicPlaceRepository.scrollTopographicPlaces(stopPlacePrimaryIds), topographicPlaceRepository);
        } else {
            logger.info("Topographic export mode is {}. Will not export topographic places", exportParams.getTopographicPlaceExportMode());
            return Collections.emptyIterator();
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

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Transactional
    public EntitiesEvictor instantiateEvictor() {
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
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty("com.sun.xml.bind.xmlDeclaration", Boolean.FALSE);
        marshaller.setProperty("com.sun.xml.bind.xmlHeaders", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        if (validateAgainstSchema) {
            marshaller.setSchema(neTExValidator.getSchema());
        }

        return marshaller;
    }

    /*
     * SI possible tout ça à mettre en amont de la génération du xml
     * NB : la suppression de standalone ok en amont
     */

    private static String documentToString(Document doc) {
        String output = null;
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            output = writer.getBuffer().toString();
        } catch (Exception e) {
            logger.error("error",e);

            return null;
        }
        return output;
    }

    private static Document stringToDocument(String strXml) {

        Document doc = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            StringReader strReader = new StringReader(strXml);
            InputSource is = new InputSource(strReader);
            doc = (Document) builder.parse(is);
            doc.setXmlStandalone(false);
        } catch (Exception e) {
            return null;
        }

        return doc;
    }
}

