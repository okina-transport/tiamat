package org.rutebanken.tiamat.service.stopplace;

import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.lock.MutateLock;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class StopPlaceDeleterByOrganisation {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceDeleterByOrganisation.class);

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private UsernameFetcher usernameFetcher;

    @Autowired
    private MutateLock mutateLock;

    public boolean deleteStopPlacesByOrganisation(String organisation) {

        return mutateLock.executeInLock(() -> {
            String usernameForAuthenticatedUser = usernameFetcher.getUserNameForAuthenticatedUser();
            logger.warn("About to delete stop place by organisation {}. User: {}", organisation, usernameForAuthenticatedUser);

            return stopPlaceRepository.deleteAllStopPlacesQuaysByOrganisation(organisation);
        });
    }
}
