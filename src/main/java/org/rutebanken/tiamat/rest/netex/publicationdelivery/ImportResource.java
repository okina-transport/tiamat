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


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.helper.organisation.NotAuthenticatedException;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ImportType;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.importer.PublicationDeliveryImporter;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.versioning.util.AccessibilityAssessmentOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.RequestBody;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;


/**
 * Import publication deliveries
 */
@Component
@Tag(name = "Import resource")
@Produces(MediaType.APPLICATION_XML + "; charset=UTF-8")
@Path("netex")
public class ImportResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportResource.class);

    private final PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;

    private final PublicationDeliveryStreamingOutput publicationDeliveryStreamingOutput;

    private final PublicationDeliveryImporter publicationDeliveryImporter;

    private final Set<ImportType> enabledImportTypes;

    private final QuayRepository quayRepository;
    
    private final StopPlaceRepository stopPlaceRepository;

    private final AccessibilityAssessmentOptimizer accessibilityAssessmentOptimizer;

    @Autowired
    public ImportResource(PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller,
                          PublicationDeliveryStreamingOutput publicationDeliveryStreamingOutput,
                          PublicationDeliveryImporter publicationDeliveryImporter,
                          @Value("#{'${netex.import.enabled.types:ID_MATCH}'.split(',')}") Set<ImportType> enabledImportTypes,
                          QuayRepository quayRepository, StopPlaceRepository stopPlaceRepository,
                          AccessibilityAssessmentOptimizer accessibilityAssessmentOptimizer) {

        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.publicationDeliveryStreamingOutput = publicationDeliveryStreamingOutput;
        this.publicationDeliveryImporter = publicationDeliveryImporter;
        this.enabledImportTypes = enabledImportTypes;
        this.quayRepository = quayRepository;
        this.stopPlaceRepository = stopPlaceRepository;
        this.accessibilityAssessmentOptimizer = accessibilityAssessmentOptimizer;
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML + "; charset=UTF-8")
    public Response importPublicationDelivery(@Parameter(hidden = true) InputStream inputStream, @BeanParam ImportParams importParams) throws IOException, JAXBException, SAXException, TiamatBusinessException, BindException {
        logger.info("Received Netex publication delivery, starting to parse...");
        logger.info(".........................................................(importParams.providerCode = {})", importParams.providerCode);

        ImportType effectiveImportType = safeGetImportType(importParams);
        if (!enabledImportTypes.contains(effectiveImportType)) {
            String error = "ImportType: " + effectiveImportType + " not enabled!";
            logger.warn(error);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        try {
            PublicationDeliveryStructure responsePublicationDelivery = publicationDeliveryImporter.importPublicationDelivery(incomingPublicationDelivery, importParams);
            if (importParams != null && importParams.skipOutput) {
                return Response.ok().build();
            } else {
                return Response.ok(publicationDeliveryStreamingOutput.stream(responsePublicationDelivery)).build();
            }


        } catch (NotAuthenticatedException | NotAuthorizedException e) {
            logger.debug("Access denied for publication delivery: " + e.getMessage(), e);
            throw e;
        } catch (RuntimeException e) {
            logger.warn("Caught exception while importing publication delivery: " + incomingPublicationDelivery, e);
            throw e;
        }
    }

    /**
     * Return specified ImportType or default value if not set.
     */
    private ImportType safeGetImportType(ImportParams importParams) {
        if (importParams == null || importParams.importType == null) {
            return new ImportParams().importType;
        }
        return importParams.importType;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("createTADquays")
    @Transactional
    public Response createTADquays(@HeaderParam("provider") String provider, @RequestBody List<StopPlaceView> stopsToCreate) {
        logger.info("Starting to create TAD quays for provider: {}", provider);

        for (StopPlaceView stopPlaceView : stopsToCreate) {
            if (CollectionUtils.isEmpty(stopPlaceView.getQuays())) {
                continue;
            }

            for (QuayView quayToCreate : stopPlaceView.getQuays()) {
                processQuay(provider, stopPlaceView, quayToCreate);
            }
        }

        logger.info("TAD quays created successfully");
        return Response.ok().build();
    }

    private void processQuay(String provider, StopPlaceView stopPlaceView, QuayView quayToCreate) {
        String importedQuayId = provider + ":Quay:" + quayToCreate.getImportedId();

        List<Quay> existingQuay = quayRepository.findAllByImportedId(importedQuayId);
        if (CollectionUtils.isNotEmpty(existingQuay)) {
            return;
        }

        Quay newQuay = createNewQuay(quayToCreate, importedQuayId);
        newQuay = quayRepository.save(newQuay);

        String stopPlaceImportedId = provider + ":StopPlace:" + stopPlaceView.getImportedId();
        handleStopPlaceLink(stopPlaceImportedId, quayToCreate, newQuay, provider);
    }

    private Quay createNewQuay(QuayView quayToCreate, String importedQuayId) {
        Quay quay = new Quay();
        quay.setName(new EmbeddableMultilingualString(quayToCreate.getName()));
        quay.setNetexId(quayToCreate.getNetexId());
        quay.setCentroid(ImporterUtils.createPoint(quayToCreate.getLongitude().doubleValue(), quayToCreate.getLatitude().doubleValue()));
        quay.setAccessibilityAssessment(accessibilityAssessmentOptimizer.createDefaultAccessibilityAssessment());

        org.rutebanken.tiamat.model.Value importedIdVal = new org.rutebanken.tiamat.model.Value();
        importedIdVal.getItems().add(importedQuayId);

        quay.getKeyValues().put(ORIGINAL_ID_KEY, importedIdVal);
        quay.getKeyValues().put("zonalStopPlace", new org.rutebanken.tiamat.model.Value("yes"));

        return quay;
    }

    private void handleStopPlaceLink(String stopPlaceImportedId, QuayView quayToCreate, Quay newQuay, String provider) {
        List<StopPlace> existingStops = stopPlaceRepository.findAllFromKeyValue(ORIGINAL_ID_KEY, Collections.singleton(stopPlaceImportedId));

        if (existingStops.isEmpty()) {
            StopPlace newStopPlace = new StopPlace();
            newStopPlace.setName(new EmbeddableMultilingualString(quayToCreate.getName(), "FR"));
            newStopPlace.setCentroid(ImporterUtils.createPoint(quayToCreate.getLongitude().doubleValue(), quayToCreate.getLatitude().doubleValue()));
            newStopPlace.setAccessibilityAssessment(accessibilityAssessmentOptimizer.createDefaultAccessibilityAssessment());

            org.rutebanken.tiamat.model.Value importedIdVal = new org.rutebanken.tiamat.model.Value();
            importedIdVal.getItems().add(provider + ":Quay:" + quayToCreate.getImportedId());

            newStopPlace.getKeyValues().put(ORIGINAL_ID_KEY, importedIdVal);
            newStopPlace.getKeyValues().put("zonalStopPlace", new org.rutebanken.tiamat.model.Value("yes"));
            newStopPlace.getQuays().add(newQuay);

            stopPlaceRepository.save(newStopPlace);
        } else {
            for (StopPlace stop : existingStops) {
                StopPlace latestVersion = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(stop.getNetexId());
                if (latestVersion != null && !latestVersion.getQuays().contains(newQuay)) {
                    latestVersion.getQuays().add(newQuay);
                }
            }
        }
    }


}
