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

package org.rutebanken.tiamat.rest.dto;

import io.swagger.annotations.Api;
import org.locationtech.jts.geom.Point;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.dtoassembling.dto.IdMappingDto;
import org.rutebanken.tiamat.dtoassembling.dto.IdMappingDtoCsvMapper;
import org.rutebanken.tiamat.exporter.PublicationDeliveryExporter;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBElement;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Api(tags = {"Stop place resource"}, produces = "text/plain")
@Produces("application/json")
@Path("/")
@Transactional
public class DtoStopPlaceResource {

    private static final Logger logger = LoggerFactory.getLogger(DtoStopPlaceResource.class);

    private final StopPlaceRepository stopPlaceRepository;

    private final DtoMappingSemaphore dtoMappingSemaphore;

    private final IdMappingDtoCsvMapper csvMapper;

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    private PublicationDeliveryExporter publicationDeliveryExporter;


    private final QuayRepository quayRepository;

    @Autowired
    public DtoStopPlaceResource(StopPlaceRepository stopPlaceRepository, DtoMappingSemaphore dtoMappingSemaphore, IdMappingDtoCsvMapper csvMapper, QuayRepository quayRepository) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.dtoMappingSemaphore = dtoMappingSemaphore;
        this.csvMapper = csvMapper;
        this.quayRepository = quayRepository;
    }

    @GET
    @Path("/mapping/stop_place")
    @Produces("text/plain")
    public Response getIdMapping(@DefaultValue(value = "300000") @QueryParam(value = "recordsPerRoundTrip") int recordsPerRoundTrip,
                                        @QueryParam("includeStopType") boolean includeStopType, @QueryParam("includeFuture") boolean includeFuture) throws InterruptedException {

        dtoMappingSemaphore.aquire();
        try {
            logger.info("Fetching StopPlace mapping table...");

            return Response.ok().entity((StreamingOutput) output -> {

                int recordPosition = 0;
                boolean lastEmpty = false;
                Instant validFrom = Instant.now();
                Instant validTo = includeFuture ? null : validFrom;
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)))) {
                    while (!lastEmpty) {

                        List<IdMappingDto> stopPlaceMappings = stopPlaceRepository.findKeyValueMappingsForStop(validFrom, validTo, recordPosition, recordsPerRoundTrip);
                        for (IdMappingDto mapping : stopPlaceMappings) {
                            writer.println(csvMapper.toCsvString(mapping, includeStopType, includeFuture));
                            recordPosition++;
                        }
                        writer.flush();
                        if (stopPlaceMappings.isEmpty()) lastEmpty = true;
                    }
                    writer.close();
                }
            }).build();
        } finally {
            dtoMappingSemaphore.release();
        }
    }

    @GET
    @Path("/id/stop_place")
    @Produces("text/plain")
    public String getIdUniqueStopPlaceIds(@QueryParam("includeFuture") boolean includeFuture) {
        Instant validFrom = Instant.now();
        Instant validTo = includeFuture ? null : validFrom;
        return String.join("\n", stopPlaceRepository.findUniqueStopPlaceIds(validFrom, validTo));
    }

    @GET
    @Path("/list/stop_place_quays")
    public Map<String, Set<String>> listStopPlaceQuays(@QueryParam("includeFuture") boolean includeFuture) {
        Instant validFrom = Instant.now();
        Instant validTo = includeFuture ? null : validFrom;
        return stopPlaceRepository.listStopPlaceIdsAndQuayIds(validFrom, validTo);
    }

    @GET
    @Path("/tad_stop_place_from_quay")
    @Produces("application/xml")
    public JAXBElement<org.rutebanken.netex.model.StopPlace> getTadStopPlaceFromQuayId(@QueryParam("quayId") String netexQuayId) {

        Optional<Quay> quayOpt = quayRepository.findTADQuay(netexQuayId);
        if (quayOpt.isEmpty()){
            return null;
        }

        Quay quay = quayOpt.get();
        StopPlace stopPlace = stopPlaceRepository.findByQuay(quay);

        org.rutebanken.netex.model.StopPlace lightNetex = convertToLightNetex(stopPlace);
        return netexObjectFactory.createStopPlace(lightNetex);
    }

    @GET
    @Path("/stop_places")
    @Produces("application/xml")
    public JAXBElement<PublicationDeliveryStructure> getKeyValueStopPlace(@QueryParam(value = "key") String key, @QueryParam(value = "value") String value) {

        List<StopPlace> stopPlaces;
        if(StringUtils.hasLength(key)){
            stopPlaces = stopPlaceRepository.findAllFromKeyValue(key, value);
        } else {
            stopPlaces = stopPlaceRepository.findAll();
        }

        //tiamat.StopPlace to netex.StopPlace to List<JAXBElement<? extends EntityStructure>>
        List<JAXBElement<? extends EntityStructure>> netexStopPlaces = stopPlaces.stream()
                .map(stopPlace -> netexMapper.mapToNetexModel(stopPlace))
                .map(netexObjectFactory::createStopPlace).collect(Collectors.toList());

        //creating GeneralFrame
        org.rutebanken.tiamat.model.GeneralFrame generalFrame = new org.rutebanken.tiamat.model.GeneralFrame();
        String localDateTimeString = LocalDateTime.now() + "Z";
        localDateTimeString = localDateTimeString.replace("-", "");
        localDateTimeString = localDateTimeString.replace(":", "");
        generalFrame.setModification(org.rutebanken.tiamat.model.ModificationEnumeration.REVISE);
        generalFrame.setVersion(1L);
        generalFrame.setNetexId("GeneralFrame:NETEX_ARRET_" + localDateTimeString + ":LOC");
        org.rutebanken.tiamat.model.TypeOfFrameRefStructure typeOfFrameRefStructure = new org.rutebanken.tiamat.model.TypeOfFrameRefStructure();
        typeOfFrameRefStructure.setRef("FR:TypeOfFrame:NETEX_ARRET");
        typeOfFrameRefStructure.setValue("version=\"1.1:FR-NETEX_ARRET-2.2\"");
        generalFrame.setTypeOfFrameRef(typeOfFrameRefStructure);

        org.rutebanken.netex.model.GeneralFrame netexGeneralFrame = netexMapper.mapToNetexModel(generalFrame);

        //adding members to the frame
        General_VersionFrameStructure.Members general_VersionFrameStructure = netexObjectFactory.createGeneral_VersionFrameStructureMembers();
        general_VersionFrameStructure.withGeneralFrameMemberOrDataManagedObjectOrEntity_Entity(netexStopPlaces);
        netexGeneralFrame.withMembers(general_VersionFrameStructure);

        //creating publicationDelivery
        PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryExporter.createPublicationDelivery(netexGeneralFrame,"idSite", LocalDateTime.now());

        return netexObjectFactory.createPublicationDelivery(publicationDeliveryStructure);
    }


    /**
     * Creates a light netex stop place with only few informations (name/id/location)
     * @param stopPlace
     * @return
     */
    private org.rutebanken.netex.model.StopPlace convertToLightNetex(StopPlace stopPlace){

        org.rutebanken.netex.model.StopPlace lightStopPlace = new org.rutebanken.netex.model.StopPlace();
        lightStopPlace.setId(stopPlace.getNetexId());
        if (stopPlace.getName() != null){
            lightStopPlace.setName(convertToMultiLingualString(stopPlace.getName()));
        }

        try {
            convertQuaysToLightNetex(lightStopPlace, stopPlace);
        }catch(Exception e){
            logger.error("Error while converting to light netex",e);
        }

        return lightStopPlace;
    }

    private void convertQuaysToLightNetex(org.rutebanken.netex.model.StopPlace lightStopPlace, StopPlace stopPlace) {
        List<JAXBElement<?>> netexLightQuays = new ArrayList<>();
        for (Quay quay : stopPlace.getQuays()) {

            org.rutebanken.netex.model.Quay netexQuay = new org.rutebanken.netex.model.Quay();
            netexQuay.setId(quay.getNetexId());
            if (quay.getName() != null){
                netexQuay.setName(convertToMultiLingualString(quay.getName()));
            }
            Point centroid = quay.getCentroid();
            SimplePoint_VersionStructure simplePoint = new SimplePoint_VersionStructure();
            LocationStructure location = new LocationStructure();
            location.setLongitude(BigDecimal.valueOf(centroid.getX()));
            location.setLatitude(BigDecimal.valueOf(centroid.getY()));
            simplePoint.setLocation(location);
            netexQuay.setCentroid(simplePoint);
            netexLightQuays.add(netexObjectFactory.createQuay(netexQuay));
        }
        Quays_RelStructure quays = new Quays_RelStructure();
        quays.withQuayRefOrQuay(netexLightQuays);
        lightStopPlace.setQuays(quays);
    }

    private MultilingualString convertToMultiLingualString(EmbeddableMultilingualString emString) {
        MultilingualString result = new MultilingualString();

        result.setValue(emString.getValue());
        return result;
    }


}
