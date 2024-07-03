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
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.netex.mapping.NetexMappingContextThreadLocal.updateMappingContext;
import static org.rutebanken.tiamat.netex.mapping.NetexMappingContextThreadLocal.updateMappingGeneralFrameContext;

@Service
public class NetexImporter {

    @Autowired
    protected CacheProviderRepository providerRepository;

    private static final Logger logger = LoggerFactory.getLogger(NetexImporter.class);
    public static final String IMPORT_CORRELATION_ID = "importCorrelationId";
    private final PublicationDeliveryHelper publicationDeliveryHelper;
    private final ParkingsImportHandler parkingsImportHandler;
    private final StopPlacesImportHandler stopPlacesImportHandler;
    private final PointOfInterestsImportHandler pointOfInterestsImportHandler;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    public NetexImporter(PublicationDeliveryHelper publicationDeliveryHelper,
                         ParkingsImportHandler parkingsImportHandler,
                         StopPlacesImportHandler stopPlacesImportHandler,
                         PointOfInterestsImportHandler pointOfInterestsImportHandler) {
        this.publicationDeliveryHelper = publicationDeliveryHelper;
        this.parkingsImportHandler = parkingsImportHandler;
        this.stopPlacesImportHandler = stopPlacesImportHandler;
        this.pointOfInterestsImportHandler = pointOfInterestsImportHandler;
    }

    public Response.ResponseBuilder importProcessTest(PublicationDeliveryStructure publicationDeliveryStructure, String providerId, String fileName, Boolean containsMobiitiIds, JobImportType jobType) throws TiamatBusinessException {
        return importProcess(publicationDeliveryStructure, providerId, fileName, "/", containsMobiitiIds, jobType);
    }

    @SuppressWarnings("unchecked")
    public Response.ResponseBuilder importProcess(PublicationDeliveryStructure publicationDeliveryStructure,
                                                  String providerId, String fileName, String folder, Boolean containsMobiitiIds, JobImportType jobType) {

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
        List<javax.xml.bind.JAXBElement<? extends org.rutebanken.netex.model.Common_VersionFrameStructure>> findedFrameType = publicationDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame();

        try {
            List<GeneralOrganisation> generalOrganisations = new ArrayList<>();
            List<ResponsibilitySet> responsibilitySets = new ArrayList<>();
            List<JAXBElement<? extends EntityStructure>> members = null;

            // Première boucle pour traiter GeneralFrame et récupérer les GeneralOrganisations et ResponsibilitySets
            for (JAXBElement<? extends Common_VersionFrameStructure> frameType : findedFrameType) {
                if (frameType.getValue() instanceof GeneralFrame) {
                    members = getMembers(publicationDeliveryStructure, importParams, fileName, folder, jobType, provider, job);
                    assert members != null;
                    generalOrganisations = NetexUtils.getMembers(GeneralOrganisation.class, members);
                    responsibilitySets = NetexUtils.getMembers(ResponsibilitySet.class, members);
                    generalFrameProcess(members, importParams, fileName, folder, jobType, provider, job, atomicInteger, generalOrganisations, responsibilitySets, containsMobiitiIds);
                    break; // Quitter la boucle après avoir trouvé et traité le GeneralFrame
                }
            }

            // Deuxième boucle pour traiter SiteFrame et utiliser les GeneralOrganisations et ResponsibilitySets si disponibles
            for (JAXBElement<? extends Common_VersionFrameStructure> frameType : findedFrameType) {
                if (frameType.getValue() instanceof SiteFrame) {
                    // Si les GeneralOrganisations et ResponsibilitySets ont été trouvés, les utiliser
                    siteFrameProcess(publicationDeliveryStructure, importParams, fileName, folder, jobType, provider, job, atomicInteger, generalOrganisations, responsibilitySets);
                    break; // Quitter la boucle après avoir trouvé et traité le SiteFrame
                }
            }

            updateJobState(JobStatus.FINISHED, importParams, fileName, folder, jobType, provider, job);

            if (provider != null) {
                return builder.location(URI.create(String.format("/services/stop_places/jobs/%s/scheduled_jobs/%d", folder, job.getId())));
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

    private void siteFrameProcess(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams, String fileName, String folder, JobImportType jobType, Provider provider, Job job, AtomicInteger atomicInteger, List<GeneralOrganisation> generalOrganisations, List<ResponsibilitySet> responsibilitySets) {
        SiteFrame netexSiteFrame = publicationDeliveryHelper.findSiteFrame(publicationDeliveryStructure);
        String requestId = netexSiteFrame.getId();
        updateMappingContext(netexSiteFrame);

        SiteFrame responseSiteFrame = new SiteFrame();
        MDC.put(IMPORT_CORRELATION_ID, requestId);
        logger.info("Publication delivery contains site frame created at {}", netexSiteFrame.getCreated());
        responseSiteFrame.withId(requestId + "-response").withVersion("1");

        if (publicationDeliveryHelper.hasPointOfInterests(netexSiteFrame)) {
            pointOfInterestImport(importParams, fileName, folder, jobType, provider, job, atomicInteger, netexSiteFrame, responseSiteFrame, generalOrganisations, responsibilitySets);
        }
    }

    private void pointOfInterestImport(ImportParams importParams, String fileName, String folder, JobImportType jobType, Provider provider, Job job, AtomicInteger atomicInteger, SiteFrame netexSiteFrame, SiteFrame responseSiteFrame, List<GeneralOrganisation> generalOrganisations, List<ResponsibilitySet> responsibilitySets) {
        updateJobState(JobStatus.PROCESSING, importParams, fileName, folder, jobType, provider, job);
        pointOfInterestsImportHandler.handlePointOfInterests(netexSiteFrame, importParams, atomicInteger, responseSiteFrame, provider, fileName, folder, job, generalOrganisations, responsibilitySets);
    }

    private List<JAXBElement<? extends EntityStructure>> getMembers(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams, String fileName, String folder, JobImportType jobType, Provider provider, Job job) {
        GeneralFrame netexGeneralFrame = publicationDeliveryHelper.findGeneralFrame(publicationDeliveryStructure);
        String requestId = netexGeneralFrame.getId();
        updateMappingGeneralFrameContext(netexGeneralFrame);

        GeneralFrame responseGeneralFrame = new GeneralFrame();
        MDC.put(IMPORT_CORRELATION_ID, requestId);
        logger.info("Publication delivery contains site frame created at {}", netexGeneralFrame.getCreated());
        responseGeneralFrame.withId(requestId + "-response").withVersion("1");

        if (publicationDeliveryHelper.hasGeneralFrame(netexGeneralFrame)) {
            return netexGeneralFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();
        }
        return null;
    }

    private void generalFrameProcess(List<JAXBElement<? extends EntityStructure>> members, ImportParams importParams, String fileName, String folder, JobImportType jobType, Provider provider, Job job, AtomicInteger atomicInteger, List<GeneralOrganisation> generalOrganisations, List<ResponsibilitySet> responsibilitySets, Boolean containsMobiitiIds) {
        updateJobState(JobStatus.PROCESSING, importParams, fileName, folder, jobType, provider, job);

        if (!members.isEmpty()) {
            if (members.stream().anyMatch(mem -> mem.getValue() instanceof Parking)) {
                parkingsImport(importParams, atomicInteger, members, generalOrganisations, responsibilitySets);
            }

            else if (members.stream().anyMatch(mem -> mem.getValue() instanceof StopPlace || mem.getValue() instanceof Quay)) {
                stopPlaceAndQuayImport(importParams, atomicInteger, members, containsMobiitiIds);
            }
        }
    }

    private void stopPlaceAndQuayImport(ImportParams importParams, AtomicInteger atomicInteger, List<JAXBElement<? extends EntityStructure>> members, Boolean containsMobiitiIds) {
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
        stopPlacesImportHandler.handleStopPlacesGeneralFrame(tiamatStopPlaces, importParams, members, atomicInteger, quaysParsed, containsMobiitiIds);
    }

    private void parkingsImport(ImportParams importParams, AtomicInteger atomicInteger, List<JAXBElement<? extends EntityStructure>> members, List<GeneralOrganisation> generalOrganisations, List<ResponsibilitySet> responsibilitySets) {
        List<Parking> tiamatParking = NetexUtils.getMembers(Parking.class, members);
        parkingsImportHandler.handleParkingsGeneralFrame(tiamatParking, importParams, members, atomicInteger, generalOrganisations, responsibilitySets);
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
        netexQuaysInFrame.forEach(netexQuay -> {
            org.rutebanken.tiamat.model.Quay currentQuay = netexMapper.mapToTiamatModel(netexQuay);
            if (currentQuay.getNetexId() == null) currentQuay.setNetexId(netexQuay.getId());
            quaysList.add(currentQuay);

        });
        return quaysList;
    }
}
