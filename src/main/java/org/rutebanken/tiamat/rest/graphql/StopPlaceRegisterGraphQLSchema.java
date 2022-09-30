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

package org.rutebanken.tiamat.rest.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.rest.graphql.fetchers.AuthorizationCheckDataFetcher;
import org.rutebanken.tiamat.rest.graphql.fetchers.TagFetcher;
import org.rutebanken.tiamat.rest.graphql.operations.*;
import org.rutebanken.tiamat.rest.graphql.resolvers.MutableTypeResolver;
import org.rutebanken.tiamat.rest.graphql.scalars.DateScalar;
import org.rutebanken.tiamat.rest.graphql.scalars.TransportModeScalar;
import org.rutebanken.tiamat.rest.graphql.types.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static graphql.Scalars.GraphQLBigDecimal;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;
import static org.rutebanken.tiamat.rest.graphql.types.AuthorizationCheckCreator.createAuthorizationCheckArguments;
import static org.rutebanken.tiamat.rest.graphql.types.AuthorizationCheckCreator.createAuthorizationCheckOutputType;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.*;

@Component
public class StopPlaceRegisterGraphQLSchema {

    public final static int DEFAULT_PAGE_VALUE = 0;
    public final static int DEFAULT_SIZE_VALUE = 20;

    public GraphQLSchema stopPlaceRegisterSchema;

    @Autowired
    private PathLinkEndObjectTypeCreator pathLinkEndObjectTypeCreator;

    @Autowired
    private PathLinkObjectTypeCreator pathLinkObjectTypeCreator;

    @Autowired
    private EntityRefObjectTypeCreator entityRefObjectTypeCreator;

    @Autowired
    private TopographicPlaceObjectTypeCreator topographicPlaceObjectTypeCreator;

    @Autowired
    private StopPlaceOperationsBuilder stopPlaceOperationsBuilder;

    @Autowired
    private ParkingOperationsBuilder parkingOperationsBuilder;

    @Autowired
    private PointOfInterestOperationsBuilder pointOfInterestOperationsBuilder;

    @Autowired
    private ZoneCommonFieldListCreator zoneCommonFieldListCreator;

    @Autowired
    private StopPlaceObjectTypeCreator stopPlaceObjectTypeCreator;

    @Autowired
    private ParkingObjectTypeCreator parkingObjectTypeCreator;

    @Autowired
    private PointOfInterestObjectTypeCreator pointOfInterestObjectTypeCreator;

    @Autowired
    private GroupOfStopPlacesObjectTypeCreator groupOfStopPlaceObjectTypeCreator;

    @Autowired
    private ParentStopPlaceObjectTypeCreator parentStopPlaceObjectTypeCreator;

    @Autowired
    private StopPlaceInterfaceCreator stopPlaceInterfaceCreator;

    @Autowired
    private ParkingInterfaceCreator parkingInterfaceCreator;

    @Autowired
    private PointOfInterestInterfaceCreator pointOfInterestInterfaceCreator;

    @Autowired
    private ParentStopPlaceInputObjectTypeCreator parentStopPlaceInputObjectTypeCreator;

    @Autowired
    private TagOperationsBuilder tagOperationsBuilder;

    @Autowired
    private TagObjectTypeCreator tagObjectTypeCreator;

    @Autowired
    private TagFetcher tagFetcher;

    @Autowired
    private TariffZoneObjectTypeCreator tariffZoneObjectTypeCreator;

    @Autowired
    private AuthorizationCheckDataFetcher authorizationCheckDataFetcher;

    @Autowired
    private MultiModalityOperationsBuilder multiModalityOperationsBuilder;

    @Autowired
    DataFetcher stopPlaceFetcher;

    @Autowired
    private DataFetcher<Page<GroupOfStopPlaces>> groupOfStopPlacesFetcher;

    @Autowired
    private DataFetcher<GroupOfStopPlaces> groupOfStopPlacesUpdater;

    @Autowired
    private DataFetcher<Boolean> groupOfStopPlacesDeleterFetcher;

    @Autowired
    private DataFetcher<Page<TariffZone>> tariffZonesFetcher;

    @Autowired
    DataFetcher pathLinkFetcher;

    @Autowired
    DataFetcher pathLinkUpdater;

    @Autowired
    DataFetcher topographicPlaceFetcher;

    @Autowired
    DataFetcher stopPlaceUpdater;

    @Autowired
    DataFetcher parkingFetcher;

    @Autowired
    DataFetcher parkingUpdater;

    @Autowired
    DataFetcher pointOfInterestFetcher;

    @Autowired
    DataFetcher pointOfInterestUpdater;

    @Autowired
    DateScalar dateScalar;

    @Autowired
    TransportModeScalar transportModeScalar;

    @Autowired
    DataFetcher nameRecommendationsFetcher;

    @PostConstruct
    public void init() {

        /**
         * Common field list for quays, stop places, addressable place and parkings.
         */
        List<GraphQLFieldDefinition> commonFieldsList = new ArrayList<>();
        commonFieldsList.add(newFieldDefinition()
                .name(PLACE_EQUIPMENTS)
                .type(equipmentType)
                .dataFetcher(env -> {
                    if (env.getSource() instanceof StopPlace) {
                        return ((StopPlace) env.getSource()).getPlaceEquipments();
                    } else if (env.getSource() instanceof Quay) {
                        return ((Quay) env.getSource()).getPlaceEquipments();
                    } else if (env.getSource() instanceof Parking) {
                        return ((Parking) env.getSource()).getPlaceEquipments();
                    } else if (env.getSource() instanceof PointOfInterest) {
                        return ((PointOfInterest) env.getSource()).getPlaceEquipments();
                    }
                    return null;
                })
                .build());
        commonFieldsList.add(newFieldDefinition()
                .name(ACCESSIBILITY_ASSESSMENT)
                .description("This field is set either on StopPlace (i.e. all Quays are equal), or on every Quay or on Parking")
                .type(accessibilityAssessmentObjectType)
                .build()
        );

        commonFieldsList.add(newFieldDefinition()
                        .name(PUBLIC_CODE)
                        .type(GraphQLString).build());
        commonFieldsList.add(privateCodeFieldDefinition);
        commonFieldsList.add(
                newFieldDefinition()
                        .name(MODIFICATION_ENUMERATION)
                        .type(modificationEnumerationType)
                        .build()
        );

        List<GraphQLFieldDefinition> zoneCommandFieldList = zoneCommonFieldListCreator.create();

        commonFieldsList.addAll(zoneCommandFieldList);

        GraphQLObjectType quayObjectType = createQuayObjectType(commonFieldsList);

        GraphQLObjectType validBetweenObjectType = createValidBetweenObjectType();

        GraphQLObjectType topographicPlaceObjectType = topographicPlaceObjectTypeCreator.create();

        GraphQLObjectType tariffZoneObjectType = tariffZoneObjectTypeCreator.create(zoneCommandFieldList);

        MutableTypeResolver stopPlaceTypeResolver = new MutableTypeResolver();
        MutableTypeResolver parkingTypeResolver = new MutableTypeResolver();
        MutableTypeResolver pointOfInterestTypeResolver = new MutableTypeResolver();

        List<GraphQLFieldDefinition> stopPlaceInterfaceFields = stopPlaceInterfaceCreator.createCommonInterfaceFields(tariffZoneObjectType, topographicPlaceObjectType, validBetweenObjectType);
        List<GraphQLFieldDefinition> parkingInterfaceFields = parkingInterfaceCreator.createCommonInterfaceFields(topographicPlaceObjectType, validBetweenObjectType);
        List<GraphQLFieldDefinition> pointOfInterestInterfaceFields = pointOfInterestInterfaceCreator.createCommonInterfaceFields(topographicPlaceObjectType, validBetweenObjectType);

        GraphQLInterfaceType stopPlaceInterface = stopPlaceInterfaceCreator.createInterface(stopPlaceInterfaceFields, commonFieldsList, stopPlaceTypeResolver);
        GraphQLInterfaceType parkingInterface = parkingInterfaceCreator.createInterface(parkingInterfaceFields, commonFieldsList, parkingTypeResolver);
        GraphQLInterfaceType pointOfInterestInterface = pointOfInterestInterfaceCreator.createInterface(pointOfInterestInterfaceFields, commonFieldsList, pointOfInterestTypeResolver);

        GraphQLObjectType stopPlaceObjectType = stopPlaceObjectTypeCreator.create(stopPlaceInterface, stopPlaceInterfaceFields, commonFieldsList, quayObjectType);
        GraphQLObjectType parentStopPlaceObjectType = parentStopPlaceObjectTypeCreator.create(stopPlaceInterface, stopPlaceInterfaceFields, commonFieldsList, stopPlaceObjectType);

        stopPlaceTypeResolver.setResolveFunction(object -> {
            if (object instanceof StopPlace) {
                StopPlace stopPlace = (StopPlace) object;
                if (stopPlace.isParentStopPlace()) {
                    return parentStopPlaceObjectType;
                } else {
                    return stopPlaceObjectType;
                }
            }
            throw new IllegalArgumentException("StopPlaceTypeResolver cannot resolve type of Object " + object + ". Was expecting StopPlace");
        });

        GraphQLObjectType groupOfStopPlacesObjectType = groupOfStopPlaceObjectTypeCreator.create(stopPlaceInterface);

        GraphQLObjectType addressablePlaceObjectType = createAddressablePlaceObjectType(commonFieldsList);

        GraphQLObjectType entityRefObjectType = entityRefObjectTypeCreator.create(addressablePlaceObjectType);

        GraphQLObjectType pathLinkEndObjectType = pathLinkEndObjectTypeCreator.create(entityRefObjectType, netexIdFieldDefinition);

        GraphQLObjectType pathLinkObjectType = pathLinkObjectTypeCreator.create(pathLinkEndObjectType, netexIdFieldDefinition, geometryFieldDefinition);

        GraphQLObjectType parkingObjectType = parkingObjectTypeCreator.create(parkingInterface, parkingInterfaceFields, commonFieldsList);

        GraphQLObjectType pointOfInterestObjectType = pointOfInterestObjectTypeCreator.create(pointOfInterestInterface, pointOfInterestInterfaceFields, commonFieldsList);

        parkingTypeResolver.setResolveFunction(object -> {
            if (object instanceof Parking) {
                return parkingObjectType;
            }
            throw new IllegalArgumentException("ParkingTypeResolver cannot resolve type of Object " + object + ". Was expecting Parking");
        });

        pointOfInterestTypeResolver.setResolveFunction(o -> {
            if (o instanceof PointOfInterest) {
                return pointOfInterestObjectType;
            }
            throw new IllegalArgumentException("PointOfInterestTypeResolver cannot resolve type of Object " + o + ". Was expecting PointOfInterest");
        });

        GraphQLArgument allVersionsArgument = GraphQLArgument.newArgument()
                .name(ALL_VERSIONS)
                .type(GraphQLBoolean)
                .description(ALL_VERSIONS_ARG_DESCRIPTION)
                .build();

        GraphQLObjectType stopPlaceRegisterQuery = newObject()
                .name("StopPlaceRegister")
                .description("Query and search for data")
                .field(newFieldDefinition()
                        .type(new GraphQLList(stopPlaceInterface))
                        .name(FIND_STOPPLACE)
                        .description("Search for StopPlaces")
                        .argument(createFindStopPlaceArguments(allVersionsArgument))
                        .dataFetcher(stopPlaceFetcher))
                //Search by BoundingBox
                .field(newFieldDefinition()
                        .type(new GraphQLList(stopPlaceInterface))
                        .name(FIND_STOPPLACE_BY_BBOX)
                        .description("Find StopPlaces within given BoundingBox.")
                        .argument(createBboxArguments())
                        .dataFetcher(stopPlaceFetcher))
                .field(newFieldDefinition()
                        .name(FIND_PARKING)
                        .type(new GraphQLList(parkingInterface))
                        .description("Find parking")
                        .argument(createFindParkingArguments(allVersionsArgument))
                        .dataFetcher(parkingFetcher))
                .field(newFieldDefinition()
                        .type(new GraphQLList(parkingInterface))
                        .name(FIND_PARKING_BY_BBOX)
                        .description("Find Parking within given BoundingBox.")
                        .argument(createBboxArguments())
                        .dataFetcher(parkingFetcher))
                .field(newFieldDefinition()
                        .type(new GraphQLList(pointOfInterestInterface))
                        .name(FIND_POI)
                        .description("Find points of interest")
                        .argument(createFindPointOfInterestArguments(allVersionsArgument))
                        .dataFetcher(pointOfInterestFetcher))
                .field(newFieldDefinition()
                        .type(new GraphQLList(pointOfInterestInterface))
                        .name(FIND_POI_BY_BBOX)
                        .description("Find point of interest within given BoundingBox.")
                        .argument(createBboxArguments())
                        .dataFetcher(pointOfInterestFetcher))
                .field(newFieldDefinition()
                        .name(FIND_TOPOGRAPHIC_PLACE)
                        .type(new GraphQLList(topographicPlaceObjectType))
                        .description("Find topographic places")
                        .argument(createFindTopographicPlaceArguments(allVersionsArgument))
                        .dataFetcher(topographicPlaceFetcher))
                .field(newFieldDefinition()
                        .name(FIND_PATH_LINK)
                        .type(new GraphQLList(pathLinkObjectType))
                        .description("Find path links")
                        .argument(createFindPathLinkArguments(allVersionsArgument))
                        .dataFetcher(pathLinkFetcher))
                .field(newFieldDefinition()
                        .name(VALID_TRANSPORT_MODES)
                        .type(new GraphQLList(transportModeSubmodeObjectType))
                        .description("List all valid Transportmode/Submode-combinations.")
                        .staticValue(transportModeScalar.getConfiguredTransportModes().keySet()))
                .field(newFieldDefinition()
                        .name(CHECK_AUTHORIZED)
                        .type(createAuthorizationCheckOutputType())
                        .description(AUTHORIZATION_CHECK_DESCRIPTION)
                        .argument(createAuthorizationCheckArguments())
                        .dataFetcher(authorizationCheckDataFetcher))
                .field(newFieldDefinition()
                        .name(TAGS)
                        .type(new GraphQLList(tagObjectTypeCreator.create()))
                        .description(TAGS_DESCRIPTION)
                        .argument(GraphQLArgument.newArgument()
                                .name(TAG_NAME)
                                .description(TAG_NAME_DESCRIPTION)
                                .type(new GraphQLNonNull(GraphQLString)))
                        .dataFetcher(tagFetcher)
                        .build())
                .field(newFieldDefinition()
                        .name(GROUP_OF_STOP_PLACES)
                        .type(new GraphQLList(groupOfStopPlacesObjectType))
                        .description("Group of stop places")
                        .argument(createFindGroupOfStopPlacesArguments())
                        .dataFetcher(groupOfStopPlacesFetcher)
                        .build())
                .field(newFieldDefinition()
                        .name(TARIFF_ZONES)
                        .type(new GraphQLList(tariffZoneObjectType))
                        .description("Tariff zones")
                        .argument(createFindTariffZonesArguments())
                        .dataFetcher(tariffZonesFetcher)
                        .build())
                .field(newFieldDefinition()
                        .type(GraphQLString)
                        .name(NAME_WITH_RECOMMENDATIONS)
                        .description("Get name with Modalis recommendations.")
                        .argument(GraphQLArgument.newArgument()
                                .name(NAME)
                                .type(GraphQLString)
                                .build())
                        .dataFetcher(nameRecommendationsFetcher))
                .build();


        List<GraphQLInputObjectField> commonInputFieldList = createCommonInputFieldList(embeddableMultiLingualStringInputObjectType);

        GraphQLInputObjectType quayInputObjectType = createQuayInputObjectType(commonInputFieldList);

        GraphQLInputObjectType validBetweenInputObjectType = createValidBetweenInputObjectType();

        GraphQLInputObjectType stopPlaceInputObjectType = createStopPlaceInputObjectType(commonInputFieldList,
                topographicPlaceInputObjectType, quayInputObjectType, validBetweenInputObjectType);

        GraphQLInputObjectType parentStopPlaceInputObjectType = parentStopPlaceInputObjectTypeCreator.create(commonInputFieldList, validBetweenInputObjectType, stopPlaceInputObjectType);

        GraphQLInputObjectType parkingInputObjectType = createParkingInputObjectType(commonInputFieldList, validBetweenInputObjectType);

        GraphQLInputObjectType pointOfInterestInputObjectType = createPointOfInterestInputObjectType(commonInputFieldList, validBetweenInputObjectType);

        GraphQLInputObjectType groupOfStopPlacesInputObjectType = createGroupOfStopPlacesInputObjectType();

        GraphQLObjectType stopPlaceRegisterMutation = newObject()
                .name("StopPlaceMutation")
                .description("Create and edit stopplaces")
                .field(newFieldDefinition()
                        .type(new GraphQLList(stopPlaceObjectType))
                        .name(MUTATE_STOPPLACE)
                        .description("Create new or update existing StopPlace")
                        .argument(GraphQLArgument.newArgument()
                                .name(OUTPUT_TYPE_STOPPLACE)
                                .type(stopPlaceInputObjectType))
                        .dataFetcher(stopPlaceUpdater))
                .field(newFieldDefinition()
                        .type(new GraphQLList(parentStopPlaceObjectType))
                        .name(MUTATE_PARENT_STOPPLACE)
                        .description("Update existing Parent StopPlace")
                        .argument(GraphQLArgument.newArgument()
                                .name(OUTPUT_TYPE_PARENT_STOPPLACE)
                                .type(parentStopPlaceInputObjectType))
                        .dataFetcher(stopPlaceUpdater))
                .field(newFieldDefinition()
                        .name(MUTATE_GROUP_OF_STOP_PLACES)
                        .type(groupOfStopPlacesObjectType)
                        .description("Mutate group of stop places")
                        .argument(GraphQLArgument.newArgument()
                                .name(OUTPUT_TYPE_GROUP_OF_STOPPLACES)
                                .type(groupOfStopPlacesInputObjectType))
                        .dataFetcher(groupOfStopPlacesUpdater))
                .field(newFieldDefinition()
                        .type(new GraphQLList(pathLinkObjectType))
                        .name(MUTATE_PATH_LINK)
                        .description("Create new or update existing PathLink")
                        .argument(GraphQLArgument.newArgument()
                                .name(OUTPUT_TYPE_PATH_LINK)
                                .type(new GraphQLList(pathLinkObjectInputType)))
                        .description("Create new or update existing " + OUTPUT_TYPE_PATH_LINK)
                        .dataFetcher(pathLinkUpdater))
                .field(newFieldDefinition()
                        .type(new GraphQLList(parkingObjectType))
                        .name(MUTATE_PARKING)
                        .argument(GraphQLArgument.newArgument()
                                .name(OUTPUT_TYPE_PARKING)
                                .type(new GraphQLList(parkingInputObjectType)))
                        .description("Create new or update existing " + OUTPUT_TYPE_PARKING)
                        .dataFetcher(parkingUpdater))
                .field(newFieldDefinition()
                        .type(new GraphQLList(pointOfInterestObjectType))
                        .name(MUTATE_POINT_OF_INTEREST)
                        .argument(GraphQLArgument.newArgument()
                                .name(OUTPUT_TYPE_POINT_OF_INTEREST)
                                .type(new GraphQLList(pointOfInterestInputObjectType)))
                        .description("Create new or update existing " + OUTPUT_TYPE_POINT_OF_INTEREST)
                        .dataFetcher(pointOfInterestUpdater))
                .fields(tagOperationsBuilder.getTagOperations())
                .fields(stopPlaceOperationsBuilder.getStopPlaceOperations(stopPlaceInterface))
                .fields(parkingOperationsBuilder.getParkingOperations())
                .fields(pointOfInterestOperationsBuilder.getPointOfInterestOperations())
                .fields(multiModalityOperationsBuilder.getMultiModalityOperations(parentStopPlaceObjectType, validBetweenInputObjectType))
                .field(newFieldDefinition()
                        .type(GraphQLBoolean)
                        .name("deleteGroupOfStopPlaces")
                        .argument(GraphQLArgument.newArgument()
                                .name(ID)
                                .type(new GraphQLNonNull(GraphQLString)))
                        .description("Hard delete group of stop places by ID")
                        .dataFetcher(groupOfStopPlacesDeleterFetcher))
                .build();

        stopPlaceRegisterSchema = GraphQLSchema.newSchema()
                .query(stopPlaceRegisterQuery)
                .mutation(stopPlaceRegisterMutation)
                .build();
    }

    private GraphQLObjectType createAddressablePlaceObjectType(List<GraphQLFieldDefinition> commonFieldsList) {
        return newObject()
                .name(OUTPUT_TYPE_ADDRESSABLE_PLACE)
                .fields(commonFieldsList)
                .build();
    }

    private List<GraphQLArgument> createFindPathLinkArguments(GraphQLArgument allVersionsArgument) {
        List<GraphQLArgument> arguments = new ArrayList<>();
        arguments.add(GraphQLArgument.newArgument()
                .name(ID)
                .type(GraphQLString)
                .build());
        arguments.add(allVersionsArgument);
        arguments.add(GraphQLArgument.newArgument()
                .name(FIND_BY_STOP_PLACE_ID)
                .type(GraphQLString)
                .build());
        return arguments;
    }

    private List<GraphQLArgument> createFindParkingArguments(GraphQLArgument allVersionsArgument) {
        List<GraphQLArgument> arguments = createPageAndSizeArguments();
        arguments.add(GraphQLArgument.newArgument()
                .name(ID)
                .type(GraphQLString)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(QUERY)
                .type(GraphQLString)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(VERSION)
                .type(GraphQLInt)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .type(versionValidityEnumType)
                .name(VERSION_VALIDITY_ARG)
                .description(VERSION_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(FIND_BY_STOP_PLACE_ID)
                .type(GraphQLString)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(PARKING_TYPE)
                .type(new GraphQLList(parkingTypeEnum))
                .description(PARKING_TYPE_ARG_DESCRIPTION)
                .build());
        arguments.add(allVersionsArgument);
        return arguments;
    }

    private List<GraphQLArgument> createFindPointOfInterestArguments(GraphQLArgument allVersionsArgument) {
        List<GraphQLArgument> arguments = createPageAndSizeArguments();
        arguments.add(GraphQLArgument.newArgument()
                .name(ID)
                .type(GraphQLString)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(QUERY)
                .type(GraphQLString)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(VERSION)
                .type(GraphQLInt)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .type(versionValidityEnumType)
                .name(VERSION_VALIDITY_ARG)
                .description(VERSION_ARG_DESCRIPTION)
                .build());
        arguments.add(allVersionsArgument);
        return arguments;
    }

    private List<GraphQLArgument> createFindGroupOfStopPlacesArguments() {
        List<GraphQLArgument> arguments = createPageAndSizeArguments();
        arguments.add(GraphQLArgument.newArgument()
                .name(ID)
                .type(GraphQLString)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(QUERY)
                .type(GraphQLString)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(FIND_BY_STOP_PLACE_ID)
                .type(GraphQLString)
                .build());
        return arguments;
    }

    private List<GraphQLArgument> createFindTariffZonesArguments() {
        List<GraphQLArgument> arguments = createPageAndSizeArguments();
        arguments.add(GraphQLArgument.newArgument()
                .name(QUERY)
                .type(GraphQLString)
                .build());
        return arguments;
    }

    private List<GraphQLArgument> createFindTopographicPlaceArguments(GraphQLArgument allVersionsArgument) {
        List<GraphQLArgument> arguments = new ArrayList<>();
        arguments.add(GraphQLArgument.newArgument()
                .name(ID)
                .type(GraphQLString)
                .build());
        arguments.add(allVersionsArgument);
        arguments.add(GraphQLArgument.newArgument()
                .name(TOPOGRAPHIC_PLACE_TYPE)
                .type(topographicPlaceTypeEnum)
                .description("Limits results to specified placeType.")
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(QUERY)
                .type(GraphQLString)
                .description("Searches for TopographicPlaces by name.")
                .build());
        return arguments;
    }

    private List<GraphQLArgument> createFindStopPlaceArguments(GraphQLArgument allVersionsArgument) {
        List<GraphQLArgument> arguments = createPageAndSizeArguments();
        arguments.add(allVersionsArgument);
        //Search
        arguments.add(GraphQLArgument.newArgument()
                .name(ID)
                .type(GraphQLString)
                .description(ID_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(VERSION)
                .type(GraphQLInt)
                .description(VERSION_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .type(versionValidityEnumType)
                .name(VERSION_VALIDITY_ARG)
                .description(VERSION_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(STOP_PLACE_TYPE)
                .type(new GraphQLList(stopPlaceTypeEnum))
                .description(STOP_PLACE_TYPE_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(COUNTY_REF)
                .type(new GraphQLList(GraphQLString))
                .description(COUNTY_REF_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(COUNTRY_REF)
                .type(new GraphQLList(GraphQLString))
                .description(COUNTRY_REF_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(TAGS)
                .type(new GraphQLList(GraphQLString))
                .description(TAGS_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(MUNICIPALITY_REF)
                .type(new GraphQLList(GraphQLString))
                .description(MUNICIPALITY_REF_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(QUERY)
                .type(GraphQLString)
                .description(QUERY_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(IMPORTED_ID_QUERY)
                .type(GraphQLString)
                .description(IMPORTED_ID_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(POINT_IN_TIME)
                .type(dateScalar.getGraphQLDateScalar())
                .description(POINT_IN_TIME_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(KEY)
                .type(GraphQLString)
                .description(KEY_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(WITHOUT_LOCATION_ONLY)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description(WITHOUT_LOCATION_ONLY_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(NEARBY_STOP_PLACES)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description(NEARBY_STOP_PLACES_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(STOP_PLACES_WITHOUT_QUAY)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description(STOP_PLACES_WITHOUT_QUAY_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(DETECT_MULTI_MODAL_POINTS)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description(DETECT_MULTI_MODAL_POINTS_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(NEARBY_RADIUS)
                .type(GraphQLInt)
                .defaultValue(0)
                .description(NEARBY_RADIUS_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(ORGANISATION_NAME)
                .type(GraphQLString)
                .defaultValue("")
                .description(ORGANISATION_NAME_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(WITH_DISTANT_QUAYS)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description(WITH_DISTANT_QUAYS_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(WITHOUT_QUAYS_ONLY)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description(WITHOUT_QUAYS_ONLY_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(WITH_DUPLICATED_QUAY_IMPORTED_IDS)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description(WITH_DUPLICATED_QUAY_IMPORTED_IDS_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(WITH_NEARBY_SIMILAR_DUPLICATES)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description(WITH_NEARBY_SIMILAR_DUPLICATES_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(ONLY_MONOMODAL_STOPPLACES)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description(ONLY_MONOMODAL_STOPPLACES_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(HAS_PARKING)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description(HAS_PARKING)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(VALUES)
                .type(new GraphQLList(GraphQLString))
                .description(VALUES_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(WITH_TAGS)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description(WITH_TAGS_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(SEARCH_WITH_CODE_SPACE)
                .type(GraphQLString)
                .defaultValue(null)
                .description(SEARCH_WITH_CODE_SPACE_ARG_DESCRIPTION)
                .build());
        return arguments;
    }

    private List<GraphQLArgument> createBboxArguments() {
        List<GraphQLArgument> arguments = createPageAndSizeArguments();
        //BoundingBox
        arguments.add(GraphQLArgument.newArgument()
                .name(LONGITUDE_MIN)
                .description("Bottom left longitude (xMin).")
                .type(new GraphQLNonNull(GraphQLBigDecimal))
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(LATITUDE_MIN)
                .description("Bottom left latitude (yMin).")
                .type(new GraphQLNonNull(GraphQLBigDecimal))
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(LONGITUDE_MAX)
                .description("Top right longitude (xMax).")
                .type(new GraphQLNonNull(GraphQLBigDecimal))
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(LATITUDE_MAX)
                .description("Top right longitude (yMax).")
                .type(new GraphQLNonNull(GraphQLBigDecimal))
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(IGNORE_STOPPLACE_ID)
                .type(GraphQLString)
                .description("ID of StopPlace to excluded from result.")
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(IGNORE_PARKING_ID)
                .type(GraphQLString)
                .description("ID of Parking to excluded from result.")
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(IGNORE_POINT_OF_INTEREST_ID)
                .type(GraphQLString)
                .description("ID of POI to excluded from result.")
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(INCLUDE_EXPIRED)
                .type(GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .description("Set to true if expired StopPlaces should be returned, default is 'false'.")
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(POINT_IN_TIME)
                .type(dateScalar.getGraphQLDateScalar())
                .description(POINT_IN_TIME_ARG_DESCRIPTION)
                .build());
        return arguments;
    }

    private List<GraphQLArgument> createPageAndSizeArguments() {
        List<GraphQLArgument> arguments = new ArrayList<>();
        arguments.add(GraphQLArgument.newArgument()
                .name(PAGE)
                .type(GraphQLInt)
                .defaultValue(DEFAULT_PAGE_VALUE)
                .description(PAGE_ARG_DESCRIPTION)
                .build());
        arguments.add(GraphQLArgument.newArgument()
                .name(SIZE)
                .type(GraphQLInt)
                .defaultValue(DEFAULT_SIZE_VALUE)
                .description(SIZE_ARG_DESCRIPTION)
                .build());
        return arguments;
    }

    private GraphQLObjectType createQuayObjectType(List<GraphQLFieldDefinition> commonFieldsList) {
        return newObject()
                .name(OUTPUT_TYPE_QUAY)
                .fields(commonFieldsList)
                .field(newFieldDefinition()
                        .name(COMPASS_BEARING)
                        .type(GraphQLBigDecimal))
                .field(newFieldDefinition()
                        .name(ALTERNATIVE_NAMES)
                        .type(new GraphQLList(alternativeNameObjectType)))
                .build();
    }

    private GraphQLObjectType createValidBetweenObjectType() {
        return newObject()
                .name(OUTPUT_TYPE_VALID_BETWEEN)
                .field(newFieldDefinition()
                        .name(VALID_BETWEEN_FROM_DATE)
                        .type(dateScalar.getGraphQLDateScalar())
                        .description(DATE_SCALAR_DESCRIPTION))
                .field(newFieldDefinition()
                        .name(VALID_BETWEEN_TO_DATE)
                        .type(dateScalar.getGraphQLDateScalar())
                        .description(DATE_SCALAR_DESCRIPTION))
                .build();
    }

    private GraphQLInputObjectType createValidBetweenInputObjectType() {
        return newInputObject()
                .name(INPUT_TYPE_VALID_BETWEEN)
                .field(newInputObjectField()
                        .name(VALID_BETWEEN_FROM_DATE)
                        .type(new GraphQLNonNull(dateScalar.getGraphQLDateScalar()))
                        .description("When the new version is valid from"))
                .field(newInputObjectField()
                        .name(VALID_BETWEEN_TO_DATE)
                        .type(dateScalar.getGraphQLDateScalar())
                        .description("When the version is no longer valid"))
                .build();
    }


    private GraphQLInputObjectType createStopPlaceInputObjectType(List<GraphQLInputObjectField> commonInputFieldsList,
                                                                  GraphQLInputObjectType topographicPlaceInputObjectType,
                                                                  GraphQLInputObjectType quayObjectInputType,
                                                                  GraphQLInputObjectType validBetweenInputObjectType) {
        return newInputObject()
                .name(INPUT_TYPE_STOPPLACE)
                .fields(commonInputFieldsList)
                .fields(transportModeScalar.createTransportModeInputFieldsList())
                .field(newInputObjectField()
                        .name(STOP_PLACE_TYPE)
                        .type(stopPlaceTypeEnum))
                .field(newInputObjectField()
                        .name(TOPOGRAPHIC_PLACE)
                        .type(topographicPlaceInputObjectType))
                .field(newInputObjectField()
                        .name(WEIGHTING)
                        .type(interchangeWeightingEnum))
                .field(newInputObjectField()
                        .name(PARENT_SITE_REF)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(VERSION_COMMENT)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(QUAYS)
                        .type(new GraphQLList(quayObjectInputType)))
                .field(newInputObjectField()
                        .name(VALID_BETWEEN)
                        .type(validBetweenInputObjectType))
                .field(newInputObjectField()
                        .name(TARIFF_ZONES)
                        .description("List of tariff zone references without version")
                        .type(new GraphQLList(versionLessRefInputObjectType)))
                .field(newInputObjectField()
                        .name(ADJACENT_SITES)
                        .description(ADJACENT_SITES_DESCRIPTION)
                        .type(new GraphQLList(versionLessRefInputObjectType)))
                .build();
    }

    private GraphQLInputObjectType createGroupOfStopPlacesInputObjectType() {
        return newInputObject()
                .name(INPUT_TYPE_GROUP_OF_STOPPLACES)
                .field(newInputObjectField().name(ID).type(GraphQLString).description("Ignore ID when creating new"))
                .field(newInputObjectField().name(NAME).type(new GraphQLNonNull(embeddableMultiLingualStringInputObjectType)))
                .field(newInputObjectField().name(SHORT_NAME).type(embeddableMultiLingualStringInputObjectType))
                .field(newInputObjectField().name(DESCRIPTION).type(embeddableMultiLingualStringInputObjectType))
                .field(newInputObjectField().name(ALTERNATIVE_NAMES).type(new GraphQLList(alternativeNameInputObjectType)))
                .field(newInputObjectField().name(VERSION_COMMENT).type(GraphQLString))
                .field(newInputObjectField()
                        .name(GROUP_OF_STOP_PLACES_MEMBERS)
                        .description("References to group of stop places members. Stop place IDs.")
                        .type(new GraphQLList(versionLessRefInputObjectType)))
                .build();
    }

    private List<GraphQLInputObjectField> createCommonInputFieldList(GraphQLInputObjectType embeddableMultiLingualStringInputObjectType) {

        List<GraphQLInputObjectField> commonInputFieldsList = new ArrayList<>();
        commonInputFieldsList.add(newInputObjectField().name(ID).type(GraphQLString).description("Ignore when creating new").build());
        commonInputFieldsList.add(newInputObjectField().name(NAME).type(embeddableMultiLingualStringInputObjectType).build());
        commonInputFieldsList.add(newInputObjectField().name(SHORT_NAME).type(embeddableMultiLingualStringInputObjectType).build());
        commonInputFieldsList.add(newInputObjectField().name(PUBLIC_CODE).type(GraphQLString).build());
        commonInputFieldsList.add(newInputObjectField().name(PRIVATE_CODE).type(privateCodeInputType).build());
        commonInputFieldsList.add(newInputObjectField().name(DESCRIPTION).type(embeddableMultiLingualStringInputObjectType).build());
        commonInputFieldsList.add(newInputObjectField().name(GEOMETRY).type(geoJsonInputType).build());
        commonInputFieldsList.add(newInputObjectField().name(ALTERNATIVE_NAMES).type(new GraphQLList(alternativeNameInputObjectType)).build());
        commonInputFieldsList.add(newInputObjectField().name(PLACE_EQUIPMENTS).type(equipmentInputType).build());
        commonInputFieldsList.add(newInputObjectField().name(KEY_VALUES).type(new GraphQLList(keyValuesObjectInputType)).build());
        commonInputFieldsList.add(
                newInputObjectField()
                        .name(ACCESSIBILITY_ASSESSMENT)
                        .description("This field is set either on StopPlace (i.e. all Quays are equal), or on every Quay or on Parking")
                        .type(accessibilityAssessmentInputObjectType)
                        .build()
        );
        return commonInputFieldsList;
    }


    private GraphQLInputObjectType createQuayInputObjectType(List<GraphQLInputObjectField> graphQLCommonInputObjectFieldsList) {
        return newInputObject()
                .name(INPUT_TYPE_QUAY)
                .fields(graphQLCommonInputObjectFieldsList)
                .field(newInputObjectField()
                        .name(COMPASS_BEARING)
                        .type(GraphQLBigDecimal))
                .field(newInputObjectField()
                        .name(PUBLIC_CODE)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(PRIVATE_CODE)
                        .type(privateCodeInputType))
                .build();
    }

}

