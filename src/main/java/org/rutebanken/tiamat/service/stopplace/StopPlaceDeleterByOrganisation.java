package org.rutebanken.tiamat.service.stopplace;

import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.changelog.LoggingService;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.lock.MutateLock;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class StopPlaceDeleterByOrganisation {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceDeleterByOrganisation.class);


    private final StopPlaceRepository stopPlaceRepository;
    private final UsernameFetcher usernameFetcher;
    private final MutateLock mutateLock;
    private final MdmService mdmService;
    private final LoggingService loggingService;

    public StopPlaceDeleterByOrganisation(StopPlaceRepository stopPlaceRepository, UsernameFetcher usernameFetcher, MutateLock mutateLock, MdmService mdmService, LoggingService loggingService) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.usernameFetcher = usernameFetcher;
        this.mutateLock = mutateLock;
        this.mdmService = mdmService;
        this.loggingService = loggingService;
    }

    public boolean deleteStopPlacesByOrganisation(String organisation) {

        return mutateLock.executeInLock(() -> {
            String usernameForAuthenticatedUser = usernameFetcher.getUserNameForAuthenticatedUser();
            logger.warn("About to delete stop place by organisation {}. User: {}", organisation, usernameForAuthenticatedUser);
            loggingService.logStopPlaceDeleteByOrganisation(usernameForAuthenticatedUser, organisation);
            mdmService.deleteStopPlaceAndQuayIdsByDataset(organisation);
            return stopPlaceRepository.deleteAllStopPlacesQuaysByOrganisation(organisation);
        });
    }
}
