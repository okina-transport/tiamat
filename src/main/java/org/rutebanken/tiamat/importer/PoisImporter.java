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
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.importer.handler.PointOfInterestsImportHandler;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobImportType;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.repository.CacheProviderRepository;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;
import static org.rutebanken.tiamat.netex.mapping.NetexMappingContextThreadLocal.updateMappingContext;

@Service
public class PoisImporter {

    @Autowired
    protected CacheProviderRepository providerRepository;

    private static final Logger logger = LoggerFactory.getLogger(PoisImporter.class);
    public static final String IMPORT_CORRELATION_ID = "importCorrelationId";
    public static final String KC_ROLE_PREFIX = "ROLE_";
    private final PublicationDeliveryHelper publicationDeliveryHelper;
    private final PointOfInterestsImportHandler pointOfInterestsImportHandler;
    private final RoleAssignmentExtractor roleAssignmentExtractor;

    @Autowired
    public PoisImporter(PublicationDeliveryHelper publicationDeliveryHelper,
                        PointOfInterestsImportHandler pointOfInterestsImportHandler,
                        RoleAssignmentExtractor roleAssignmentExtractor) {
        this.publicationDeliveryHelper = publicationDeliveryHelper;
        this.pointOfInterestsImportHandler = pointOfInterestsImportHandler;
        this.roleAssignmentExtractor = roleAssignmentExtractor;
    }


    public void importPointOfInterests(PublicationDeliveryStructure publicationDeliveryStructure, String providerId, String fileName) throws TiamatBusinessException {
        importPointOfInterests(publicationDeliveryStructure, null, providerId, fileName);
    }

    @SuppressWarnings("unchecked")
    public Response.ResponseBuilder importPointOfInterests(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams, String providerId, String fileName) throws TiamatBusinessException {

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

        AtomicInteger pointOfInterestCounter = new AtomicInteger(0);
        SiteFrame netexSiteFrame = publicationDeliveryHelper.findSiteFrame(publicationDeliveryStructure);
        String requestId = netexSiteFrame.getId();
        updateMappingContext(netexSiteFrame);
        Provider provider = Importer.getCurrentProvider(providerId);
        Job job = new Job();
        Response.ResponseBuilder builder = Response.accepted();

        try {
            SiteFrame responseSiteFrame = new SiteFrame();
            MDC.put(IMPORT_CORRELATION_ID, requestId);
            logger.info("Publication delivery contains site frame created at {}", netexSiteFrame.getCreated());
            responseSiteFrame.withId(requestId + "-response").withVersion("1");
            Importer.manageJob(job, JobStatus.PROCESSING, importParams, provider, fileName, null, JobImportType.NETEX_POI);
            pointOfInterestsImportHandler.handlePointOfInterests(netexSiteFrame, importParams, pointOfInterestCounter, responseSiteFrame, provider, fileName, job);
            return builder.location(URI.create("/services/stop_places/jobs/"+provider.name+"/scheduled_jobs/"+job.getId()));
        } catch (Exception e) {
            Importer.manageJob(job, JobStatus.FAILED, importParams, provider, fileName, e, JobImportType.NETEX_POI);
            throw new RuntimeException(e);
        } finally {
            MDC.remove(IMPORT_CORRELATION_ID);
        }
    }
}
