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

import org.jetbrains.annotations.NotNull;
import org.rutebanken.helper.organisation.NotAuthenticatedException;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.importer.handler.ParkingsImportHandler;
import org.rutebanken.tiamat.importer.handler.PointOfInterestsImportHandler;
import org.rutebanken.tiamat.importer.handler.StopPlacesImportHandler;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobImportType;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.NetexUtils;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.repository.CacheProviderRepository;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.rest.utils.Importer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;
import static org.rutebanken.tiamat.netex.mapping.NetexMappingContextThreadLocal.updateMappingContext;
import static org.rutebanken.tiamat.netex.mapping.NetexMappingContextThreadLocal.updateMappingGeneralFrameContext;

@Service
public class NetexImporter {

    @Autowired
    protected CacheProviderRepository providerRepository;

    private static final Logger logger = LoggerFactory.getLogger(NetexImporter.class);
    public static final String IMPORT_CORRELATION_ID = "importCorrelationId";
    public static final String KC_ROLE_PREFIX = "ROLE_";
    private final PublicationDeliveryHelper publicationDeliveryHelper;
    private final ParkingsImportHandler parkingsImportHandler;
    private final StopPlacesImportHandler stopPlacesImportHandler;
    private final PointOfInterestsImportHandler pointOfInterestsImportHandler;
    private final RoleAssignmentExtractor roleAssignmentExtractor;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    public NetexImporter(PublicationDeliveryHelper publicationDeliveryHelper,
                         ParkingsImportHandler parkingsImportHandler,
                         StopPlacesImportHandler stopPlacesImportHandler, PointOfInterestsImportHandler pointOfInterestsImportHandler,
                         RoleAssignmentExtractor roleAssignmentExtractor) {
        this.publicationDeliveryHelper = publicationDeliveryHelper;
        this.parkingsImportHandler = parkingsImportHandler;
        this.stopPlacesImportHandler = stopPlacesImportHandler;
        this.pointOfInterestsImportHandler = pointOfInterestsImportHandler;
        this.roleAssignmentExtractor = roleAssignmentExtractor;
    }

    public Response.ResponseBuilder importProcessTest(PublicationDeliveryStructure publicationDeliveryStructure, String providerId, String fileName, JobImportType jobType) throws TiamatBusinessException {
        return importProcess(publicationDeliveryStructure, providerId, fileName, "/", jobType);
    }

    @SuppressWarnings("unchecked")
    public Response.ResponseBuilder importProcess(PublicationDeliveryStructure publicationDeliveryStructure,
                                                  String providerId, String fileName, String folder, JobImportType jobType) {

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

        ImportParams importParams = new ImportParams();

        logger.info("Got publication delivery with {} site frames and description {}",
                publicationDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame().size(),
                publicationDeliveryStructure.getDescription());

        Provider provider = getCurrentProvider(providerId);
        Job job = new Job();
        jobRepository.save(job);
        Response.ResponseBuilder builder = Response.accepted();

        AtomicInteger atomicInteger = new AtomicInteger(0);
        String locationBuilderPath = null;
        Common_VersionFrameStructure findedFrameType = publicationDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame().get(0).getValue();

        try {
            if (findedFrameType instanceof GeneralFrame) {
                locationBuilderPath = generalFrameProcess(publicationDeliveryStructure, importParams, fileName, folder, jobType, provider, job, atomicInteger, locationBuilderPath);
            }
            else if (findedFrameType instanceof SiteFrame) {
                locationBuilderPath = siteFrameProcess(publicationDeliveryStructure, importParams, fileName, folder, jobType, provider, job, atomicInteger, locationBuilderPath);
            }
            updateJobState(JobStatus.FINISHED, importParams, fileName, folder, jobType, provider, job);

            if (provider != null && locationBuilderPath != null) {
                return builder.location(URI.create("/services/" + locationBuilderPath + "/jobs/" + provider.name + "/scheduled_jobs/" + job.getId()));
            } else {
                return builder;
            }
        } catch (Exception e) {
            Importer.manageJob(job, JobStatus.FAILED, importParams, provider, fileName, folder, e, jobType);
            jobRepository.save(job);
            throw new RuntimeException(e);
        } finally {
            MDC.remove(IMPORT_CORRELATION_ID);
        }
    }

    private String siteFrameProcess(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams, String fileName, String folder, JobImportType jobType, Provider provider, Job job, AtomicInteger atomicInteger, String locationBuilderPath) {
        SiteFrame netexSiteFrame = publicationDeliveryHelper.findSiteFrame(publicationDeliveryStructure);
        String requestId = netexSiteFrame.getId();
        updateMappingContext(netexSiteFrame);

        SiteFrame responseSiteFrame = new SiteFrame();
        MDC.put(IMPORT_CORRELATION_ID, requestId);
        logger.info("Publication delivery contains site frame created at {}", netexSiteFrame.getCreated());
        responseSiteFrame.withId(requestId + "-response").withVersion("1");

        if (publicationDeliveryHelper.hasPointOfInterests(netexSiteFrame)) {
            locationBuilderPath = pointOfInterestImport(importParams, fileName, folder, jobType, provider, job, atomicInteger, netexSiteFrame, responseSiteFrame);
        }
        return locationBuilderPath;
    }

    @NotNull
    private String pointOfInterestImport(ImportParams importParams, String fileName, String folder, JobImportType jobType, Provider provider, Job job, AtomicInteger atomicInteger, SiteFrame netexSiteFrame, SiteFrame responseSiteFrame) {
        updateJobState(JobStatus.PROCESSING, importParams, fileName, folder, jobType, provider, job);
        pointOfInterestsImportHandler.handlePointOfInterests(netexSiteFrame, importParams, atomicInteger, responseSiteFrame, provider, fileName, folder, job);
        return "pois";
    }

    private String generalFrameProcess(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams, String fileName, String folder, JobImportType jobType, Provider provider, Job job, AtomicInteger atomicInteger, String locationBuilderPath) {
        GeneralFrame netexGeneralFrame = publicationDeliveryHelper.findGeneralFrame(publicationDeliveryStructure);
        String requestId = netexGeneralFrame.getId();
        updateMappingGeneralFrameContext(netexGeneralFrame);

        GeneralFrame responseGeneralFrame = new GeneralFrame();
        MDC.put(IMPORT_CORRELATION_ID, requestId);
        logger.info("Publication delivery contains site frame created at {}", netexGeneralFrame.getCreated());
        responseGeneralFrame.withId(requestId + "-response").withVersion("1");

        if (publicationDeliveryHelper.hasGeneralFrame(netexGeneralFrame)) {
            updateJobState(JobStatus.PROCESSING, importParams, fileName, folder, jobType, provider, job);

            List<JAXBElement<? extends EntityStructure>> members = netexGeneralFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();

            if (!members.isEmpty()) {
                EntityStructure firstMember = members.get(0).getValue();

                if (firstMember instanceof Parking) {
                    locationBuilderPath = parkingsImport(importParams, atomicInteger, members);
                }

                else if (firstMember instanceof StopPlace || firstMember instanceof Quay) {
                    locationBuilderPath = stopPlaceAndQuayImport(importParams, atomicInteger, members);
                }
            }
        }
        return locationBuilderPath;
    }

    @NotNull
    private String stopPlaceAndQuayImport(ImportParams importParams, AtomicInteger atomicInteger, List<JAXBElement<? extends EntityStructure>> members) {
        // Récupération de tous les quay présents dans le netex
        List<Quay> tiamatQuays = members.stream()
                .filter(member -> member.getValue() instanceof Quay)
                .map(member -> (Quay) member.getValue())
                .collect(Collectors.toList());

        // Récupération de tous les stop places présents dans le netex
        List<StopPlace> tiamatStopPlaces = members.stream()
                .filter(member -> member.getValue() instanceof StopPlace)
                .map(member -> (StopPlace) member.getValue())
                .collect(Collectors.toList());

        List<org.rutebanken.tiamat.model.Quay> quaysParsed = mapQuaysToTiamatModel(tiamatQuays);
        stopPlacesImportHandler.handleStopPlacesGeneralFrame(tiamatStopPlaces, importParams, members, atomicInteger, quaysParsed);

        return "stop_places";
    }

    @NotNull
    private String parkingsImport(ImportParams importParams, AtomicInteger atomicInteger, List<JAXBElement<? extends EntityStructure>> members) {
        List<Parking> tiamatParking = NetexUtils.getMembers(Parking.class, members);
        parkingsImportHandler.handleParkingsGeneralFrame(tiamatParking, importParams, members, atomicInteger);
        return "parkings";
    }

    private void updateJobState(JobStatus jobStatus, ImportParams importParams, String fileName, String folder, JobImportType jobType, Provider provider, Job job) {
        Job jobUpdated = Importer.manageJob(job, jobStatus, importParams, provider, fileName, folder, jobType);
        jobRepository.save(jobUpdated);
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

    public List<org.rutebanken.tiamat.model.Quay> mapQuaysToTiamatModel(List<org.rutebanken.netex.model.Quay> netexQuaysInFrame) {
        if (netexQuaysInFrame.isEmpty())
            return null;

        List<org.rutebanken.tiamat.model.Quay> quaysList = new ArrayList<>();
        netexQuaysInFrame.forEach(netexQuay -> quaysList.add(netexMapper.mapToTiamatModel(netexQuay)));
        return quaysList;
    }
}
