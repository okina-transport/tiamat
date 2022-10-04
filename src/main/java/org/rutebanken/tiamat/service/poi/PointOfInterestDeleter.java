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


package org.rutebanken.tiamat.service.poi;

import org.rutebanken.helper.organisation.ReflectionAuthorizationService;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.changelog.EntityChangedListener;
import org.rutebanken.tiamat.model.DataManagedObjectStructure;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;

@Service
public class PointOfInterestDeleter {

    private static final Logger logger = LoggerFactory.getLogger(PointOfInterestDeleter.class);

    private final EntityChangedListener entityChangedListener;

    private final ReflectionAuthorizationService authorizationService;

    private final UsernameFetcher usernameFetcher;

    private PointOfInterestRepository pointOfInterestRepository;

    private ReferenceResolver referenceResolver;

    @Autowired
    public PointOfInterestDeleter(PointOfInterestRepository pointOfInterestRepository,
                                  EntityChangedListener entityChangedListener,
                                  ReflectionAuthorizationService authorizationService,
                                  UsernameFetcher usernameFetcher, ReferenceResolver referenceResolver) {
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.entityChangedListener = entityChangedListener;
        this.authorizationService = authorizationService;
        this.usernameFetcher = usernameFetcher;
        this.referenceResolver = referenceResolver;
    }

    public boolean deletePointOfInterest(String pointOfInterestId) {

        String usernameForAuthenticatedUser = usernameFetcher.getUserNameForAuthenticatedUser();
        logger.warn("About to delete point of interest by ID {}. User: {}", pointOfInterestId, usernameForAuthenticatedUser);

        List<PointOfInterest> pointsOfInterest = pointOfInterestRepository.findByNetexId(pointOfInterestId);

        if(pointsOfInterest.isEmpty()) {
            throw new IllegalArgumentException("Cannot find point of interest to delete from ID: " + pointOfInterestId);
        }

        for(PointOfInterest pointOfInterest : pointsOfInterest) {
            if(pointOfInterest.getParentSiteRef() != null) {
                DataManagedObjectStructure resolved = referenceResolver.resolve(pointOfInterest.getParentSiteRef());
                if(resolved instanceof StopPlace) {
                    authorizationService.assertAuthorized(ROLE_EDIT_STOPS, Arrays.asList(resolved));
                }
            }
        }

        pointOfInterestRepository.deleteAll(pointsOfInterest);
        notifyDeleted(pointsOfInterest);

        logger.warn("All versions ({}) of point of interest {} deleted by user {}", pointsOfInterest.size(), pointOfInterestId, usernameForAuthenticatedUser);

        return true;
    }

    private void notifyDeleted(List<PointOfInterest> pointsOfInterest) {
        entityChangedListener.onDelete(Collections.max(pointsOfInterest, Comparator.comparing(c -> c.getVersion())));
    }
}
