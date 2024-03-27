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

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.validation.NeTExValidator;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.exporter.async.NetexMappingIterator;
import org.rutebanken.tiamat.exporter.async.NetexMappingIteratorList;
import org.rutebanken.tiamat.exporter.async.ParentStopFetchingIterator;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.TiamatVehicleModeStopPlacetypeMapping;
import org.rutebanken.tiamat.model.PointOfInterestClassification;
import org.rutebanken.tiamat.model.TariffZone;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.model.VehicleModeEnumeration;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.*;
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
import java.io.*;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
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

    public void stream(OutputStream outputStream, Provider provider, LocalDateTime localDateTime, Long exportJobId) throws JAXBException, IOException, SAXException {
        streamForAsyncExportJob(outputStream, provider, localDateTime, exportJobId);
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

        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryExporter.createPublicationDelivery(netexGeneralFrame,"idSite", LocalDateTime.now());

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
     *
     * @param outputStream
     * @param provider
     * @param localDateTime
     * @param exportJobId
     * @throws JAXBException
     * @throws IOException
     * @throws SAXException
     */
    public void streamForAsyncExportJob(OutputStream outputStream, Provider provider, LocalDateTime localDateTime, Long exportJobId) throws JAXBException, IOException, SAXException {
        org.rutebanken.tiamat.model.GeneralFrame generalFrame = tiamatGeneralFrameExporter.createTiamatGeneralFrame("MOBI-ITI", localDateTime, ExportTypeEnumeration.STOP_PLACE);

        AtomicInteger mappedStopPlaceCount = new AtomicInteger();
        AtomicInteger mappedParkingCount = new AtomicInteger();
        AtomicInteger mappedTariffZonesCount = new AtomicInteger();
        AtomicInteger mappedTopographicPlacesCount = new AtomicInteger();
        AtomicInteger mappedGroupOfStopPlacesCount = new AtomicInteger();

        //List that will contain all the members in the General Frame
        List<JAXBElement<? extends EntityStructure>> listMembers = new ArrayList<>();

        GeneralFrame netexGeneralFrame = netexMapper.mapToNetexModel(generalFrame);

        stopPlaceRepository.initExportJobTable(provider, exportJobId);
        logger.info("Initialization completed for table export_job_id_list. jobId :" + exportJobId);

        stopPlaceRepository.addParentStopPlacesToExportJobTable(exportJobId);
        logger.info("Parent stop places has been added successfully");

        int totalNbOfStops = stopPlaceRepository.countStopsInExport(exportJobId);
        logger.info("Total nb of stops to export:" + totalNbOfStops);

        boolean isDataToExport = true;
        int totalStopsProcessed = 0;
        Set<Long> totalIdsToExport = new HashSet<>();

        while (isDataToExport){

            Set<Long> batchIdsToExport = stopPlaceRepository.getNextBatchToProcess(exportJobId);
            totalIdsToExport = batchIdsToExport.size() > totalIdsToExport.size() ? batchIdsToExport : totalIdsToExport;
            if (batchIdsToExport == null || batchIdsToExport.size() == 0) {
                logger.info("no more stops to export");
                isDataToExport = false;
            } else {
                launchBatchExport(batchIdsToExport, mappedStopPlaceCount, listMembers, false);
                stopPlaceRepository.deleteProcessedIds(exportJobId, batchIdsToExport);

                totalStopsProcessed = totalStopsProcessed + batchIdsToExport.size();
                logger.info("total stops processed:" + totalStopsProcessed);
            }

        }

        desanitizeImportedIds(listMembers, provider.getChouetteInfo().getNameNetexStop());

        logger.info("Preparing scrollable iterators");
        prepareTopographicPlaces(mappedTopographicPlacesCount, listMembers, exportJobId);

        prepareTariffZones(mappedTariffZonesCount, listMembers, exportJobId, totalIdsToExport, provider.getChouetteInfo().getNameNetexStop());

        completeStreamingProcess(outputStream, mappedStopPlaceCount, mappedParkingCount, mappedTariffZonesCount, mappedTopographicPlacesCount, mappedGroupOfStopPlacesCount, netexGeneralFrame, listMembers);
    }

    /**
     * Launch a stream of the object, for netex export launched by user
     * @param outputStream
     * @param localDateTime
     * @param exportJobId
     * @throws JAXBException
     * @throws IOException
     * @throws SAXException
     */
    public void streamParkings(OutputStream outputStream, LocalDateTime localDateTime, Long exportJobId) throws JAXBException, IOException, SAXException {
        org.rutebanken.tiamat.model.GeneralFrame generalFrame = tiamatGeneralFrameExporter.createTiamatGeneralFrame("MOBI-ITI", localDateTime, ExportTypeEnumeration.PARKING);

        AtomicInteger mappedParkingCount = new AtomicInteger();

        //List that will contain all the members in the General Frame
        List <JAXBElement<? extends EntityStructure>> listMembers = new ArrayList<>();

        GeneralFrame netexGeneralFrame = netexMapper.mapToNetexModel(generalFrame);


        parkingRepository.initExportJobTable(exportJobId);
        logger.info("Initialization completed for table export_job_id_list. jobId :" + exportJobId);

        prepareParkings(mappedParkingCount, listMembers, exportJobId);
        logger.info("Parking preparation completed");

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

        logger.info("Mapped {} parkings to netex", mappedParkingCount.get());

    }

    /**
     * Read objects and replace sanitized code for : (##3A) by : character
     * @param listMembers
     */
    private void desanitizeImportedIds( List <JAXBElement<? extends EntityStructure>> listMembers, String prefix){
        for (JAXBElement<? extends EntityStructure> listMember : listMembers) {
            EntityStructure entity = listMember.getValue();
            if (entity instanceof Zone_VersionStructure){
                Zone_VersionStructure zone = (Zone_VersionStructure) entity;

                KeyListStructure keyList = zone.getKeyList();
                if (keyList != null && keyList.getKeyValue() != null) {
                    List<KeyValueStructure> keyValue = keyList.getKeyValue();
                    Iterator<KeyValueStructure> iterator = keyValue.iterator();
                    while (iterator.hasNext()) {
                        KeyValueStructure structure = iterator.next();
                        if (structure != null && ("imported-id".equals(structure.getKey()) || "fare-zone".equals(structure.getKey()))) {
                            structure.setValue(structure.getValue().replace("##3A##", ":"));
                            if (StringUtils.isNotBlank(prefix) && structure.getValue().contains(":")) {
                                String oldString = structure.getValue().split(":")[0];
                                structure.setValue(structure.getValue().replace(oldString, prefix));
                            }
                        }
                        if (structure != null && StringUtils.isEmpty(structure.getValue())){
                            iterator.remove();
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

    public void streamPOI(ExportParams exportParams, OutputStream outputStream, Long exportJobId) throws JAXBException, IOException, SAXException {
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
        List<org.rutebanken.tiamat.model.PointOfInterestClassification> initializedPoiClassification = new ArrayList<>();

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

        pointOfInterestClassificationRepository.initExportJobTable(exportJobId);

        isDataToExport = true;
        int totalPoiClassificationProcessed = 0;

        while (isDataToExport) {
            Set<Long> batchIdsToExport = pointOfInterestClassificationRepository.getNextBatchToProcess(exportJobId);
            if (batchIdsToExport == null || batchIdsToExport.size() == 0) {
                logger.info("no more POI classification to export");
                isDataToExport = false;
            } else {
                initializedPoiClassification.addAll(pointOfInterestClassificationRepository.getPOIClassificationInitializedForExport(batchIdsToExport));
                pointOfInterestClassificationRepository.deleteProcessedIds(exportJobId, batchIdsToExport);
                totalPoiClassificationProcessed = totalPoiClassificationProcessed + batchIdsToExport.size();
                logger.info("total poi classification processed:" + totalPoiClassificationProcessed);
            }
        }

        logger.info("Preparing scrollable iterators for poi");
        preparePointsOfInterest(mappedPointOfInterestCount, netexSiteFrame, initializedPoi.iterator());

        logger.info("Preparing scrollable iterators for poi class");
        preparePointsOfInterestClassification(mappedPointOfInterestClassificationCount, netexSiteFrame, initializedPoiClassification.iterator());

        logger.info("Preparing scrollable iterators for poi classification hierarchies");
        preparePointsOfInterestClassificationHierarchies(netexSiteFrame, initializedPoiClassification);

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
            ; // SWALLOW
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void prepareParkings(AtomicInteger mappedParkingCount, List<JAXBElement<? extends EntityStructure>> listMembers, Long exportJobId) {
        Iterator<org.rutebanken.tiamat.model.Parking> parkingResultsIterator;

        if (exportJobId == null) {
            List<org.rutebanken.tiamat.model.Parking> parkingList = parkingRepository.getParkingsInitializedForExport(parkingRepository.scrollParkings());
            parkingResultsIterator = parkingList.iterator();
        } else {
            parkingResultsIterator = getIteratorForManualExport(exportJobId);
        }

        // ExportParams could be used for parkingExportMode.

        int parkingsCount = parkingRepository.countResult();
        if (parkingsCount > 0) {
            // Only set parkings if they will exist during marshalling.
            logger.info("Parking count is {}, will create parking in publication delivery", parkingsCount);
            mappedParkingCount.set(parkingsCount);

            while (parkingResultsIterator.hasNext()) {
                org.rutebanken.tiamat.model.Parking tp = parkingResultsIterator.next();
                Parking np = netexMapper.getFacade().map(tp, Parking.class);
                if (tp.getSiret() != null && !tp.getSiret().isEmpty()) {
                    String organisationId = "MOBIITI:Organisation:" + UUID.randomUUID().toString();
                    String responsabilitySetId = "MOBIITI:ResponsibilitySet:" + UUID.randomUUID().toString();
                    String responsibilityRoleAssignmentId = "MOBIITI:ResponsibilityRoleAssignment:" + UUID.randomUUID().toString();
                    GeneralOrganisation generalOrganisation = new GeneralOrganisation();
                    generalOrganisation.setId(organisationId);
                    generalOrganisation.setVersion("any");
                    generalOrganisation.getOrganisationType().add(OrganisationTypeEnumeration.OTHER);
                    generalOrganisation.setCompanyNumber(tp.getSiret());
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

                    np.setResponsibilitySetRef(responsibilitySet.getId());
                }
                listMembers.add(netexObjectFactory.createParking(np));
            }

            TypeOfParking typeOfParkingSecureBikeParking = new TypeOfParking();
            typeOfParkingSecureBikeParking.withVersion("any");
            typeOfParkingSecureBikeParking.withId("SecureBikeParking");
            typeOfParkingSecureBikeParking.withName(new MultilingualString().withValue("Secure Bike Parking"));

            TypeOfParking typeOfParkingIndividualBox = new TypeOfParking();
            typeOfParkingIndividualBox.withVersion("any");
            typeOfParkingIndividualBox.withId("IndividualBox");
            typeOfParkingIndividualBox.withName(new MultilingualString().withValue("Individual Box"));

            TypeOfParking typeBikeParking = new TypeOfParking();
            typeBikeParking.withVersion("any");
            typeBikeParking.withId("BikeParking");
            typeBikeParking.withName(new MultilingualString().withValue("Bike Parking"));

            listMembers.add(netexObjectFactory.createTypeOfParking(typeBikeParking));
            listMembers.add(netexObjectFactory.createTypeOfParking(typeOfParkingSecureBikeParking));
            listMembers.add(netexObjectFactory.createTypeOfParking(typeOfParkingIndividualBox));
            logger.info("Adding {} typesOfParking in generalFrame");
        } else {
            logger.info("No parkings to export");
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


    private void preparePointsOfInterestClassification(AtomicInteger mappedPointOfInterestClassificationCount, SiteFrame netexSiteFrame, Iterator<org.rutebanken.tiamat.model.PointOfInterestClassification> pointOfInterestClassificationIterator) {
        int poiClassificationCount = pointOfInterestClassificationRepository.countResult();
        if (poiClassificationCount > 0) {
            logger.info("POI count is {}, will create poi classifications in publication delivery", poiClassificationCount);

            Site_VersionFrameStructure.PointOfInterestClassifications pointOfInterestClassificationsInFrame_relStructure = new Site_VersionFrameStructure.PointOfInterestClassifications();
            List<org.rutebanken.netex.model.PointOfInterestClassification> pointOfInterestClassifications = new NetexMappingIteratorList<>(() -> new NetexMappingIterator<>(netexMapper, pointOfInterestClassificationIterator,
                    org.rutebanken.netex.model.PointOfInterestClassification.class, mappedPointOfInterestClassificationCount));

            setField(PointOfInterestClassificationsInFrame_RelStructure.class, "pointOfInterestClassification", pointOfInterestClassificationsInFrame_relStructure, pointOfInterestClassifications);//pointsOfInterestClassification);
            netexSiteFrame.setPointOfInterestClassifications(pointOfInterestClassificationsInFrame_relStructure);
        } else {
            logger.info("No poi classifications to export");
        }
    }

    private void preparePointsOfInterestClassificationHierarchies(SiteFrame netexSiteFrame, List<PointOfInterestClassification> pointOfInterestClassificationList) {
        org.rutebanken.netex.model.PointOfInterestClassificationHierarchyMembers_RelStructure pointOfInterestClassificationHierarchyMembers_relStructure = new org.rutebanken.netex.model.PointOfInterestClassificationHierarchyMembers_RelStructure();

        pointOfInterestClassificationList.forEach(pointOfInterestClassification -> {
            if(pointOfInterestClassification.getParent() != null && pointOfInterestClassification.getParent().getNetexId() != null){
                org.rutebanken.netex.model.PointOfInterestClassificationHierarchyMemberStructure pointOfInterestClassificationHierarchyMemberStructure = new org.rutebanken.netex.model.PointOfInterestClassificationHierarchyMemberStructure();
                org.rutebanken.netex.model.PointOfInterestClassificationRefStructure parentClassificationRef = new org.rutebanken.netex.model.PointOfInterestClassificationRefStructure();
                parentClassificationRef.setVersion("any");
                parentClassificationRef.setRef(pointOfInterestClassification.getParent().getNetexId());

                org.rutebanken.netex.model.PointOfInterestClassificationRefStructure pointOfInterestClassificationRef = new org.rutebanken.netex.model.PointOfInterestClassificationRefStructure();
                pointOfInterestClassificationRef.setVersion("any");
                pointOfInterestClassificationRef.setRef(pointOfInterestClassification.getNetexId());

                pointOfInterestClassificationHierarchyMemberStructure.setParentClassificationRef(parentClassificationRef);
                pointOfInterestClassificationHierarchyMemberStructure.setPointOfInterestClassificationRef(pointOfInterestClassificationRef);
                pointOfInterestClassificationHierarchyMembers_relStructure.getClassificationHierarchyMember().add(pointOfInterestClassificationHierarchyMemberStructure);
            }
        });

        if(pointOfInterestClassificationHierarchyMembers_relStructure.getClassificationHierarchyMember().size() > 0){
            org.rutebanken.netex.model.PointOfInterestClassificationHierarchiesInFrame_RelStructure pointOfInterestClassificationHierarchiesInFrame_RelStructure = new org.rutebanken.netex.model.PointOfInterestClassificationHierarchiesInFrame_RelStructure();

            org.rutebanken.netex.model.PointOfInterestClassificationHierarchy pointOfInterestClassificationHierarchy = new org.rutebanken.netex.model.PointOfInterestClassificationHierarchy();
            MultilingualString name = new MultilingualString();
            name.setValue("Main Hierarchy");
            pointOfInterestClassificationHierarchy.setName(name);
            pointOfInterestClassificationHierarchy.setVersion("any");
            pointOfInterestClassificationHierarchy.setId("1");

            pointOfInterestClassificationHierarchy.setMembers(pointOfInterestClassificationHierarchyMembers_relStructure);
            pointOfInterestClassificationHierarchiesInFrame_RelStructure.withPointOfInterestClassificationHierarchy(pointOfInterestClassificationHierarchy);
            netexSiteFrame.setPointOfInterestClassificationHierarchies(pointOfInterestClassificationHierarchiesInFrame_RelStructure);
        }
        else {
            logger.info("No poi classification hierarchies to export");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void launchBatchExport(Set<Long> stopPlacePrimaryIds, AtomicInteger mappedStopPlaceCount, List<JAXBElement<? extends EntityStructure>> listMembers, boolean shouldRecoverParents) {
        logger.info("There are stop places to export");

        List<org.rutebanken.tiamat.model.StopPlace> recoveredStopPlaces = stopPlaceRepository.getStopPlaceInitializedForExport(stopPlacePrimaryIds);

        recoveredStopPlaces.forEach(this::addAdditionalInfo);
        logger.info("Feed of addAdditionalInfo completed");

        NetexMappingIterator<org.rutebanken.tiamat.model.StopPlace, StopPlace> netexMappingIterator;

        if (shouldRecoverParents) {
            // Use Listening iterator to collect stop place IDs.
            ParentStopFetchingIterator parentStopFetchingIterator = new ParentStopFetchingIterator(recoveredStopPlaces.iterator(), stopPlaceRepository);
            netexMappingIterator = new NetexMappingIterator<>(netexMapper, parentStopFetchingIterator, StopPlace.class, mappedStopPlaceCount);
        } else {
            logger.info("Not parent iterator");
            netexMappingIterator = new NetexMappingIterator<>(netexMapper, recoveredStopPlaces.iterator(), StopPlace.class, mappedStopPlaceCount);
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
        if(stopPlace.getTransportMode() == null) {
            VehicleModeEnumeration transportMode = TiamatVehicleModeStopPlacetypeMapping.getVehicleModeEnumeration(stopPlace.getStopPlaceType());
            stopPlace.setTransportMode(transportMode);
        }

        for (org.rutebanken.tiamat.model.Quay quay : stopPlace.getQuays()) {
            if (quay.getSiteRef() == null) {
                org.rutebanken.tiamat.model.SiteRefStructure siteRef = new org.rutebanken.tiamat.model.SiteRefStructure();
                siteRef.setRef(stopPlace.getNetexId());
                quay.setSiteRef(siteRef);
            }

            if (quay.getTransportMode() == null) {
                quay.setTransportMode(stopPlace.getTransportMode());
            }
        }
    }

    private void prepareTopographicPlaces(AtomicInteger mappedTopographicPlacesCount, List<JAXBElement<? extends EntityStructure>> listMembers, Long jobid) {
        Iterator<TopographicPlace> relevantTopographicPlacesIterator;

        relevantTopographicPlacesIterator = prepareTopographicPlacesIteratorForExportJob(jobid);

        if (relevantTopographicPlacesIterator.hasNext()) {
            NetexMappingIterator<TopographicPlace, org.rutebanken.netex.model.TopographicPlace> topographicPlaceNetexMappingIterator = new NetexMappingIterator<>(
                    netexMapper, relevantTopographicPlacesIterator, org.rutebanken.netex.model.TopographicPlace.class, mappedTopographicPlacesCount);

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

    private void prepareTariffZones(AtomicInteger mappedTariffZonesCount, List<JAXBElement<? extends EntityStructure>> listMembers, Long jobid, Set<Long> listStopPlaces, String prefix) {
        Iterator<org.rutebanken.tiamat.model.TariffZone> tariffZoneIterator;

        tariffZoneIterator = prepareTariffZonesIteratorForExportJob(jobid, listStopPlaces);

        if (tariffZoneIterator.hasNext()) {
            NetexMappingIterator<org.rutebanken.tiamat.model.TariffZone, org.rutebanken.netex.model.TariffZone> tariffZoneMappingIterator =
                    new NetexMappingIterator<>(netexMapper, tariffZoneIterator, org.rutebanken.netex.model.TariffZone.class, mappedTariffZonesCount);

            List<org.rutebanken.netex.model.TariffZone> tariffZones = new ArrayList<>();
            tariffZoneMappingIterator.forEachRemaining(tariffZones::add);
            List<JAXBElement<? extends EntityStructure>> jaxbElementList = new ArrayList<>();
            tariffZones.forEach(tariffZone -> {
                JAXBElement<org.rutebanken.netex.model.TariffZone> jaxbElementTariffZone = netexObjectFactory.createTariffZone(tariffZone);
                jaxbElementList.add(jaxbElementTariffZone);
            });
            desanitizeImportedIds(jaxbElementList, prefix);
            listMembers.addAll(jaxbElementList);
        } else {
            logger.info("No tariff zones to export");
        }
    }

    private Iterator<TariffZone> prepareTariffZonesIteratorForExportJob(Long jobid, Set<Long> listStopPlaces) {

        tariffZoneRepository.initExportJobTable(jobid, listStopPlaces);

        int totalNbOfTariffZones = stopPlaceRepository.countStopsInExport(jobid);
        logger.info("Total nb of tariff zones to export:" + totalNbOfTariffZones);


        boolean isDataToExport = true;
        int totalTariffZonesProcessed = 0;

        List<org.rutebanken.tiamat.model.TariffZone> completeTariffZonesList = new ArrayList<>();

        while (isDataToExport){
            Set<Long> batchIdsToExport = stopPlaceRepository.getNextBatchToProcess(jobid);
            if (batchIdsToExport == null || batchIdsToExport.size() == 0) {
                logger.info("no more tariff zones to export");
                isDataToExport = false;
            } else {
                List<org.rutebanken.tiamat.model.TariffZone> initializedTariffZones = tariffZoneRepository.getTariffZonesInitializedForExport(batchIdsToExport);
                completeTariffZonesList.addAll(initializedTariffZones);
                stopPlaceRepository.deleteProcessedIds(jobid, batchIdsToExport);
                totalTariffZonesProcessed = totalTariffZonesProcessed + batchIdsToExport.size();
                logger.info("total tariff zones processed:" + totalTariffZonesProcessed);
            }
        }

        return completeTariffZonesList.iterator();

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