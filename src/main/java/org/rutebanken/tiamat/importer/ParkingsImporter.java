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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.helper.organisation.NotAuthenticatedException;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.rutebanken.netex.model.GeneralFrame;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.importer.handler.*;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.repository.CacheProviderRepository;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;
import static org.rutebanken.tiamat.netex.mapping.NetexMappingContextThreadLocal.updateMappingGeneralFrameContext;
@Service
public class ParkingsImporter {

    @Autowired
    protected CacheProviderRepository providerRepository;

    private static final Logger logger = LoggerFactory.getLogger(ParkingsImporter.class);

    public static final String ASYNC_IMPORT_JOB_PATH = "import_parking";

    private static final ExecutorService exportService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    public static final String IMPORT_CORRELATION_ID = "importCorrelationId";
    public static final String KC_ROLE_PREFIX = "ROLE_";

    private final PublicationDeliveryHelper publicationDeliveryHelper;
    private final ParkingsImportHandler parkingsImportHandler;
    private final RoleAssignmentExtractor roleAssignmentExtractor;

    @Autowired
    public ParkingsImporter(PublicationDeliveryHelper publicationDeliveryHelper,
                            ParkingsImportHandler parkingsImportHandler,
                            RoleAssignmentExtractor roleAssignmentExtractor) {
        this.publicationDeliveryHelper = publicationDeliveryHelper;
        this.parkingsImportHandler = parkingsImportHandler;
        this.roleAssignmentExtractor = roleAssignmentExtractor;
    }


    public void importParkings(PublicationDeliveryStructure publicationDeliveryStructure, String providerId, String fileName) throws TiamatBusinessException {
        importParkings(publicationDeliveryStructure, null, providerId, fileName);
    }

    @SuppressWarnings("unchecked")
    public Response.ResponseBuilder importParkings(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams, String providerId, String fileName) throws TiamatBusinessException {

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
            validate(importParams);
        }

        logger.info("Got publication delivery with {} site frames and description {}",
                publicationDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame().size(),
                publicationDeliveryStructure.getDescription());

        AtomicInteger parkingCounter = new AtomicInteger(0);
        GeneralFrame netexGeneralFrame = publicationDeliveryHelper.findGeneralFrame(publicationDeliveryStructure);
        String requestId = netexGeneralFrame.getId();
        updateMappingGeneralFrameContext(netexGeneralFrame);
        Provider provider = getCurrentProvider(providerId);
        Job job = new Job();
        Response.ResponseBuilder builder = Response.accepted();

        try {
            GeneralFrame responseGeneralFrame = new GeneralFrame();
            MDC.put(IMPORT_CORRELATION_ID, requestId);
            logger.info("Publication delivery contains site frame created at {}", netexGeneralFrame.getCreated());
            responseGeneralFrame.withId(requestId + "-response").withVersion("1");
            manageJob(job, JobStatus.PROCESSING, importParams, provider, fileName, null);
            parkingsImportHandler.handleParkingsGeneralFrame(netexGeneralFrame, importParams, parkingCounter, responseGeneralFrame, provider, fileName, job);
            return builder.location(URI.create("/services/stop_places/jobs/"+provider.name+"/scheduled_jobs/"+job.getId()));
        } catch (Exception e) {
            manageJob(job, JobStatus.FAILED, importParams, provider, fileName, e);
            throw new RuntimeException(e);
        } finally {
            MDC.remove(IMPORT_CORRELATION_ID);
        }
    }

    private Provider getCurrentProvider(String providerId) {
        providerRepository.populate();
        Collection<Provider> providers = providerRepository.getProviders();
        Optional<Provider> findProvider = providers.stream()
                .filter(provider -> Objects.equals(provider.getId(), Long.valueOf(providerId)))
                .findFirst();

        return findProvider.orElse(null);
    }


    private void validate(ImportParams importParams) {
        if (importParams.targetTopographicPlaces != null && importParams.onlyMatchOutsideTopographicPlaces != null) {
            if (!importParams.targetTopographicPlaces.isEmpty() && !importParams.onlyMatchOutsideTopographicPlaces.isEmpty()) {
                throw new IllegalArgumentException("targetTopographicPlaces and onlyMatchOutsideTopographicPlaces cannot be specified at the same time!");
            }
        }
    }

    public static Job manageJob(Job job, JobStatus state, ImportParams importParams, Provider provider, String fileName, Exception exception) {

        if (state.equals(JobStatus.FAILED)) {
            String message = "Error executing import job " + job.getId() + ". " + exception.getClass().getSimpleName() + " - " + exception.getMessage();
            logger.error("{}.\nImport job was {}", message, job, exception);
            job.setMessage(message);
        }

        else if (state.equals(JobStatus.PROCESSING)) {
            job.setStatus(state);
            if(provider != null) {
                logger.info("Starting import {} for provider {}", job.getId(), provider.id + "/" + provider.chouetteInfo.codeIdfm);
                job.setStarted(Instant.now());
                job.setImportParams(importParams);
                job.setSubFolder(provider.name);
                job.setMessage(null);

                String nameFileXml = null;

                try {
                    nameFileXml = URLEncoder.encode(fileName, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                job.setFileName(nameFileXml);

                ImportJobWorker importJobWorker = new ImportJobWorker(job);
                exportService.submit(importJobWorker);
                logger.info("Returning started import job {}", job);
                setJobUrl(job);
            }
        }

        else if (state.equals(JobStatus.FINISHED)) {
            job.setFinished(Instant.now());
        }



        return job;
    }

    private static Job setJobUrl(Job jobWithId) {
        jobWithId.setJobUrl(ASYNC_IMPORT_JOB_PATH + "/" + jobWithId.getId());
        return jobWithId;
    }
}
