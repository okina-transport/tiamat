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
import org.rutebanken.netex.model.*;
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
import org.rutebanken.tiamat.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
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

    public void stream(ExportParams exportParams, OutputStream outputStream, Provider provider) throws Exception {
        stream(exportParams, outputStream, false, provider);
    }

    public void stream(ExportParams exportParams, OutputStream outputStream, boolean ignorePaging, Provider provider) throws JAXBException, IOException, SAXException {
        stream(exportParams, outputStream, false, provider, LocalDateTime.now().withNano(0));
    }

    public void stream(ExportParams exportParams, OutputStream outputStream, boolean ignorePaging, Provider provider, LocalDateTime localDateTime) throws JAXBException, IOException, SAXException {

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
        prepareTopographicPlaces(exportParams, stopPlacePrimaryIdsWithParents, mappedTopographicPlacesCount, listMembers, entitiesEvictor);
        prepareTariffZones(exportParams, stopPlacePrimaryIds, mappedTariffZonesCount, listMembers, entitiesEvictor);
        prepareStopPlaces(exportParams, stopPlacePrimaryIds, mappedStopPlaceCount, listMembers, entitiesEvictor);
        prepareParkings(exportParams, stopPlacePrimaryIds, mappedParkingCount,listMembers, entitiesEvictor);
        //prepareGroupOfStopPlaces(exportParams, stopPlacePrimaryIds, mappedGroupOfStopPlacesCount, entitiesEvictor);


        //adding the members to the general Frame
        General_VersionFrameStructure.Members general_VersionFrameStructure = netexObjectFactory.createGeneral_VersionFrameStructureMembers();
        general_VersionFrameStructure.withGeneralFrameMemberOrDataManagedObjectOrEntity_Entity(listMembers);
        netexGeneralFrame.withMembers(general_VersionFrameStructure);

        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryExporter.createPublicationDelivery(netexGeneralFrame,"idSite",LocalDateTime.now());

        Marshaller marshaller = createMarshaller();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();


        JAXBElement<PublicationDeliveryStructure> publicationDelivery = netexObjectFactory.createPublicationDelivery(publicationDeliveryStructure);

        logger.info("Start marshalling publication delivery");
        marshaller.marshal(publicationDelivery, byteArrayOutputStream);

        doLastModifications(outputStream, byteArrayOutputStream);

        logger.info("Mapped {} stop places, {} parkings, {} topographic places, {} group of stop places and {} tariff zones to netex",
                mappedStopPlaceCount.get(),
                mappedParkingCount.get(),
                mappedTopographicPlacesCount,
                mappedGroupOfStopPlacesCount,
                mappedTariffZonesCount);
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
                    Quay netexQuay = (Quay) quay;
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

    private void prepareTariffZones(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedTariffZonesCount, List <JAXBElement<? extends EntityStructure>> listMembers,EntitiesEvictor evicter) {


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

    private void prepareParkings(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedParkingCount,List <JAXBElement<? extends EntityStructure>> listMembers, EntitiesEvictor evicter) {

        // ExportParams could be used for parkingExportMode.

        int parkingsCount = parkingRepository.countResult(stopPlacePrimaryIds);
        if (parkingsCount > 0) {
            // Only set parkings if they will exist during marshalling.
            logger.info("Parking count is {}, will create parking in publication delivery", parkingsCount);


            NetexMappingIterator<org.rutebanken.tiamat.model.Parking, Parking> parkingsMappingIterator = new NetexMappingIterator<>(netexMapper, parkingRepository.scrollParkings(stopPlacePrimaryIds),
            Parking.class, mappedParkingCount, evicter);

            List<Parking> netexParkings = new ArrayList<>();
            parkingsMappingIterator.forEachRemaining(netexParkings::add);

            netexParkings.stream().forEach(parking->{
                listMembers.add(netexObjectFactory.createParking(parking));
            });

        } else {
            logger.info("No parkings to export based on stop places");
        }
    }

    private void prepareStopPlaces(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedStopPlaceCount, List <JAXBElement<? extends EntityStructure>> listMembers, EntitiesEvictor evicter) {
        // Override lists with custom iterator to be able to scroll database results on the fly.
        if (!stopPlacePrimaryIds.isEmpty()) {
            logger.info("There are stop places to export");

            final Iterator<org.rutebanken.tiamat.model.StopPlace> stopPlaceIterator = stopPlaceRepository.scrollStopPlaces(stopPlacePrimaryIds);
            List<org.rutebanken.tiamat.model.StopPlace> recoveredStopPlaces = new ArrayList<>();
            stopPlaceIterator.forEachRemaining(recoveredStopPlaces::add);
            recoveredStopPlaces.forEach(this::addAdditionalInfo);

            // Use Listening iterator to collect stop place IDs.
            ParentStopFetchingIterator parentStopFetchingIterator = new ParentStopFetchingIterator(recoveredStopPlaces.iterator(), stopPlaceRepository);
            NetexMappingIterator<org.rutebanken.tiamat.model.StopPlace, StopPlace> netexMappingIterator = new NetexMappingIterator<>(netexMapper, parentStopFetchingIterator, StopPlace.class, mappedStopPlaceCount, evicter);

            List<StopPlace> netexStopPlaces = new ArrayList<>();
            netexMappingIterator.forEachRemaining(netexStopPlaces::add);


            netexStopPlaces.stream().forEach(netexStopPlace -> {
                if (netexStopPlace.getQuays() != null && netexStopPlace.getQuays().getQuayRefOrQuay() != null){

                    Quays_RelStructure quays = netexStopPlace.getQuays();
                    Quays_RelStructure quaysReference = netexObjectFactory.createQuays_RelStructure();

                    quays.getQuayRefOrQuay().stream().forEach(quay ->{
                        //Adding the quay to the memberList
                        Quay netexQuay = (Quay) quay;
                        listMembers.add(netexObjectFactory.createQuay(netexQuay));

                        //To isolate the reference to set Quay by the QuayRefStructure
                        QuayRefStructure quayRefStructure = new QuayRefStructure();
                        quayRefStructure.setRef(String.valueOf(netexQuay.getId()));
                        quayRefStructure.setVersion(netexQuay.getVersion());

                        quaysReference.getQuayRefOrQuay().add(quayRefStructure);
                    });
                    netexStopPlace.setQuays(quaysReference);
                }
                listMembers.add(netexObjectFactory.createStopPlace(netexStopPlace));
            });

        } else {
            logger.info("No stop places to export");
        }
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

    private void prepareTopographicPlaces(ExportParams exportParams, Set<Long> stopPlacePrimaryIds, AtomicInteger mappedTopographicPlacesCount, List <JAXBElement<? extends EntityStructure>> listMembers, EntitiesEvictor evicter) {

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

            List<org.rutebanken.netex.model.TopographicPlace> netexTopographicPlaces = new ArrayList<>();
            topographicPlaceNetexMappingIterator.forEachRemaining(netexTopographicPlaces::add);

            netexTopographicPlaces.stream().forEach(topographicPlace -> {
                JAXBElement<org.rutebanken.netex.model.TopographicPlace> jaxbElementTopographicPlace = netexObjectFactory.createTopographicPlace(topographicPlace);
                listMembers.add(jaxbElementTopographicPlace);
            });

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

