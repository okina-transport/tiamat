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

package org.rutebanken.tiamat.rest.netex.publicationdelivery;


import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ImportType;
import org.rutebanken.tiamat.model.identification.IdentifiedEntity;
import org.rutebanken.tiamat.netex.id.NetexIdHelper;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static javax.xml.bind.JAXBContext.newInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

@Component
public class PublicationDeliveryTestHelper {

    private static final Logger logger = LoggerFactory.getLogger(PublicationDeliveryTestHelper.class);

    private static final JAXBContext jaxbContext;

    private static final String defaultTimeZone = "Europe/Paris";

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    static {
        try {
            jaxbContext = newInstance(PublicationDeliveryStructure.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private ImportResource importResource;

    public PublicationDeliveryStructure createPublicationDeliveryTopographicPlace(TopographicPlace... topographicPlace) {
        SiteFrame siteFrame = siteFrame();
        siteFrame.withTopographicPlaces(new TopographicPlacesInFrame_RelStructure().withTopographicPlace(topographicPlace));
        return publicationDelivery(siteFrame);
    }

    public PublicationDeliveryStructure publicationDelivery(SiteFrame siteFrame) {
        return new PublicationDeliveryStructure()
                .withPublicationTimestamp(LocalDateTime.now())
                .withVersion("1")
                .withParticipantRef("test")
                .withDataObjects(new PublicationDeliveryStructure.DataObjects()
                        .withCompositeFrameOrCommonFrame(new ObjectFactory().createSiteFrame(siteFrame)));
    }

    public PublicationDeliveryStructure publicationDelivery(GeneralFrame generalFrame){
        return new PublicationDeliveryStructure()
                .withPublicationTimestamp(LocalDateTime.now())
                .withVersion("1")
                .withParticipantRef("test")
                .withDataObjects(new PublicationDeliveryStructure.DataObjects()
                        .withCompositeFrameOrCommonFrame(new ObjectFactory().createGeneralFrame(generalFrame)));
    }
    public SiteFrame siteFrame() {
        SiteFrame siteFrame = new SiteFrame();
        siteFrame.setVersion("1");
        siteFrame.setId(UUID.randomUUID().toString());
        siteFrame.setFrameDefaults(
                new VersionFrameDefaultsStructure()
                        .withDefaultLocale(
                                new LocaleStructure().withTimeZone(defaultTimeZone)));
        return siteFrame;
    }


    public PublicationDeliveryStructure createPublicationDeliveryWithStopPlace(StopPlace... stopPlace) {
        SiteFrame siteFrame = siteFrame();
        siteFrame.withStopPlaces(new StopPlacesInFrame_RelStructure()
                .withStopPlace(stopPlace));

        return publicationDelivery(siteFrame);
    }

    public PublicationDeliveryStructure createPublicationDeliveryWithStopPlaceInAGeneralFrame(StopPlace... stopPlaces) {

        List <JAXBElement<? extends EntityStructure>> listMembers = new ArrayList<>();
        Arrays.asList(stopPlaces).stream().forEach(stop -> listMembers.add(netexObjectFactory.createStopPlace(stop)));

        GeneralFrame netexGeneralFrame = new GeneralFrame();
        netexGeneralFrame.setVersion("1");
        netexGeneralFrame.setId(UUID.randomUUID().toString());
        General_VersionFrameStructure.Members general_VersionFrameStructure = netexObjectFactory.createGeneral_VersionFrameStructureMembers();
        general_VersionFrameStructure.withGeneralFrameMemberOrDataManagedObjectOrEntity_Entity(listMembers);
        netexGeneralFrame.withMembers(general_VersionFrameStructure);

        return publicationDelivery(netexGeneralFrame);
    }

        public void addPathLinks(PublicationDeliveryStructure publicationDeliveryStructure, PathLink... pathLink) {
        findSiteFrame(publicationDeliveryStructure)
                .withPathLinks(new PathLinksInFrame_RelStructure().withPathLink(pathLink));
    }

    public void hasOriginalId(String expectedId, DataManagedObjectStructure object) {
        assertThat(object).isNotNull();
        assertThat(object.getKeyList()).isNotNull();
        List<String> list = object.getKeyList().getKeyValue()
                .stream()
                .peek(keyValueStructure -> System.out.println(keyValueStructure))
                .filter(keyValueStructure -> keyValueStructure.getKey().equals(ORIGINAL_ID_KEY))
                .map(keyValueStructure -> keyValueStructure.getValue())
                .map(value -> value.split(","))
                .flatMap(values -> Stream.of(values))
                .filter(value -> value.equals(expectedId))
                .collect(Collectors.toList());
        assertThat(list).as("Matching original ID " + expectedId).hasSize(1);
    }

    public List<StopPlace> extractStopPlaces(Response response, boolean isGeneralFrame) throws IOException, JAXBException {
        return extractStopPlaces(fromResponse(response), isGeneralFrame);
    }

    public List<StopPlace> extractStopPlaces(PublicationDeliveryStructure publicationDeliveryStructure, boolean isGeneralFrame) {
        return extractStopPlaces(publicationDeliveryStructure, isGeneralFrame, true);
    }

    public List<StopPlace> extractStopPlaces(PublicationDeliveryStructure publicationDeliveryStructure, boolean isGeneralFrame,boolean verifyNotNull) {
        if(isGeneralFrame){
            GeneralFrame generalFrame = findGeneralFrame(publicationDeliveryStructure);
            return extractStopPlacesFromGeneralFrame(generalFrame, verifyNotNull);
        }else{
            return extractStopPlaces(findSiteFrame(publicationDeliveryStructure), verifyNotNull);
        }
    }

    public List<StopPlace> extractStopPlaces(SiteFrame siteFrame) {
        return extractStopPlaces(siteFrame, true);
    }

    public List<StopPlace> extractStopPlaces(SiteFrame siteFrame, boolean verifyNotNull) {
        if (verifyNotNull) {
            assertThat(siteFrame.getStopPlaces()).as("Site frame stop places").isNotNull();
            assertThat(siteFrame.getStopPlaces().getStopPlace()).as("Site frame stop places getStopPlace").isNotNull();
        } else if (siteFrame.getStopPlaces() == null || siteFrame.getStopPlaces().getStopPlace() == null) {
            return new ArrayList<>();
        }
        return siteFrame.getStopPlaces().getStopPlace();
    }

    public List<StopPlace> extractStopPlacesFromGeneralFrame(GeneralFrame generalFrame, boolean verifyNotNull) {
        if(verifyNotNull){
            assertThat(generalFrame.getMembers()).isNotNull();
            assertThat(generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity()).isNotNull();
        }else{
            return new ArrayList<>();
        }
        List<JAXBElement> jaxStopPlaces = generalFrame.getMembers()
                .getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity()
                .stream()
                .filter(jaxbElement -> jaxbElement.getValue() instanceof org.rutebanken.netex.model.StopPlace)
                .collect(Collectors.toList());

        return jaxStopPlaces.stream().map(jaxStopPlace -> (StopPlace)jaxStopPlace.getValue()).collect(Collectors.toList());
    }

    public GroupOfStopPlaces extractGroupOfStopPlacesFromGeneralFrame(GeneralFrame generalFrame){

        List<JAXBElement> jaxGroupStopPlaces = generalFrame.getMembers()
                .getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity()
                .stream()
                .filter(jaxbElement -> jaxbElement.getValue() instanceof org.rutebanken.netex.model.GroupOfStopPlaces)
                .collect(Collectors.toList());

        return jaxGroupStopPlaces.stream().map(jaxGroupStopPlace -> (GroupOfStopPlaces)jaxGroupStopPlace.getValue()).collect(Collectors.toList()).get(0);

    }
    public GroupOfStopPlaces extractGroupOfStopPlaces(SiteFrame siteFrame) {
        assertThat(siteFrame.getGroupsOfStopPlaces()).as("site frame groups of stop places").isNotNull();
        assertThat(siteFrame.getGroupsOfStopPlaces().getGroupOfStopPlaces())
                .as("groups of stop places list")
                .isNotNull();

        return siteFrame.getGroupsOfStopPlaces().getGroupOfStopPlaces().get(0);
    }

    public List<PathLink> extractPathLinks(PublicationDeliveryStructure publicationDeliveryStructure) {

        SiteFrame siteFrame = findSiteFrame(publicationDeliveryStructure);
        if (siteFrame.getPathLinks() != null && siteFrame.getPathLinks().getPathLink() != null) {
            return siteFrame.getPathLinks().getPathLink();
        } else {
            return new ArrayList<>();
        }
    }

    public List<TopographicPlace> extractTopographicPlace(PublicationDeliveryStructure publicationDeliveryStructure) {

        SiteFrame siteFrame = findSiteFrame(publicationDeliveryStructure);
        if (siteFrame.getTopographicPlaces() != null && siteFrame.getTopographicPlaces().getTopographicPlace() != null) {
            return siteFrame.getTopographicPlaces().getTopographicPlace();
        } else {
            return new ArrayList<>();
        }
    }

    public List<Quay> extractQuays(StopPlace stopPlace) {
        return stopPlace
                .getQuays()
                .getQuayRefOrQuay()
                .stream()
                .filter(object -> object instanceof Quay)
                .map(object -> ((Quay) object))
                .collect(toList());
    }

    public StopPlace findFirstStopPlace(PublicationDeliveryStructure publicationDeliveryStructure) {
        return publicationDeliveryStructure.getDataObjects()
                .getCompositeFrameOrCommonFrame()
                .stream()
                .map(JAXBElement::getValue)
                .filter(commonVersionFrameStructure -> commonVersionFrameStructure instanceof SiteFrame)
                .flatMap(commonVersionFrameStructure -> ((SiteFrame) commonVersionFrameStructure).getStopPlaces().getStopPlace().stream())
                .findFirst().get();
    }

    public PublicationDeliveryStructure postAndReturnPublicationDelivery(PublicationDeliveryStructure publicationDeliveryStructure) throws JAXBException, IOException, SAXException, TiamatBusinessException {
        ImportParams importParams = new ImportParams();
        importParams.providerCode = "PROV1";
        importParams.importType = ImportType.MATCH;
        return postAndReturnPublicationDelivery(publicationDeliveryStructure, importParams);
    }

    public PublicationDeliveryStructure postAndReturnPublicationDelivery(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams) throws JAXBException, IOException, SAXException, TiamatBusinessException {
        Response response = postPublicationDelivery(publicationDeliveryStructure, importParams);

        if (!(response.getEntity() instanceof StreamingOutput)) {
            throw new RuntimeException("Response is not instance of streaming output: " + response);
        }
        return fromResponse(response);
    }

    public PublicationDeliveryStructure postAndReturnPublicationDelivery(String publicationDeliveryXml) throws JAXBException, IOException, SAXException, TiamatBusinessException {
        return postAndReturnPublicationDelivery(publicationDeliveryXml, null);
    }

    public PublicationDeliveryStructure postAndReturnPublicationDelivery(String publicationDeliveryXml, ImportParams importParams) throws JAXBException, IOException, SAXException, TiamatBusinessException {

        InputStream stream = new ByteArrayInputStream(publicationDeliveryXml.getBytes(StandardCharsets.UTF_8));

        Response response = importResource.importPublicationDelivery(stream, importParams);

        assertThat(response.getStatus()).isEqualTo(200);

        return fromResponse(response);
    }

    public PublicationDeliveryStructure fromString(String xml) throws IOException, JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setEventHandler(new javax.xml.bind.helpers.DefaultValidationEventHandler());

        logger.info("Printing received response publication delivery \n--------------\n{}\n--------------", xml);

        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        JAXBElement element = (JAXBElement) unmarshaller.unmarshal(inputStream);
        return (PublicationDeliveryStructure) element.getValue();
    }

    public PublicationDeliveryStructure fromResponse(Response response) throws IOException, JAXBException {

        StreamingOutput output = (StreamingOutput) response.getEntity();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        output.write(outputStream);

        return fromString(new String(outputStream.toByteArray()));
    }

    public Response postPublicationDelivery(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams) throws JAXBException, IOException, SAXException, TiamatBusinessException {
        Marshaller marshaller = jaxbContext.createMarshaller();

        JAXBElement<PublicationDeliveryStructure> jaxPublicationDelivery = new ObjectFactory().createPublicationDelivery(publicationDeliveryStructure);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        marshaller.marshal(jaxPublicationDelivery, outputStream);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());        

        return importResource.importPublicationDelivery(inputStream, importParams);
    }
    public GeneralFrame findGeneralFrame(PublicationDeliveryStructure incomingPublicationDelivery){

        List<JAXBElement<? extends Common_VersionFrameStructure>> compositeFrameOrCommonFrame = incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();

        Optional<GeneralFrame> optionalGeneralFrame = compositeFrameOrCommonFrame.stream()
                .filter(element -> element.getValue() instanceof GeneralFrame)
                .map(element -> (GeneralFrame)element.getValue())
                .findFirst();

        if(optionalGeneralFrame.isPresent()){
            return optionalGeneralFrame.get();
        }

        return compositeFrameOrCommonFrame
                .stream()
                .filter(element -> element.getValue() instanceof CompositeFrame)
                .map(element -> (CompositeFrame) element.getValue())
                .map(compositeFrame -> compositeFrame.getFrames())
                .flatMap(frames -> frames.getCommonFrame().stream())
                .filter(jaxbElement -> jaxbElement.getValue() instanceof GeneralFrame)
                .map(jaxbElement -> (GeneralFrame) jaxbElement.getValue())
                .findAny().get();
    }
    public SiteFrame findSiteFrame(PublicationDeliveryStructure publicationDelivery) {

        List<JAXBElement<? extends Common_VersionFrameStructure>> compositeFrameOrCommonFrame = publicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();

        Optional<SiteFrame> optionalSiteframe = compositeFrameOrCommonFrame
                .stream()
                .filter(element -> element.getValue() instanceof SiteFrame)
                .map(element -> (SiteFrame) element.getValue())
                .findFirst();

        if (optionalSiteframe.isPresent()) {
            logger.info("Found site frame from compositeFrameOrCommonFrame {}", optionalSiteframe.get().getStopPlaces());
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

    public StopPlace findStopPlace(PublicationDeliveryStructure publicationDeliveryStructure, String stopPlaceId,  boolean isGeneralFrame) {
        return findStopPlace(publicationDeliveryStructure, stopPlaceId, true, isGeneralFrame);
    }

    public StopPlace findStopPlace(PublicationDeliveryStructure publicationDeliveryStructure, String stopPlaceId, boolean verifyNotNull, boolean isGeneralFrame) {
        return extractStopPlaces(publicationDeliveryStructure, isGeneralFrame, verifyNotNull).stream()
                .filter(stopPlace -> stopPlace.getId().equals(stopPlaceId))
                .findFirst().orElse(null);
    }

}
