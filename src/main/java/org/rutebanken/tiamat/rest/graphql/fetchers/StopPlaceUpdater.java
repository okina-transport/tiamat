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

package org.rutebanken.tiamat.rest.graphql.fetchers;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.util.Preconditions;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.rutebanken.tiamat.lock.MutateLock;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.ProviderRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.rest.graphql.helpers.CleanupHelper;
import org.rutebanken.tiamat.rest.graphql.mappers.StopPlaceMapper;
import org.rutebanken.tiamat.service.stopplace.StopPlaceRenamer;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.CHILDREN;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.CREATE_MULTI_MODAL_STOPPLACE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ID;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.MUTATE_PARENT_STOPPLACE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.MUTATE_STOPPLACE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.OUTPUT_TYPE_PARENT_STOPPLACE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.OUTPUT_TYPE_STOPPLACE;

@Service("stopPlaceUpdater")
@Transactional
class StopPlaceUpdater implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceUpdater.class);

    @Autowired
    private StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private StopPlaceMapper stopPlaceMapper;

    @Autowired
    private MutateLock mutateLock;

    @Autowired
    StopPlaceRenamer stopPlaceRenamer;

    @Autowired
    private VersionCreator versionCreator;

    @Autowired
    private RoleAssignmentExtractor roleAssignmentExtractor;

    @Autowired
    private ProviderRepository providerRepository;

    @Override
    public Object get(DataFetchingEnvironment environment) {
        List<Field> fields = environment.getFields();
        CleanupHelper.trimValues(environment.getArguments());
        StopPlace stopPlace = null;
        for (Field field : fields) {
            if (field.getName().equals(MUTATE_STOPPLACE)) {
                stopPlace = createOrUpdateStopPlaceInLock(environment, false);
            } else if (field.getName().equals(MUTATE_PARENT_STOPPLACE)) {
                stopPlace = createOrUpdateStopPlaceInLock(environment, true);
            }
        }
        return Arrays.asList(stopPlace);
    }


    private StopPlace createOrUpdateStopPlaceInLock(DataFetchingEnvironment environment, boolean mutateParent) {
        return mutateLock.executeInLock(() -> createOrUpdateStopPlace(environment, mutateParent));
    }

    private StopPlace createOrUpdateStopPlace(DataFetchingEnvironment environment, boolean mutateParent) {
        StopPlace updatedStopPlace;
        StopPlace existingVersion = null;

        Map input = environment.getArgument(OUTPUT_TYPE_STOPPLACE);
        if (input == null) {
            input = environment.getArgument(OUTPUT_TYPE_PARENT_STOPPLACE);
        }

        if (input != null) {

            String netexId = (String) input.get(ID);
            if (netexId != null) {

                logger.info("About to update StopPlace {}", netexId);

                existingVersion = findAndVerify(netexId);

                if (mutateParent) {
                    Preconditions.checkArgument(existingVersion.isParentStopPlace(),
                            "Attempting to update StopPlace as parent [id = %s], but StopPlace is not a parent", netexId);
                } else {
                    Preconditions.checkArgument(!existingVersion.isParentStopPlace(),
                            "Attempting to update parent StopPlace [id = %s] with incorrect mutation. Use %s", netexId, MUTATE_PARENT_STOPPLACE);

                    Preconditions.checkArgument(existingVersion.getParentSiteRef() == null,
                            "Attempting to update stop place which has parent [id = %s]. Edit the parent instead. (Parent %s)", netexId, existingVersion.getParentSiteRef());
                }

                updatedStopPlace = versionCreator.createCopy(existingVersion, StopPlace.class);

            } else {
                Preconditions.checkArgument(!mutateParent,
                        "Cannot create new parent stop place. Use mutation %s", CREATE_MULTI_MODAL_STOPPLACE);

                logger.info("Creating new StopPlace");
                updatedStopPlace = new StopPlace();
            }

            // Assign provider to stop place.
            // TODO : allow provider selection in UI, for now taking first in line in user credentials.
            roleAssignmentExtractor.getRoleAssignmentsForUser().stream().findFirst().flatMap(roleAssignment -> providerRepository.findByName(roleAssignment.getOrganisation()).stream().findFirst()).ifPresent(updatedStopPlace::setProvider);


            if (updatedStopPlace != null) {
                boolean hasValuesChanged = stopPlaceMapper.populateStopPlaceFromInput(input, updatedStopPlace);

                Set<String> childStopsUpdated;
                if (updatedStopPlace.isParentStopPlace()) {
                    childStopsUpdated = handleChildStops(input, updatedStopPlace);
                    hasValuesChanged |= !childStopsUpdated.isEmpty();
                } else {
                    childStopsUpdated = new HashSet<>();
                }

                if (hasValuesChanged) {
                    if (updatedStopPlace.getName() == null || Strings.isNullOrEmpty(updatedStopPlace.getName().getValue())) {
                        throw new IllegalArgumentException("Updated stop place must have name set: " + updatedStopPlace);
                    }

                    verifyCorrectStopPlaceName(updatedStopPlace.getName().getValue(), stopPlaceRenamer.renameIfNeeded(updatedStopPlace.getName().getValue()));

                    updatedStopPlace = stopPlaceVersionedSaverService.saveNewVersion(existingVersion, updatedStopPlace, childStopsUpdated);

                    return updatedStopPlace;
                }
            }
        }
        return existingVersion;
    }

    private Set<String> handleChildStops(Map input, StopPlace updatedParentStopPlace) {
        Set<String> childStopsUpdated = new HashSet<>();

        if (input.get(CHILDREN) != null) {
            List childObjects = (List) input.get(CHILDREN);
            logger.info("Incoming child stop objects: {}", childObjects);

            for (Object childStopObject : childObjects) {
                Map childStopMap = (Map) childStopObject;
                String childNetexId = (String) childStopMap.get(ID);

                if (updatedParentStopPlace.getChildren().stream().noneMatch(child -> child.getNetexId().equals(childNetexId))) {
                    throw new RuntimeException("Parent " + updatedParentStopPlace.getNetexId() + " does not already contain this child " + childNetexId + ". Cannot continue.");
                }

                logger.info("Finding existing child stop place from parent: {}", updatedParentStopPlace.getNetexId());
                StopPlace existingChildStopPlace = updatedParentStopPlace.getChildren().stream().filter(c -> c.getNetexId().equals(childNetexId)).findFirst().orElse(null);
                verifyStopPlaceNotNull(existingChildStopPlace, childNetexId);

                // Next line is not strictly required. As the child will always belong to the parent.
                verifyCorrectParentSet(existingChildStopPlace, updatedParentStopPlace);

                logger.info("Populating changes for child stop {} (parent: {})", childNetexId, updatedParentStopPlace.getNetexId());
                boolean wasUpdated = stopPlaceMapper.populateStopPlaceFromInput((Map) childStopMap, existingChildStopPlace);;

                if (wasUpdated) {
                    childStopsUpdated.add(existingChildStopPlace.getNetexId());
                }
            }

            logger.info("Applied changes for {} child stops. Parent stop contains {} child stops", childStopsUpdated.size(), updatedParentStopPlace.getChildren().size());
        }
        return childStopsUpdated;
    }

    private StopPlace findAndVerify(String netexId) {
        StopPlace existingStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(netexId);
        verifyStopPlaceNotNull(existingStopPlace, netexId);
        return existingStopPlace;
    }

    private void verifyStopPlaceNotNull(StopPlace existingStopPlace, String netexId) {
        Preconditions.checkArgument(existingStopPlace != null, "Attempting to update StopPlace [id = %s], but StopPlace does not exist.", netexId);
    }

    private void verifyCorrectParentSet(StopPlace existingChildStop, StopPlace existingParentStop) {
        Preconditions.checkArgument(existingChildStop.getParentSiteRef() != null,
                "Attempting to update StopPlace child [id = %s], but it does not belong to any parent.", existingChildStop.getNetexId());

        Preconditions.checkArgument(existingChildStop.getParentSiteRef().getRef().equals(existingParentStop.getNetexId()),
                "Attempting to update StopPlace child [id = %s], but it does not belong to parent %s.", existingChildStop.getNetexId(), existingParentStop.getNetexId());

        Preconditions.checkArgument(existingChildStop.getParentSiteRef().getVersion().equals(String.valueOf(existingParentStop.getVersion())),
                "Attempting to update StopPlace child [id = %s], but it does not refer to parent %s in correct version: %s.",
                existingChildStop.getNetexId(),
                existingParentStop.getNetexId(),
                existingParentStop.getVersion());

    }

    private void verifyCorrectStopPlaceName(String updatedStopPlaceName, String correctName) {
        if(updatedStopPlaceName.equals(correctName)){
            throw new IllegalArgumentException("For respect the Modalis recommendations, the correct stop place name is : " + correctName);
        }
    }

}
