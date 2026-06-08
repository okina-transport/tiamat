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

package org.rutebanken.tiamat.service.stopplace;

import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.helper.organisation.ReflectionAuthorizationService;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.changelog.EntityChangedListener;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.lock.MutateLock;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_DELETE_STOPS;

@Service
public class StopPlaceDeleter {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceDeleter.class);

    private final StopPlaceRepository stopPlaceRepository;

    private final EntityChangedListener entityChangedListener;

    private final ReflectionAuthorizationService authorizationService;

    private final UsernameFetcher usernameFetcher;

    private final MutateLock mutateLock;

    private final StopPlaceQuayDeleterToChouette stopPlaceQuayDeleterToChouette;

    private final MdmService mdmService;

    @Autowired
    public StopPlaceDeleter(StopPlaceRepository stopPlaceRepository, EntityChangedListener entityChangedListener, ReflectionAuthorizationService authorizationService, UsernameFetcher usernameFetcher, MutateLock mutateLock, StopPlaceQuayDeleterToChouette stopPlaceQuayDeleterToChouette, MdmService mdmService) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.entityChangedListener = entityChangedListener;
        this.authorizationService = authorizationService;
        this.usernameFetcher = usernameFetcher;
        this.mutateLock = mutateLock;
        this.stopPlaceQuayDeleterToChouette = stopPlaceQuayDeleterToChouette;
        this.mdmService = mdmService;
    }

    public boolean deleteStopPlace(String stopPlaceNetexId) {

        return mutateLock.executeInLock(() -> {
            String usernameForAuthenticatedUser = usernameFetcher.getUserNameForAuthenticatedUser();
            logger.warn("About to delete stop place by ID {}. User: {}", stopPlaceNetexId, usernameForAuthenticatedUser);

            List<StopPlace> stopPlaces = getAllVersionsOfStopPlace(stopPlaceNetexId);

            stopPlaceQuayDeleterToChouette.delete(stopPlaceNetexId);

            StopPlace lastVersionStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(stopPlaceNetexId);
            if (lastVersionStopPlace != null && (lastVersionStopPlace.isParentStopPlace() || lastVersionStopPlace.getParentSiteRef() != null)) {
                throw new IllegalArgumentException("Deleting parent stop place or childs of parent stop place is not allowed: " + stopPlaceNetexId);
            }

            authorizationService.assertAuthorized(ROLE_DELETE_STOPS, stopPlaces);

            deleteIdsInMdm(lastVersionStopPlace);

            stopPlaceRepository.deleteStopPlaceChildrenByChildren(stopPlaces);
            stopPlaceRepository.deleteAll(stopPlaces);


//            notifyDeleted(stopPlaces);

            logger.warn("All versions ({}) of stop place {} deleted by user {}", stopPlaces.size(), stopPlaceNetexId, usernameForAuthenticatedUser);

            return true;
        });
    }

    private void deleteIdsInMdm(StopPlace stopPlaceToDelete) {
        if (stopPlaceToDelete == null){
            return;
        }

        mdmService.deleteStopPlaceBySuperId(stopPlaceToDelete.getNetexId());
        if (CollectionUtils.isNotEmpty(stopPlaceToDelete.getQuays())){
            stopPlaceToDelete.getQuays().forEach(quay ->  mdmService.deleteQuaysBySuperId(quay.getNetexId()));
        }
    }

    private List<StopPlace> getAllVersionsOfStopPlace(String stopPlaceId) {
        List<String> idList = new ArrayList<>();
        idList.add(stopPlaceId);

        List<StopPlace> stopPlaces = stopPlaceRepository.findAll(idList);

        Preconditions.checkArgument((stopPlaces != null && !stopPlaces.isEmpty()), "Attempting to fetch StopPlace [id = %s], but StopPlace does not exist.", stopPlaceId);

        return stopPlaces;
    }

    private void notifyDeleted(List<StopPlace> stopPlaces) {
        Collections.sort(stopPlaces,
                (o1, o2) -> Long.compare(o1.getVersion(), o2.getVersion()));
        StopPlace newest = stopPlaces.get(stopPlaces.size() - 1);
        entityChangedListener.onDelete(newest);
    }
}
