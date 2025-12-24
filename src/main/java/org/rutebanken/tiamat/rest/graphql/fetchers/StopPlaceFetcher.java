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

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.rutebanken.tiamat.auth.StopPlaceAuthorizationService;
import org.rutebanken.tiamat.dtoassembling.dto.BoundingBoxDto;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.model.tag.Tag;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.repository.TagRepository;
import org.rutebanken.tiamat.repository.TopographicPlaceRepository;
import org.rutebanken.tiamat.service.stopplace.ParentStopPlacesFetcher;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.exporter.params.ExportParams.newExportParamsBuilder;
import static org.rutebanken.tiamat.exporter.params.StopPlaceSearch.newStopPlaceSearchBuilder;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

@Service("stopPlaceFetcher")
@Transactional
class StopPlaceFetcher implements DataFetcher {


    private static final Logger logger = LoggerFactory.getLogger(StopPlaceFetcher.class);

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private VersionCreator versionCreator;

    /**
     * Whether to keep children when resolving parent stop places. False, because with graphql it's possible to fetch children from parent.
     */
    private static final boolean KEEP_CHILDREN = false;

    /**
     * User with role starting with this prefix will be considered as admin => do not filter stop places based on org.
     * TODO : probably already implemented somewhere else, have to find.
     */
    protected static final String ROLE_ADMIN_PREFIX = "admin";

    private final StopPlaceRepository stopPlaceRepository;

    private final ParentStopPlacesFetcher parentStopPlacesFetcher;

    private final RoleAssignmentExtractor roleAssignmentExtractor;

    private final TagRepository tagRepository;

    private final TopographicPlaceRepository topographicPlaceRepository;

    private final StopPlaceAuthorizationService stopPlaceAuthorizationService;

    private final MdmService mdmService;

    StopPlaceFetcher(StopPlaceRepository stopPlaceRepository,
                     ParentStopPlacesFetcher parentStopPlacesFetcher,
                     RoleAssignmentExtractor roleAssignmentExtractor,
                     TagRepository tagRepository,
                     TopographicPlaceRepository topographicPlaceRepository,
                     StopPlaceAuthorizationService stopPlaceAuthorizationService,
                     MdmService mdmService) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.parentStopPlacesFetcher = parentStopPlacesFetcher;
        this.roleAssignmentExtractor = roleAssignmentExtractor;
        this.tagRepository = tagRepository;
        this.topographicPlaceRepository = topographicPlaceRepository;
        this.stopPlaceAuthorizationService = stopPlaceAuthorizationService;
        this.mdmService = mdmService;
    }

    public Object get(DataFetchingEnvironment environment) {
        List<StopPlace> stopPlaces = getDataFromDB(environment);


        // we need to copy stop place to new objects(unhandled by hibernate) to avoid auto-persist of imported-ids
        List<StopPlace> copiedStopPlaces = getStopPlaceCopyWithMdmId(stopPlaces);


        boolean onlyMonomodalStopplaces = false;
        if (environment.getArgument(ONLY_MONOMODAL_STOPPLACES) != null) {
            onlyMonomodalStopplaces = environment.getArgument(ONLY_MONOMODAL_STOPPLACES);
        }

        boolean nearbyStopPlaceSearch = false;
        if (environment.getArgument(NEARBY_STOP_PLACES) != null) {
            nearbyStopPlaceSearch = environment.getArgument(NEARBY_STOP_PLACES);
        }

        boolean stopPlacesWithoutQuaySearch = false;
        if (environment.getArgument(STOP_PLACES_WITHOUT_QUAY) != null) {
            stopPlacesWithoutQuaySearch = environment.getArgument(STOP_PLACES_WITHOUT_QUAY);
        }

        boolean stopPlacesWithMultipleProducersSearch = false;
        if (environment.getArgument(STOP_PLACES_WITH_MULTIPLE_PRODUCERS) != null) {
            stopPlacesWithMultipleProducersSearch = environment.getArgument(STOP_PLACES_WITH_MULTIPLE_PRODUCERS);
        }

        boolean quaysWithMultipleProducersSearch = false;
        if (environment.getArgument(QUAYS_WITH_MULTIPLE_PRODUCERS) != null) {
            quaysWithMultipleProducersSearch = environment.getArgument(QUAYS_WITH_MULTIPLE_PRODUCERS);
        }


        //By default stop should resolve parent stops
        if (nearbyStopPlaceSearch || onlyMonomodalStopplaces || stopPlacesWithoutQuaySearch || stopPlacesWithMultipleProducersSearch || quaysWithMultipleProducersSearch) {
            return getStopPlaces(environment, copiedStopPlaces, stopPlaces.size());
        } else {
            List<StopPlace> parentsResolved = parentStopPlacesFetcher.resolveParents(copiedStopPlaces, KEEP_CHILDREN);
            return getStopPlaces(environment, getStopPlaceCopyWithMdmId(parentsResolved), parentsResolved.size());
        }
    }

    private List<StopPlace> getStopPlaceCopyWithMdmId(List<StopPlace> stopPlaces) {
        if (mdmService.isMdmEnabled()) {
            List<StopPlace> copiedStopPlaces = new ArrayList<>();
            for (StopPlace stopPlace : stopPlaces) {
                StopPlace copy = versionCreator.createCopy(stopPlace, StopPlace.class);
                copiedStopPlaces.add(copy);
            }
            mdmService.fillImportedIds(copiedStopPlaces);
            return copiedStopPlaces;
        } else {
            return stopPlaces;
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<StopPlace> getDataFromDB(DataFetchingEnvironment environment) {
        ExportParams.Builder exportParamsBuilder = newExportParamsBuilder();
        StopPlaceSearch.Builder stopPlaceSearchBuilder = newStopPlaceSearchBuilder();
        List<String> userOrgs = roleAssignmentExtractor.getRoleAssignmentsForUser().stream().map(RoleAssignment::getOrganisation).collect(Collectors.toList());

        Boolean ignoreStops = environment.getArgument(IGNORE_STOPS);
        if (ignoreStops != null && ignoreStops) { return new ArrayList<>(); }

        logger.info("Searching for StopPlaces with arguments {}", environment.getArguments());
        logger.info("User organisations : {}", userOrgs);

        Page<StopPlace> stopPlacesPage = new PageImpl<>(new ArrayList<>());

        exportParamsBuilder.setProviderList(stopPlaceAuthorizationService.getFilteredProviders());

        stopPlaceSearchBuilder.setPage(environment.getArgument(PAGE)).setSize(environment.getArgument(SIZE));

        String netexId = environment.getArgument(ID);
        String importedId = environment.getArgument(IMPORTED_ID_QUERY);
        Integer version = environment.getArgument(VERSION);

        String key = environment.getArgument(KEY);
        List<String> values = environment.getArgument(VALUES);

        Boolean allVersions = setIfNonNull(environment, ALL_VERSIONS, stopPlaceSearchBuilder::setAllVersions);
        setIfNonNull(environment, WITHOUT_LOCATION_ONLY, stopPlaceSearchBuilder::setWithoutLocationOnly);
        setIfNonNull(environment, WITHOUT_QUAYS_ONLY, stopPlaceSearchBuilder::setWithoutQuaysOnly);
        setIfNonNull(environment, WITH_DUPLICATED_QUAY_IMPORTED_IDS, stopPlaceSearchBuilder::setWithDuplicatedQuayImportedIds);
        setIfNonNull(environment, WITH_NEARBY_SIMILAR_DUPLICATES, stopPlaceSearchBuilder::setWithNearbySimilarDuplicates);
        setIfNonNull(environment, STOP_PLACES_WITHOUT_QUAY, stopPlaceSearchBuilder::setStopPlacesWithoutQuay);
        setIfNonNull(environment, NEARBY_STOP_PLACES, stopPlaceSearchBuilder::setNearbyStopPlaces);
        setIfNonNull(environment, NEARBY_RADIUS, stopPlaceSearchBuilder::setNearbyRadius);
        setIfNonNull(environment, ORGANISATION_NAME, stopPlaceSearchBuilder::setOrganisationName);
        setIfNonNull(environment, WITH_DISTANT_QUAYS, stopPlaceSearchBuilder::setWithDistantQuays);
        setIfNonNull(environment, DETECT_MULTI_MODAL_POINTS, stopPlaceSearchBuilder::setDetectMultiModalPoints);
        setIfNonNull(environment, HAS_PARKING, stopPlaceSearchBuilder::setHasParking);
        setIfNonNull(environment, WITH_TAGS, stopPlaceSearchBuilder::setWithTags);
        setIfNonNull(environment, STOP_PLACES_WITH_MULTIPLE_PRODUCERS, stopPlaceSearchBuilder::setStopPlacesWithMultipleProducers);
        setIfNonNull(environment, QUAYS_WITH_MULTIPLE_PRODUCERS, stopPlaceSearchBuilder::setQuaysWithMultipleProducers);


        Instant pointInTime;
        if (environment.getArgument(POINT_IN_TIME) != null) {
            pointInTime = environment.getArgument(POINT_IN_TIME);
        } else {
            pointInTime = null;
        }

        if (environment.getArgument(VERSION_VALIDITY_ARG) != null) {
            ExportParams.VersionValidity versionValidity = ExportParams.VersionValidity.valueOf(ExportParams.VersionValidity.class, environment.getArgument(VERSION_VALIDITY_ARG));
            stopPlaceSearchBuilder.setVersionValidity(versionValidity);
        }

        if (netexId != null && !netexId.isEmpty()) {

            try {
                List<StopPlace> stopPlace;
                if (version != null && version > 0) {
                    stopPlace = Arrays.asList(stopPlaceRepository.findFirstByNetexIdAndVersion(netexId, version));
                    stopPlacesPage = getStopPlaces(environment, getStopPlaceCopyWithMdmId(stopPlace), 1L);
                } else {
                    stopPlaceSearchBuilder.setNetexIdList(Arrays.asList(netexId));
                    stopPlacesPage = stopPlaceRepository.findStopPlace(exportParamsBuilder.setStopPlaceSearch(stopPlaceSearchBuilder.build()).build());
                }

            } catch (NumberFormatException nfe) {
                logger.info("Attempted to find stopPlace with invalid id [{}]", netexId);
            }
        } else if (importedId != null && !importedId.isEmpty()) {

            List<String> stopPlaceNetexId = stopPlaceRepository.searchByKeyValue(NetexIdMapper.ORIGINAL_ID_KEY, environment.getArgument(IMPORTED_ID_QUERY));

            if (stopPlaceNetexId != null && !stopPlaceNetexId.isEmpty()) {
                stopPlaceSearchBuilder.setNetexIdList(stopPlaceNetexId);
                stopPlacesPage = stopPlaceRepository.findStopPlace(exportParamsBuilder.setStopPlaceSearch(stopPlaceSearchBuilder.build()).build());
            }
        } else {

            if (key != null && values != null) {
                Set<String> valueSet = new HashSet<>(values);

                Set<String> stopPlaceNetexId = stopPlaceRepository.findByKeyValues(key, valueSet, true);
                if (stopPlaceNetexId != null && !stopPlaceNetexId.isEmpty()) {
                    List<String> idList = new ArrayList<>(stopPlaceNetexId);
                    stopPlaceSearchBuilder.setNetexIdList(idList);
                } else {
                    //Search for key/values returned no results
                    return new ArrayList<>();
                }
            } else {

                if (allVersions == null || !allVersions) {
                    //If requesting all versions - POINT_IN_TIME is irrelevant
                    stopPlaceSearchBuilder.setPointInTime(pointInTime);
                }

                List<StopTypeEnumeration> stopTypes = environment.getArgument(STOP_PLACE_TYPE);
                if (stopTypes != null && !stopTypes.isEmpty()) {
                    stopPlaceSearchBuilder.setStopTypeEnumerations(stopTypes.stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
                    );
                }

                List<String> countryRef = environment.getArgument(COUNTRY_REF);
                if (countryRef != null && !countryRef.isEmpty()) {
                    exportParamsBuilder.setCountryReferences(
                            countryRef.stream()
                                    .filter(countryRefValue -> countryRefValue != null && !countryRefValue.isEmpty())
                                    .collect(Collectors.toList())
                    );
                }

                List<String> countyRef = environment.getArgument(COUNTY_REF);
                if (countyRef != null && !countyRef.isEmpty()) {
                    exportParamsBuilder.setCountyReferences(
                            countyRef.stream()
                                    .filter(countyRefValue -> countyRefValue != null && !countyRefValue.isEmpty())
                                    .collect(Collectors.toList())
                    );
                }

                List<String> municipalityRef = environment.getArgument(MUNICIPALITY_REF);
                if (municipalityRef != null && !municipalityRef.isEmpty()) {
                    exportParamsBuilder.setMunicipalityReferences(
                            municipalityRef.stream()
                                    .filter(municipalityRefValue -> municipalityRefValue != null && !municipalityRefValue.isEmpty())
                                    .collect(Collectors.toList())
                    );
                }

                if (environment.getArgument(SEARCH_WITH_CODE_SPACE) != null) {
                    String code = environment.getArgument(SEARCH_WITH_CODE_SPACE);
                    exportParamsBuilder.setCodeSpace(code.toLowerCase());
                }

                setIfNonNull(environment, TAGS, stopPlaceSearchBuilder::setTags);

                stopPlaceSearchBuilder.setQuery(environment.getArgument(QUERY));
            }

            if (environment.getArgument(LONGITUDE_MIN) != null) {
                BoundingBoxDto boundingBox = new BoundingBoxDto();

                try {
                    boundingBox.xMin = ((BigDecimal) environment.getArgument(LONGITUDE_MIN)).doubleValue();
                    boundingBox.yMin = ((BigDecimal) environment.getArgument(LATITUDE_MIN)).doubleValue();
                    boundingBox.xMax = ((BigDecimal) environment.getArgument(LONGITUDE_MAX)).doubleValue();
                    boundingBox.yMax = ((BigDecimal) environment.getArgument(LATITUDE_MAX)).doubleValue();
                } catch (NullPointerException npe) {
                    RuntimeException rte = new RuntimeException(MessageFormat.format("{}, {}, {} and {} must all be set when searching within bounding box", LONGITUDE_MIN, LATITUDE_MIN, LONGITUDE_MAX, LATITUDE_MAX));
                    rte.setStackTrace(new StackTraceElement[0]);
                    throw rte;
                }

                String ignoreStopPlaceId = null;
                if (environment.getArgument(IGNORE_STOPPLACE_ID) != null) {
                    ignoreStopPlaceId = environment.getArgument(IGNORE_STOPPLACE_ID);
                }

                if (environment.getArgument(INCLUDE_EXPIRED)) {
                    pointInTime = null;
                }
                stopPlacesPage = stopPlaceRepository.findStopPlacesWithin(boundingBox.xMin, boundingBox.yMin, boundingBox.xMax,
                        boundingBox.yMax, ignoreStopPlaceId, pointInTime, PageRequest.of(environment.getArgument(PAGE), environment.getArgument(SIZE)));
            } else {
                stopPlacesPage = stopPlaceRepository.findStopPlace(exportParamsBuilder.setStopPlaceSearch(stopPlaceSearchBuilder.build()).build());
            }
        }
        List<StopPlace> results = stopPlacesPage.getContent();
        results.forEach(stopPlaceRepository::initializeStopPlace);
        return getStopPlaceCopyWithMdmId(results);
    }


    protected void fetchTags(List<StopPlace> stopPlaceCollection) {
        if (!CollectionUtils.isEmpty(stopPlaceCollection)) {
            int size = stopPlaceCollection.size();
            Set<Tag> tags = new HashSet<>();
            Set<String> stopPlaceIdentifiers = new HashSet<>(size);
            Map<String, Set<Tag>> mapStopPlaceIdentifierToTags = new HashMap<>(size);
            Map<String, StopPlace> mapIdStopPlace = new HashMap<>(size);
            int numberOfElementForSqlStatement = 0;
            for (StopPlace stopPlace : stopPlaceCollection) {
                String stopPlaceIdentifier = stopPlace.getNetexId();
                stopPlaceIdentifiers.add(stopPlaceIdentifier);
                mapIdStopPlace.put(stopPlaceIdentifier, stopPlace);
                numberOfElementForSqlStatement++;
                if (numberOfElementForSqlStatement == 999) {
                    tags.addAll(tagRepository.findAllByIdReference(stopPlaceIdentifiers));
                    stopPlaceIdentifiers.clear();
                    numberOfElementForSqlStatement = 0;
                }
            }
            if (numberOfElementForSqlStatement > 0) {
                tags.addAll(tagRepository.findAllByIdReference(stopPlaceIdentifiers));
                stopPlaceIdentifiers.clear();
            }
            for (Tag tag : tags) {
                Set<Tag> existingTagsForStopPlace = mapStopPlaceIdentifierToTags.getOrDefault(tag.getIdReference(), new HashSet<>());
                existingTagsForStopPlace.add(tag);
                mapStopPlaceIdentifierToTags.put(tag.getIdReference(), existingTagsForStopPlace);
            }
            for (Map.Entry<String, Set<Tag>> entry : mapStopPlaceIdentifierToTags.entrySet()) {
                mapIdStopPlace.get(entry.getKey()).setTags(entry.getValue());
            }

        }
    }


    protected void fetchParentTopographicPlaces(List<StopPlace> stopPlaceCollection) {
        if (!CollectionUtils.isEmpty(stopPlaceCollection)) {
            int size = stopPlaceCollection.size();
            Set<TopographicPlace> fetchedParentTopographicPlace = new HashSet<>();
            Set<Long> topographicPlaceIdentifierSet = new HashSet<>(size);
            Map<Long, StopPlace> mapTopographicIdToStopPlace = new HashMap<>(size);
            int numberOfElementForSqlStatement = 0;
            for (StopPlace stopPlace : stopPlaceCollection) {
                TopographicPlace topographicPlace = stopPlace.getTopographicPlace();
                if (topographicPlace != null && topographicPlace.getParentTopographicPlaceRef() != null) {
                    Long topographicPlaceIdentifier = topographicPlace.getId();
                    topographicPlaceIdentifierSet.add(topographicPlaceIdentifier);
                    mapTopographicIdToStopPlace.put(topographicPlaceIdentifier, stopPlace);
                    numberOfElementForSqlStatement++;
                }

                if (numberOfElementForSqlStatement == 999) {
                    fetchedParentTopographicPlace.addAll(topographicPlaceRepository.findAllParentByChildIdIn(topographicPlaceIdentifierSet));
                    topographicPlaceIdentifierSet.clear();
                    numberOfElementForSqlStatement = 0;
                }
            }
            if (numberOfElementForSqlStatement > 0) {
                fetchedParentTopographicPlace.addAll(topographicPlaceRepository.findAllParentByChildIdIn(topographicPlaceIdentifierSet));
                topographicPlaceIdentifierSet.clear();
            }
            Map<String, TopographicPlace> mapParentTopographicPlace = new HashMap<>(fetchedParentTopographicPlace.size());
            for (TopographicPlace topographicPlace : fetchedParentTopographicPlace) {
                mapParentTopographicPlace.put(topographicPlace.getNetexId() + topographicPlace.getVersion(), topographicPlace);
            }
            for (Map.Entry<Long, StopPlace> entry : mapTopographicIdToStopPlace.entrySet()) {
                TopographicPlace topographicPlace = entry.getValue().getTopographicPlace();
                String parentNetexId = topographicPlace.getParentTopographicPlaceRef().getRef();
                String parentVersion = topographicPlace.getParentTopographicPlaceRef().getVersion();
                topographicPlace.setParentTopographicPlace(mapParentTopographicPlace.get(parentNetexId + parentVersion));
            }

        }
    }

    private PageImpl<StopPlace> getStopPlaces(DataFetchingEnvironment environment, List<StopPlace> stopPlaces, long size) {
        fetchTags(stopPlaces);
        fetchParentTopographicPlaces(stopPlaces);
        return new PageImpl<>(stopPlaces, PageRequest.of(environment.getArgument(PAGE), environment.getArgument(SIZE)), size);
    }

    private <T> T setIfNonNull(DataFetchingEnvironment environment, String argumentName, Consumer<T> consumer) {
        if (environment.getArgument(argumentName) != null) {
            T value = environment.getArgument(argumentName);
            consumer.accept(value);
            return value;
        }
        return null;
    }

}
