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

package org.rutebanken.tiamat.importer;

import org.rutebanken.helper.organisation.NotAuthenticatedException;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.rutebanken.netex.model.GeneralFrame;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.importer.handler.QuaysImportHandler;
import org.rutebanken.tiamat.importer.handler.StopPlacesImportHandler;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobImportType;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.repository.CacheProviderRepository;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.rest.utils.Importer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;
import static org.rutebanken.tiamat.netex.mapping.NetexMappingContextThreadLocal.updateMappingGeneralFrameContext;

@Service
public class StopPlacesImporter {

    @Autowired
    protected CacheProviderRepository providerRepository;

    private static final Logger logger = LoggerFactory.getLogger(StopPlacesImporter.class);
    public static final String IMPORT_CORRELATION_ID = "importCorrelationId";
    public static final String KC_ROLE_PREFIX = "ROLE_";
    private final PublicationDeliveryHelper publicationDeliveryHelper;
    private final StopPlacesImportHandler stopPlacesImportHandler;
    private final QuaysImportHandler quaysImportHandler;
    private final RoleAssignmentExtractor roleAssignmentExtractor;

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private QuayRepository quayRepository;

    @Autowired
    public StopPlacesImporter(PublicationDeliveryHelper publicationDeliveryHelper,
                              StopPlacesImportHandler stopPlacesImportHandler,
                              QuaysImportHandler quaysImportHandler, RoleAssignmentExtractor roleAssignmentExtractor) {
        this.publicationDeliveryHelper = publicationDeliveryHelper;
        this.stopPlacesImportHandler = stopPlacesImportHandler;
        this.quaysImportHandler = quaysImportHandler;
        this.roleAssignmentExtractor = roleAssignmentExtractor;
    }


    public void importStopPlaces(PublicationDeliveryStructure publicationDeliveryStructure, String providerId, String fileName, String folder) throws TiamatBusinessException {
        importStopPlaces(publicationDeliveryStructure, null, providerId, fileName, folder);
    }

    @SuppressWarnings("unchecked")
    public Response.ResponseBuilder importStopPlaces(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams, String providerId, String fileName, String folder) throws TiamatBusinessException {

        if (roleAssignmentExtractor.getRoleAssignmentsForUser()
                .stream()
                .noneMatch(roleAssignment -> roleAssignment.r.equals(ROLE_EDIT_STOPS)) &&
                SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                        .stream()
                        .noneMatch(authority -> authority.getAuthority().equals(KC_ROLE_PREFIX + ROLE_EDIT_STOPS))) {
            throw new NotAuthenticatedException("Role: '" + ROLE_EDIT_STOPS + "' required for import!");
        }

        if (publicationDeliveryStructure.getDataObjects() == null) {
            String responseMessage = "Received publication delivery but it does not contain any data objects.";
            logger.warn(responseMessage);
            throw new RuntimeException(responseMessage);
        }

        if (importParams == null) {
            importParams = new ImportParams();
        } else {
            Importer.validate(importParams);
        }

        logger.info("Got publication delivery with {} site frames and description {}",
                publicationDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame().size(),
                publicationDeliveryStructure.getDescription());

        AtomicInteger stopPlacesCounter = new AtomicInteger(0);
        GeneralFrame netexGeneralFrame = publicationDeliveryHelper.findGeneralFrame(publicationDeliveryStructure);
        String requestId = netexGeneralFrame.getId();
        updateMappingGeneralFrameContext(netexGeneralFrame);
        Provider provider = getCurrentProvider(providerId);
        Job job = new Job();
        jobRepository.save(job);
        Response.ResponseBuilder builder = Response.accepted();

        try {
            GeneralFrame responseGeneralFrame = new GeneralFrame();
            MDC.put(IMPORT_CORRELATION_ID, requestId);
            logger.info("Publication delivery contains site frame created at {}", netexGeneralFrame.getCreated());
            responseGeneralFrame.withId(requestId + "-response").withVersion("1");
            Importer.manageJob(job, JobStatus.PROCESSING, importParams, provider, fileName, folder, null, JobImportType.NETEX_STOP_PlACE_QUAY);
            jobRepository.save(job);
            quaysImportHandler.handleQuaysGeneralFrame(netexGeneralFrame, importParams, stopPlacesCounter, provider, fileName, folder, job);
            stopPlacesImportHandler.handleStopPlacesGeneralFrame(netexGeneralFrame, importParams, stopPlacesCounter, provider, fileName, folder, job);
            if (provider != null) {
                return builder.location(URI.create("/services/stop_places/jobs/"+provider.name+"/scheduled_jobs/"+job.getId()));
            } else {
                return builder;
            }
        } catch (Exception e) {
            Importer.manageJob(job, JobStatus.FAILED, importParams, provider, fileName, folder, e, JobImportType.NETEX_STOP_PlACE_QUAY);
            jobRepository.save(job);
            throw new RuntimeException(e);
        } finally {
            MDC.remove(IMPORT_CORRELATION_ID);
        }
    }

    public Provider getCurrentProvider(String providerId) {
        providerRepository.populate();
        Collection<Provider> providers = providerRepository.getProviders();

        try {
            Long id = Long.valueOf(providerId);
            Optional<Provider> findProvider = providers.stream()
                    .filter(provider -> Objects.equals(provider.getId(), id))
                    .findFirst();

            return findProvider.orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
