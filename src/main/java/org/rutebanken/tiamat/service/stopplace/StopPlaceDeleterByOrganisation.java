package org.rutebanken.tiamat.service.stopplace;

import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.lock.MutateLock;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class StopPlaceDeleterByOrganisation {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceDeleterByOrganisation.class);


    private final StopPlaceRepository stopPlaceRepository;
    private final UsernameFetcher usernameFetcher;
    private final MutateLock mutateLock;
    private final MdmService mdmService;

    public StopPlaceDeleterByOrganisation(StopPlaceRepository stopPlaceRepository, UsernameFetcher usernameFetcher, MutateLock mutateLock, MdmService mdmService) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.usernameFetcher = usernameFetcher;
        this.mutateLock = mutateLock;
        this.mdmService = mdmService;
    }

    public boolean deleteStopPlacesByOrganisation(String organisation) {

        return mutateLock.executeInLock(() -> {
            String usernameForAuthenticatedUser = usernameFetcher.getUserNameForAuthenticatedUser();
            logger.warn("About to delete stop place by organisation {}. User: {}", organisation, usernameForAuthenticatedUser);
            mdmService.deleteStopPlaceAndQuayIdsByDataset(organisation);
            return stopPlaceRepository.deleteAllStopPlacesQuaysByOrganisation(organisation);
        });
    }
}
