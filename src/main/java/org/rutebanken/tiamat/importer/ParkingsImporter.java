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
import org.rutebanken.tiamat.exporter.TypeEnumeration;
import org.rutebanken.tiamat.exporter.StreamingPublicationDelivery;
import org.rutebanken.tiamat.general.JobWorker;
import org.rutebanken.tiamat.importer.handler.*;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.netex.validation.NetexXmlReferenceValidator;
import org.rutebanken.tiamat.repository.CacheProviderRepository;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.service.BlobStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;
import static org.rutebanken.tiamat.netex.mapping.NetexMappingContextThreadLocal.updateMappingGeneralFrameContext;
import static org.rutebanken.tiamat.rest.netex.publicationdelivery.AsyncExportResource.ASYNC_JOB_PATH;

@Service
public class ParkingsImporter {

    @Autowired
    protected CacheProviderRepository providerRepository;

    private static final Logger logger = LoggerFactory.getLogger(ParkingsImporter.class);

    private static final ExecutorService exportService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("exporter-%d").build());

    private static Job job;
    private static StreamingPublicationDelivery streamingPublicationDelivery;
    private static String localExportPath;
    private static String fileNameWithoutExtention;
    private static BlobStoreService blobStoreService;

    @Autowired
    private static JobRepository jobRepository;
    private static NetexXmlReferenceValidator netexXmlReferenceValidator;
    private static Provider provider;
    private LocalDateTime localDateTime; // ne peut pas être final. Vu qu'on ne le bouge pas, pas gênant mais dommage
    private static String tiamatExportDestination;
    private static TypeEnumeration exportType;

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


    public void importParkings(PublicationDeliveryStructure publicationDeliveryStructure, String providerId) throws TiamatBusinessException {
        importParkings(publicationDeliveryStructure, null, providerId);
    }

    @SuppressWarnings("unchecked")
    public void importParkings(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams, String providerId) throws TiamatBusinessException {

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

        try {
            GeneralFrame responseGeneralFrame = new GeneralFrame();
            MDC.put(IMPORT_CORRELATION_ID, requestId);
            logger.info("Publication delivery contains site frame created at {}", netexGeneralFrame.getCreated());
            responseGeneralFrame.withId(requestId + "-response").withVersion("1");
            manageJob(job, JobStatus.PROCESSING, importParams, provider, null);
            parkingsImportHandler.handleParkingsGeneralFrame(netexGeneralFrame, importParams, parkingCounter, responseGeneralFrame, provider, job);
        } catch (Exception e) {
            manageJob(job, JobStatus.FAILED, importParams, provider, e);
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

    public static Job manageJob(Job job, JobStatus state, ImportParams importParams, Provider provider, Exception exception) {

        if (state.equals(JobStatus.FAILED)) {
            String message = "Error executing export job " + job.getId() + ". " + exception.getClass().getSimpleName() + " - " + exception.getMessage();
            logger.error("{}.\nExport job was {}", message, job, exception);
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

                LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
                String idSite = provider.getChouetteInfo().getCodeIdfm();
                String nameSite = provider.name;

                if(StringUtils.isNotBlank(provider.getChouetteInfo().getNameNetexStop())) {
                    nameSite = provider.getChouetteInfo().getNameNetexStop();
                }

                String fileNameWithoutExtention = createFileNameWithoutExtention(idSite, nameSite, localDateTime, false);
                String nameFileXml = null;

                try {
                    nameFileXml = URLEncoder.encode(fileNameWithoutExtention, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                job.setFileName(nameFileXml + ".xml");

                JobWorker jobWorker = new JobWorker(job, streamingPublicationDelivery, localExportPath, fileNameWithoutExtention, blobStoreService, jobRepository, netexXmlReferenceValidator, provider, localDateTime, tiamatExportDestination, TypeEnumeration.IMPORT_PARKING);
                exportService.submit(jobWorker);
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
        jobWithId.setJobUrl(ASYNC_JOB_PATH + "/" + jobWithId.getId());
        return jobWithId;
    }

    public static String createFileNameWithoutExtention(String idSite, String nameSite, LocalDateTime localDateTime, boolean isPrefix) {
        if (isPrefix){
            return "ARRET_" + nameSite + "_" + nameSite + "_T_" + localDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "T" + localDateTime.format(DateTimeFormatter.ofPattern("HHmmss")) + "Z";
        } else {
            return "ARRET_" + idSite + "_" + nameSite + "_T_" + localDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "T" + localDateTime.format(DateTimeFormatter.ofPattern("HHmmss")) + "Z";
        }
    }
}
