package no.rutebanken.tiamat.rest.netex.siteframe;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.rutebanken.netex.model.*;
import no.rutebanken.tiamat.importers.SiteFrameImporter;
import no.rutebanken.tiamat.importers.StopPlaceImporter;
import no.rutebanken.tiamat.model.SiteFrame;
import no.rutebanken.tiamat.model.StopPlace;
import no.rutebanken.tiamat.model.StopPlacesInFrame_RelStructure;
import no.rutebanken.tiamat.model.TopographicPlace;
import no.rutebanken.tiamat.model.TopographicPlacesInFrame_RelStructure;
import no.rutebanken.tiamat.netexmapping.NetexMapper;
import no.rutebanken.tiamat.repository.StopPlaceRepository;
import no.rutebanken.tiamat.repository.TopographicPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.Iterator;

/**
 * Import/export Site Frame.
 * TODO: External data should be sent or received wrapped in Publication Delivery.
 */
@Component
@Produces("application/xml")
@Path("/site_frame")
@Transactional
public class SiteFrameResource {

    private static final Logger logger = LoggerFactory.getLogger(SiteFrameResource.class);

    private StopPlaceRepository stopPlaceRepository;

    private TopographicPlaceRepository topographicPlaceRepository;

    private SiteFrameImporter siteFrameImporter;

    private StopPlaceImporter stopPlaceImporter;

    private NetexMapper netexMapper;

    @Autowired
    public SiteFrameResource(StopPlaceRepository stopPlaceRepository,
                             TopographicPlaceRepository topographicPlaceRepository,
                             SiteFrameImporter siteFrameImporter,
                             @Qualifier("cleanStopPlaceImporter") StopPlaceImporter stopPlaceImporter,
                             NetexMapper netexMapper) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.topographicPlaceRepository = topographicPlaceRepository;
        this.siteFrameImporter = siteFrameImporter;
        this.stopPlaceImporter = stopPlaceImporter;
        this.netexMapper = netexMapper;
    }

    @GET
    public Response getSiteFrame() throws JsonProcessingException, JAXBException {

        SiteFrame siteFrame = new SiteFrame();

        Iterable<StopPlace> iterableStopPlaces = stopPlaceRepository.findAll();

        StopPlacesInFrame_RelStructure stopPlacesInFrame_relStructure = new StopPlacesInFrame_RelStructure();

        iterableStopPlaces.forEach(stopPlace -> stopPlacesInFrame_relStructure.getStopPlace().add(stopPlace));

        siteFrame.setStopPlaces(stopPlacesInFrame_relStructure);

        Iterator<TopographicPlace> topographicPlaceIterable = topographicPlaceRepository.findAll().iterator();

        TopographicPlacesInFrame_RelStructure topographicPlaces = new TopographicPlacesInFrame_RelStructure();
        topographicPlaceIterable
                .forEachRemaining(topographicPlace -> topographicPlaces.getTopographicPlace().add(topographicPlace));

        siteFrame.setTopographicPlaces(topographicPlaces);

        no.rutebanken.netex.model.SiteFrame convertedSiteFrame = netexMapper.mapToNetexModel(siteFrame);

        ObjectFactory objectFactory = new ObjectFactory();

        JAXBContext jaxbContext = JAXBContext.newInstance(no.rutebanken.netex.model.SiteFrame.class);
        StringWriter writer = new StringWriter();
        JAXBElement<no.rutebanken.netex.model.SiteFrame> objectFactorySiteFrame = objectFactory.createSiteFrame(convertedSiteFrame);
        jaxbContext.createMarshaller().marshal(objectFactorySiteFrame, writer);
        // TODO: stream
        return Response.ok(writer.toString()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public String importSiteFrame(String xml) throws IOException, JAXBException {
        logger.info("Incoming xml is {} characters long", xml.length());

        JAXBContext jaxbContext = JAXBContext.newInstance(no.rutebanken.netex.model.SiteFrame.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        JAXBElement<no.rutebanken.netex.model.SiteFrame> jaxbElement =
                (JAXBElement<no.rutebanken.netex.model.SiteFrame>) jaxbUnmarshaller.unmarshal(new ByteArrayInputStream(xml.getBytes()));
        no.rutebanken.netex.model.SiteFrame receivedNetexSiteFrame = jaxbElement.getValue();

        logger.info("Unmarshalled site frame with {} topographical places and {} stop places",
                receivedNetexSiteFrame.getTopographicPlaces().getTopographicPlace().size(),
                receivedNetexSiteFrame.getStopPlaces().getStopPlace().size());

        SiteFrame siteFrame = netexMapper.mapToTiamatModel(receivedNetexSiteFrame);

        SiteFrame siteFrameWithProcessedStops = siteFrameImporter.importSiteFrame(siteFrame, stopPlaceImporter);

        return "Imported "+siteFrameWithProcessedStops.getStopPlaces().getStopPlace().size() + " stop places";
    }
}

