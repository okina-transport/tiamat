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

import io.swagger.annotations.Api;
import org.rutebanken.tiamat.dtoassembling.disassembler.ChangedStopPlaceSearchDisassembler;
import org.rutebanken.tiamat.dtoassembling.dto.ChangedStopPlaceSearchDto;
import org.rutebanken.tiamat.exporter.PublicationDeliveryExporter;
import org.rutebanken.tiamat.exporter.PublicationDeliveryStructurePage;
import org.rutebanken.tiamat.exporter.StreamingPublicationDelivery;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.repository.search.ChangedStopPlaceSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.transaction.Transactional;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Api(tags = {"Sync export resource"}, produces = "application/xml")
@Produces("application/xml")
@Path("netex")
public class ExportResource {

    private static final Logger logger = LoggerFactory.getLogger(ExportResource.class);

    private final PublicationDeliveryStreamingOutput publicationDeliveryStreamingOutput;

    private final PublicationDeliveryExporter publicationDeliveryExporter;

    private final ChangedStopPlaceSearchDisassembler changedStopPlaceSearchDisassembler;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private QuayRepository quayRepository;

    @Qualifier("syncStreamingPublicationDelivery")
    @Autowired
    private StreamingPublicationDelivery streamingPublicationDelivery;

    @Autowired
    public ExportResource(PublicationDeliveryStreamingOutput publicationDeliveryStreamingOutput,
                          PublicationDeliveryExporter publicationDeliveryExporter,
                          ChangedStopPlaceSearchDisassembler changedStopPlaceSearchDisassembler) {

        this.publicationDeliveryStreamingOutput = publicationDeliveryStreamingOutput;
        this.publicationDeliveryExporter = publicationDeliveryExporter;
        this.changedStopPlaceSearchDisassembler = changedStopPlaceSearchDisassembler;
    }

    @GET
    @Produces(MediaType.APPLICATION_XML + "; charset=UTF-8")
    public Response exportStopPlaces(@BeanParam ExportParams exportParams) {
        logger.info("Exporting publication delivery. {}", exportParams);


        StreamingOutput streamingOutput = outputStream -> {
            try {
                streamingPublicationDelivery.stream(exportParams, outputStream, null);
            } catch (Exception e) {
                logger.warn("Could not stream site frame. {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        };

        return Response.ok(streamingOutput).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_XML + "; charset=UTF-8")
    @Path("changed_in_period")
    public Response exportStopPlacesWithEffectiveChangedInPeriod(@BeanParam ChangedStopPlaceSearchDto searchDTO,
                                                                 @BeanParam ExportParams exportParams,
                                                                 @Context UriInfo uriInfo)
            throws JAXBException, IOException, SAXException {

        ChangedStopPlaceSearch search = changedStopPlaceSearchDisassembler.disassemble(searchDTO);
        logger.info("Exporting stop places. Search: {}, topographic export mode: {}", search, exportParams.getTopographicPlaceExportMode());
        PublicationDeliveryStructurePage resultPage =
                publicationDeliveryExporter.exportStopPlacesWithEffectiveChangeInPeriod(search, exportParams);

        if (resultPage.totalElements == 0) {
            logger.debug("Returning no content. No stops changed in period.");
            return Response.noContent().build();
        }

        logger.info("Streaming {} changed stops in publication delivery structure", resultPage.size);
        Response.ResponseBuilder rsp = Response.ok(publicationDeliveryStreamingOutput.stream(resultPage.publicationDeliveryStructure));

        if (resultPage.hasNext) {
            rsp.link(createLinkToNextPage(searchDTO.from, searchDTO.to, search.getPageable().getPageNumber() + 1, search.getPageable().getPageSize(), exportParams.getTopographicPlaceExportMode(), exportParams.getTariffZoneExportMode(), uriInfo), "next");
        }

        return rsp.build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("getQuayInfo")
    @Transactional
    public Response getQuayInfo(@QueryParam("quayNetexId") String quayNetexId){

        List<Quay> quays = stopPlaceRepository.findQuayByNetexId(quayNetexId);
        if (quays.size() == 0){
            return Response.noContent().build();
        }

        List<StopPlace> stopPlaces = stopPlaceRepository.findStopPlaceByQuays(quays);


        List<StopPlaceView> stopPlaceViews = stopPlaces.stream()
                                                    .map(StopPlaceView::new)
                                                    .collect(Collectors.toList());

        return Response.ok().entity(stopPlaceViews).build();

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("getStopPlaceInfo")
    @Transactional
    public Response getStopPlaceInfo(@QueryParam("stopPlaceNetexId") String stopPlaceNetexId){

        List<StopPlace> stopPlaces = stopPlaceRepository.findAll(Arrays.asList(stopPlaceNetexId));


        List<StopPlaceView> stopPlaceViews = stopPlaces.stream()
                .map(StopPlaceView::new)
                .collect(Collectors.toList());

        return Response.ok().entity(stopPlaceViews).build();

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("getImportedIdInfo")
    @Transactional
    public Response getImportedIdInfo(@QueryParam("referential")String referential, @QueryParam("importedId") String importedId){
        List<StopPlaceView> resultViews = new ArrayList<>();

        try{
            List<Quay> quays =   quayRepository.findAllByImportedId(referential + ":Quay:" + importedId);

            if (quays.size() > 0){
                List<StopPlace> stopPlaces = stopPlaceRepository.findStopPlaceByQuays(quays);

                List<StopPlaceView> stopPlaceViews = stopPlaces.stream()
                        .map(StopPlaceView::new)
                        .collect(Collectors.toList());
                resultViews.addAll(stopPlaceViews);
            }

        List<StopPlace> stopPlaces = stopPlaceRepository.findAllFromImportedId(referential + ":StopPlace:" +importedId);

        if (stopPlaces.size() > 0 ){
            List<StopPlaceView> stopPlaceViews = stopPlaces.stream()
                                                            .map(StopPlaceView::new)
                                                            .collect(Collectors.toList());

            resultViews.addAll(stopPlaceViews);
        }

        }catch(Exception e){
            logger.error("a", e);
        }

       return Response.ok().entity(resultViews).build();

    }



    private URI createLinkToNextPage(String from, String to, int page, int perPage, ExportParams.ExportMode topographicPlaceExportMode, ExportParams.ExportMode tariffZoneExportMode, UriInfo uriInfo) {
        UriBuilder linkBuilder = uriInfo.getAbsolutePathBuilder()
                .queryParam("page", page)
                .queryParam("per_page", perPage)
                .queryParam("topographicPlaceExportMode", topographicPlaceExportMode)
                .queryParam("tariffZoneExportMode", tariffZoneExportMode);

        if (from != null) linkBuilder.queryParam("from", from);
        if (to != null) linkBuilder.queryParam("to", to);
        return linkBuilder.build();
    }
}
