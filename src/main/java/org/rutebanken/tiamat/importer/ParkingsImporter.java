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
import org.rutebanken.tiamat.exporter.PublicationDeliveryExporter;
import org.rutebanken.tiamat.importer.handler.*;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.repository.CacheProviderRepository;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.service.batch.BackgroundJobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;
import static org.rutebanken.tiamat.netex.mapping.NetexMappingContextThreadLocal.updateMappingGeneralFrameContext;

@Service
public class ParkingsImporter {

    @Autowired
    protected CacheProviderRepository providerRepository;

    private static final Logger logger = LoggerFactory.getLogger(ParkingsImporter.class);

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


    public void importParkings(PublicationDeliveryStructure publicationDeliveryStructure) throws TiamatBusinessException {
        importParkings(publicationDeliveryStructure, null);
    }

    @SuppressWarnings("unchecked")
    public void importParkings(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams) throws TiamatBusinessException {

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

        try {
            GeneralFrame responseGeneralFrame = new GeneralFrame();
            MDC.put(IMPORT_CORRELATION_ID, requestId);
            logger.info("Publication delivery contains site frame created at {}", netexGeneralFrame.getCreated());
            responseGeneralFrame.withId(requestId + "-response").withVersion("1");
            parkingsImportHandler.handleParkingsGeneralFrame(netexGeneralFrame, importParams, parkingCounter, responseGeneralFrame);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            MDC.remove(IMPORT_CORRELATION_ID);
        }
    }


    private void validate(ImportParams importParams) {
        if (importParams.targetTopographicPlaces != null && importParams.onlyMatchOutsideTopographicPlaces != null) {
            if (!importParams.targetTopographicPlaces.isEmpty() && !importParams.onlyMatchOutsideTopographicPlaces.isEmpty()) {
                throw new IllegalArgumentException("targetTopographicPlaces and onlyMatchOutsideTopographicPlaces cannot be specified at the same time!");
            }
        }
    }

}
