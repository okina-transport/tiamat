package org.rutebanken.tiamat.rest.graphql;

import com.vividsolutions.jts.geom.Point;
import graphql.Scalars;
import graphql.schema.*;
import org.rutebanken.tiamat.dtoassembling.dto.LocationDto;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.TopographicPlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.getNetexId;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

import static org.rutebanken.tiamat.rest.graphql.scalars.Scalars.GraphQLGeoJSONCoordinates;

@Component
public class StopPlaceRegisterGraphQLSchema {

    private final int DEFAULT_PAGE_VALUE = 0;
    private final int DEFAULT_SIZE_VALUE = 20;
    private final String PAGE_DESCRIPTION_TEXT = "Pagenumber when using pagination - default is " + DEFAULT_PAGE_VALUE;
    private final String SIZE_DESCRIPTION_TEXT = "Number of hits per page when using pagination - default is " + DEFAULT_SIZE_VALUE;

    public GraphQLSchema stopPlaceRegisterSchema;

    @Autowired
    private TopographicPlaceRepository topographicPlaceRepository;

    @Autowired
    DataFetcher stopPlaceFetcher;

    @Autowired
    DataFetcher pathLinkFetcher;

    @Autowired
    DataFetcher topographicPlaceFetcher;

    @Autowired
    DataFetcher stopPlaceUpdater;

    private static GraphQLEnumType topographicPlaceTypeEnum = GraphQLEnumType.newEnum()
            .name(TOPOGRAPHIC_PLACE_TYPE_ENUM)
            .value(TopographicPlaceTypeEnumeration.COUNTY.value(), TopographicPlaceTypeEnumeration.COUNTY)
            .value(TopographicPlaceTypeEnumeration.TOWN.value(), TopographicPlaceTypeEnumeration.TOWN)
            .build();

    private static GraphQLEnumType stopPlaceTypeEnum = GraphQLEnumType.newEnum()
            .name(STOP_PLACE_TYPE_ENUM)
            .value("onstreetBus", StopTypeEnumeration.ONSTREET_BUS)
            .value("onstreetTram", StopTypeEnumeration.ONSTREET_TRAM)
            .value("airport", StopTypeEnumeration.AIRPORT)
            .value("railStation", StopTypeEnumeration.RAIL_STATION)
            .value("metroStation", StopTypeEnumeration.METRO_STATION)
            .value("busStation", StopTypeEnumeration.BUS_STATION)
            .value("coachStation", StopTypeEnumeration.COACH_STATION)
            .value("tramStation", StopTypeEnumeration.TRAM_STATION)
            .value("harbourPort", StopTypeEnumeration.HARBOUR_PORT)
            .value("ferryPort", StopTypeEnumeration.FERRY_PORT)
            .value("ferryStop", StopTypeEnumeration.FERRY_STOP)
            .value("liftStation", StopTypeEnumeration.LIFT_STATION)
            .value("vehicleRailInterchange", StopTypeEnumeration.VEHICLE_RAIL_INTERCHANGE)
            .value("other", StopTypeEnumeration.OTHER)
            .build();

    @PostConstruct
    public void init() {

        GraphQLObjectType embeddableMultilingualStringObjectType = newObject()
                .name(OUTPUT_TYPE_EMBEDDABLE_MULTILINGUAL_STRING)
                .field(newFieldDefinition()
                        .name(VALUE)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(LANG)
                        .type(GraphQLString))
                .build();

        GraphQLObjectType topographicParentPlaceObjectType = newObject()
                .name(OUTPUT_TYPE_TOPOGRAPHIC_PLACE)
                .field(newFieldDefinition()
                        .name(ID)
                        .type(GraphQLString)
                        .dataFetcher(env -> {
                            TopographicPlace topographicPlace = (TopographicPlace) env.getSource();
                            if (topographicPlace != null) {
                                return getNetexId(topographicPlace);
                            } else {
                                return null;
                            }
                        }))
                .field(newFieldDefinition()
                        .name(TOPOGRAPHIC_PLACE_TYPE)
                        .type(topographicPlaceTypeEnum))
                .field(newFieldDefinition()
                        .name(NAME)
                        .type(embeddableMultilingualStringObjectType))
                .build();

        GraphQLObjectType topographicPlaceObjectType = newObject()
                .name(OUTPUT_TYPE_TOPOGRAPHIC_PLACE)
                .field(newFieldDefinition()
                        .name(ID)
                        .type(GraphQLString)
                        .dataFetcher(env -> {
                            TopographicPlace tp = (TopographicPlace) env.getSource();
                            if (tp != null) {
                                return getNetexId(tp);
                            } else {
                                return null;
                            }
                        }))
                .field(newFieldDefinition()
                        .name(TOPOGRAPHIC_PLACE_TYPE)
                        .type(topographicPlaceTypeEnum))
                .field(newFieldDefinition()
                        .name(NAME)
                        .type(embeddableMultilingualStringObjectType))
                .field(newFieldDefinition()
                        .name(PARENT_TOPOGRAPHIC_PLACE)
                        .type(topographicParentPlaceObjectType)
                )
                .build();

        GraphQLObjectType locationObjectType = newObject()
                .name(OUTPUT_TYPE_LOCATION)
                .field(newFieldDefinition()
                        .name(LONGITUDE)
                        .type(GraphQLBigDecimal))
                .field(newFieldDefinition()
                        .name(LATITUDE)
                        .type(GraphQLBigDecimal))
                .build();

        GraphQLObjectType geoJsonPointObjectType = newObject()
                .name(OUTPUT_TYPE_GEO_JSON)
                .field(newFieldDefinition()
                        .name("type")
                        .type(GraphQLString)
                        .staticValue("Point")
                )
                .field(newFieldDefinition()
                        .name("coordinates")
                        .type(GraphQLGeoJSONCoordinates))
                .build();

        GraphQLObjectType geoJsonLineStringObjectType = newObject()
                .name(OUTPUT_TYPE_GEO_JSON)
                .field(newFieldDefinition()
                        .name("type")
                        .type(GraphQLString)
                        .staticValue("LineString")
                )
                .field(newFieldDefinition()
                        .name("coordinates")
                        .type(GraphQLGeoJSONCoordinates))
                .build();


        List<GraphQLFieldDefinition> commonFieldsList = new ArrayList<>();
        commonFieldsList.add(newFieldDefinition().name(NAME).type(embeddableMultilingualStringObjectType).build());
        commonFieldsList.add(newFieldDefinition().name(SHORT_NAME).type(embeddableMultilingualStringObjectType).build());
        commonFieldsList.add(newFieldDefinition().name(DESCRIPTION).type(embeddableMultilingualStringObjectType).build());

        commonFieldsList.add(newFieldDefinition()
                .name(GEOMETRY)
                .type(geoJsonPointObjectType)
                .dataFetcher(env -> {
                    if (env.getSource() instanceof Zone_VersionStructure) {
                        Zone_VersionStructure source = (Zone_VersionStructure) env.getSource();
                        return source.getCentroid();
                    }
                    return null;
                })
                .build());

        commonFieldsList.add(newFieldDefinition()
                .name(IMPORTED_ID)
                .type(new GraphQLList(GraphQLString))
                .dataFetcher(env -> {
                    DataManagedObjectStructure source = (DataManagedObjectStructure) env.getSource();
                    if (source != null) {
                        return source.getOriginalIds();
                    } else {
                        return null;
                    }
                })
                .build());

        GraphQLFieldDefinition netexIdFieldDefinition = newFieldDefinition()
                .name(ID)
                .type(GraphQLString)
                .dataFetcher(env -> {
                    if(env.getSource() instanceof EntityStructure) {
                        return NetexIdMapper.getNetexId((EntityStructure) env.getSource());
                    } else if(env.getSource() instanceof PathLinkEnd) {
                        return NetexIdMapper.getNetexId((PathLinkEnd) env.getSource());
                    }
                    return null;
                })
                .build();

        commonFieldsList.add(netexIdFieldDefinition);
        commonFieldsList.add(newFieldDefinition()
                .name(LOCATION)
                .type(locationObjectType)
                .deprecate("Use geometry-object instead")
                .dataFetcher(env -> {
                    Zone_VersionStructure source = (Zone_VersionStructure) env.getSource();
                    Point point = source.getCentroid();
                    if (point != null) {
                        LocationDto dto = new LocationDto();
                        dto.longitude = point.getX();
                        dto.latitude = point.getY();
                        return dto;
                    } else {
                        return null;
                    }
                }).build());

        GraphQLObjectType quayObjectType = newObject()
                .name(OUTPUT_TYPE_QUAY)
                .fields(commonFieldsList)
                .field(newFieldDefinition()
                        .name(COMPASS_BEARING)
                        .type(GraphQLBigDecimal))
                .field(newFieldDefinition()
                        .name(ALL_AREAS_WHEELCHAIR_ACCESSIBLE)
                        .type(GraphQLBoolean))
                .field(newFieldDefinition()
                        .name(VERSION)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(PUBLIC_CODE)
                        .type(GraphQLString))
                .build();

        GraphQLObjectType stopPlaceObjectType = newObject()
                .name(OUTPUT_TYPE_STOPPLACE)
                .fields(commonFieldsList)
                .field(newFieldDefinition()
                        .name(STOP_PLACE_TYPE)
                        .type(stopPlaceTypeEnum))
                .field(newFieldDefinition()
                        .name(ALL_AREAS_WHEELCHAIR_ACCESSIBLE)
                        .type(GraphQLBoolean))
                .field(newFieldDefinition()
                        .name(TOPOGRAPHIC_PLACE)
                        .type(topographicPlaceObjectType))
                .field(newFieldDefinition()
                        .name(VERSION)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(QUAYS)
                        .type(new GraphQLList(quayObjectType)))
                .build();

        GraphQLObjectType pathLinkEndObjectType = createPathLinkEndObjectType(quayObjectType, stopPlaceObjectType, netexIdFieldDefinition);
        GraphQLObjectType pathLinkObjectType = createPathLinkObjectType(pathLinkEndObjectType, netexIdFieldDefinition);

        GraphQLObjectType stopPlaceRegisterQuery = newObject()
                .name("StopPlaceRegister")
                .description("Query and search for data")
                .field(newFieldDefinition()
                        .type(new GraphQLList(stopPlaceObjectType))
                        .name(FIND_STOPPLACE)
                        .description("Search for StopPlaces")
                        .argument(GraphQLArgument.newArgument()
                                .name(PAGE)
                                .type(GraphQLInt)
                                .defaultValue(DEFAULT_PAGE_VALUE)
                                .description(PAGE_DESCRIPTION_TEXT))
                        .argument(GraphQLArgument.newArgument()
                                .name(SIZE)
                                .type(GraphQLInt)
                                .defaultValue(DEFAULT_SIZE_VALUE)
                                .description(SIZE_DESCRIPTION_TEXT))
                        .argument(GraphQLArgument.newArgument()
                                .name(ID)
                                .type(GraphQLString)
                                .description("IDs used to lookup StopPlace(s). When used - all other searchparameters are ignored."))
                        //Search
                        .argument(GraphQLArgument.newArgument()
                                .name(STOP_PLACE_TYPE)
                                .type(new GraphQLList(stopPlaceTypeEnum))
                                .description("Only return StopPlaces with given StopPlaceType(s)."))
                        .argument(GraphQLArgument.newArgument()
                                .name(COUNTY_REF)
                                .type(new GraphQLList(GraphQLString))
                                .description("Only return StopPlaces located in given counties."))
                        .argument(GraphQLArgument.newArgument()
                                .name(MUNICIPALITY_REF)
                                .type(new GraphQLList(GraphQLString))
                                .description("Only return StopPlaces located in given municipalities."))
                        .argument(GraphQLArgument.newArgument()
                                .name(QUERY)
                                .type(GraphQLString)
                                .description("Searches for StopPlace by name."))
                        .argument(GraphQLArgument.newArgument()
                                .name(IMPORTED_ID_QUERY)
                                .type(GraphQLString)
                                .description("Searches for StopPlace by importedId."))
                        .dataFetcher(stopPlaceFetcher))
                        //Search by BoundingBox
                .field(newFieldDefinition()
                        .type(new GraphQLList(stopPlaceObjectType))
                        .name(FIND_STOPPLACE_BY_BBOX)
                        .description("Find StopPlaces within given BoundingBox.")
                        .argument(GraphQLArgument.newArgument()
                                .name(PAGE)
                                .type(GraphQLInt)
                                .defaultValue(DEFAULT_PAGE_VALUE)
                                .description(PAGE_DESCRIPTION_TEXT))
                        .argument(GraphQLArgument.newArgument()
                                .name(SIZE)
                                .type(GraphQLInt)
                                .defaultValue(DEFAULT_SIZE_VALUE)
                                .description(SIZE_DESCRIPTION_TEXT))
                                //BoundingBox
                        .argument(GraphQLArgument.newArgument()
                                .name(LONGITUDE_MIN)
                                .description("Bottom left longitude (xMin).")
                                .type(new GraphQLNonNull(GraphQLBigDecimal)))
                        .argument(GraphQLArgument.newArgument()
                                .name(LATITUDE_MIN)
                                .description("Bottom left latitude (yMin).")
                                .type(new GraphQLNonNull(GraphQLBigDecimal)))
                        .argument(GraphQLArgument.newArgument()
                                .name(LONGITUDE_MAX)
                                .description("Top right longitude (xMax).")
                                .type(new GraphQLNonNull(GraphQLBigDecimal)))
                        .argument(GraphQLArgument.newArgument()
                                .name(LATITUDE_MAX)
                                .description("Top right longitude (yMax).")
                                .type(new GraphQLNonNull(GraphQLBigDecimal)))
                        .argument(GraphQLArgument.newArgument()
                                .name(IGNORE_STOPPLACE_ID)
                                .type(GraphQLString)
                                .description("ID of StopPlace to excluded from result."))
                        .dataFetcher(stopPlaceFetcher))
                .field(newFieldDefinition()
                        .name(FIND_TOPOGRAPHIC_PLACE)
                        .type(new GraphQLList(topographicPlaceObjectType))
                        .description("Find topographic places")
                        .argument(GraphQLArgument.newArgument()
                                .name(ID)
                                .type(GraphQLString))
                        .argument(GraphQLArgument.newArgument()
                                .name(TOPOGRAPHIC_PLACE_TYPE)
                                .type(topographicPlaceTypeEnum)
                                .description("Limits results to specified placeType."))
                        .argument(GraphQLArgument.newArgument()
                                .name(QUERY)
                                .type(GraphQLString)
                                .description("Searches for TopographicPlaces by name."))
                        .dataFetcher(topographicPlaceFetcher))
                .field(newFieldDefinition()
                        .name(FIND_PATH_LINK)
                        .type(new GraphQLList(pathLinkObjectType))
                        .description("Find path links")
                        .argument(GraphQLArgument.newArgument()
                                .name(ID)
                                .type(GraphQLString))
                        .dataFetcher(pathLinkFetcher))
                .build();

        GraphQLInputObjectType embeddableMultiLingualStringInputObjectType = createEmbeddableMultiLingualStringInputObjectType();

        List<GraphQLInputObjectField> commonInputFieldList = createCommonInputFieldList(embeddableMultiLingualStringInputObjectType);

        GraphQLInputObjectType quayInputObjectType = createQuayInputObjectType(commonInputFieldList);
        GraphQLInputObjectType topographicPlaceInputObjectType = createTopographicPlaceInputObjectType(embeddableMultiLingualStringInputObjectType);
        GraphQLInputObjectType stopPlaceInputObjectType = createStopPlaceInputObjectType(commonInputFieldList, topographicPlaceInputObjectType, quayInputObjectType);

        GraphQLObjectType stopPlaceRegisterMutation = newObject()
                .name("StopPlaceMutation")
                .description("Create and edit stopplaces")
                .field(newFieldDefinition()
                        .type(new GraphQLList(stopPlaceObjectType))
                        .name(MUTATE_STOPPLACE)
                        .description("Create new or update existing StopPlace")
                        .argument(GraphQLArgument.newArgument().name(OUTPUT_TYPE_STOPPLACE).type(stopPlaceInputObjectType))
                        .dataFetcher(stopPlaceUpdater))
                .build();

        GraphQLInputObjectType transferDurationInputObjectType = createTransferDurationInputObjectType();
        GraphQLInputObjectType pathLinkObjectInputType = createPathLinkInputObjectType(quayInputObjectType, transferDurationInputObjectType);


        stopPlaceRegisterSchema = GraphQLSchema.newSchema()
                .query(stopPlaceRegisterQuery)
                .mutation(stopPlaceRegisterMutation)
                .build();
    }

    private GraphQLObjectType createPathLinkEndObjectType(GraphQLObjectType quayObjectType, GraphQLObjectType stopPlaceObjectType, GraphQLFieldDefinition netexIdFieldDefinition) {
        return newObject()
                .name("PathLinkEnd")
                .field(netexIdFieldDefinition)
                .field(newFieldDefinition()
                        .name("quay")
                        .type(quayObjectType))
                .field(newFieldDefinition()
                        .name("stopPlace")
                        .type(stopPlaceObjectType))
                .build();
    }

    private GraphQLObjectType createPathLinkObjectType(GraphQLObjectType pathLinkEndObjecttype, GraphQLFieldDefinition netexIdFieldDefinition) {
        return newObject()
                .name(OUTPUT_TYPE_PATH_LINK)
                .field(netexIdFieldDefinition)
                .field(newFieldDefinition()
                        .name("from")
                        .type(pathLinkEndObjecttype))
                .field(newFieldDefinition()
                        .name("to")
                        .type(pathLinkEndObjecttype))
                .build();
    }

    private GraphQLInputObjectType createTopographicPlaceInputObjectType(GraphQLInputObjectType embeddableMultiLingualStringInputObjectType) {
        return GraphQLInputObjectType.newInputObject()
                .name(INPUT_TYPE_TOPOGRAPHIC_PLACE)
                .field(newInputObjectField()
                        .name(ID)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(TOPOGRAPHIC_PLACE_TYPE)
                        .type(topographicPlaceTypeEnum))
                .field(newInputObjectField()
                        .name(NAME)
                        .type(embeddableMultiLingualStringInputObjectType))
                .build();
    }

    private GraphQLInputObjectType createStopPlaceInputObjectType(List<GraphQLInputObjectField> commonInputFieldsList,
                                                                  GraphQLInputObjectType topographicPlaceInputObjectType,
                                                                  GraphQLInputObjectType quayObjectInputType) {
        return GraphQLInputObjectType.newInputObject()
                .name(INPUT_TYPE_STOPPLACE)
                .fields(commonInputFieldsList)
                .field(newInputObjectField()
                        .name(STOP_PLACE_TYPE)
                        .type(stopPlaceTypeEnum))
                .field(newInputObjectField()
                        .name(ALL_AREAS_WHEELCHAIR_ACCESSIBLE)
                        .type(GraphQLBoolean))
                .field(newInputObjectField()
                        .name(TOPOGRAPHIC_PLACE)
                        .type(topographicPlaceInputObjectType))
                .field(newInputObjectField()
                        .name(QUAYS)
                        .type(new GraphQLList(quayObjectInputType)))
                .build();
    }

    private GraphQLInputObjectType createEmbeddableMultiLingualStringInputObjectType() {
        return GraphQLInputObjectType.newInputObject()
                .name(INPUT_TYPE_EMBEDDABLE_MULTILINGUAL_STRING)
                .field(newInputObjectField()
                        .name(VALUE)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(LANG)
                        .type(GraphQLString))
                .build();
    }

    private List<GraphQLInputObjectField> createCommonInputFieldList(GraphQLInputObjectType embeddableMultiLingualStringInputObjectType) {
        GraphQLInputObjectType locationInputType = GraphQLInputObjectType.newInputObject()
                .name(INPUT_TYPE_LOCATION)
                .field(newInputObjectField()
                        .name(LATITUDE)
                        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                        .build())
                .field(newInputObjectField()
                        .name(LONGITUDE)
                        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                        .build())
                .build();

        GraphQLInputObjectType geoJsonPointInputType = GraphQLInputObjectType.newInputObject()
                .name(INPUT_TYPE_GEO_JSON)
                .field(newInputObjectField()
                        .name(TYPE)
                        .type(new GraphQLNonNull(GraphQLString))
                        .description("Type of geometry. Valid inputs are 'Point' or 'LineString'.")
                        .build())
                .field(newInputObjectField()
                        .name(COORDINATES)
                        .type(new GraphQLNonNull(GraphQLGeoJSONCoordinates))
                        .build())
                .build();


        List<GraphQLInputObjectField> commonInputFieldsList = new ArrayList<>();
        commonInputFieldsList.add(newInputObjectField().name(ID).type(GraphQLString).description("Ignore when creating new").build());
        commonInputFieldsList.add(newInputObjectField().name(NAME).type(embeddableMultiLingualStringInputObjectType).build());
        commonInputFieldsList.add(newInputObjectField().name(SHORT_NAME).type(embeddableMultiLingualStringInputObjectType).build());
        commonInputFieldsList.add(newInputObjectField().name(DESCRIPTION).type(embeddableMultiLingualStringInputObjectType).build());
        commonInputFieldsList.add(newInputObjectField().name(LOCATION).type(locationInputType).build());
        commonInputFieldsList.add(newInputObjectField().name(GEOMETRY).type(geoJsonPointInputType).build());

        return commonInputFieldsList;
    }

    private GraphQLInputObjectType createQuayInputObjectType(List<GraphQLInputObjectField> graphQLCommonInputObjectFieldsList) {
        return GraphQLInputObjectType.newInputObject()
                .name(INPUT_TYPE_QUAY)
                .fields(graphQLCommonInputObjectFieldsList)
                .field(newInputObjectField()
                        .name(COMPASS_BEARING)
                        .type(GraphQLBigDecimal))
                .field(newInputObjectField()
                        .name(ALL_AREAS_WHEELCHAIR_ACCESSIBLE)
                        .type(GraphQLBoolean))
                .field(newInputObjectField()
                        .name(PUBLIC_CODE)
                        .type(GraphQLString))
                .build();
    }

    private GraphQLInputObjectType createPathLinkInputObjectType(GraphQLInputObjectType quayObjectInputType, GraphQLInputObjectType transferDurationInputObjectType) {


        GraphQLInputType pathLinkEndInputObjectType = GraphQLInputObjectType.newInputObject()
                .name("PathLinkEndInput")
                .field(newInputObjectField()
                        .name("quay")
                        .type(quayObjectInputType))
                .build();

        GraphQLInputObjectType pathLinkInputObjectType = GraphQLInputObjectType.newInputObject()
                .name(INPUT_TYPE_PATH_LINK)
                .field(newInputObjectField()
                        .name(ID)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name("from")
                        .type(pathLinkEndInputObjectType))
                .field(newInputObjectField()
                        .name("to")
                        .type(pathLinkEndInputObjectType))
                .field(newInputObjectField()
                        .name("transferDuration")
                        .type(transferDurationInputObjectType))
                .description("Transfer durations in seconds")
                .build();

        return pathLinkInputObjectType;

    }

    private GraphQLInputObjectType createTransferDurationInputObjectType() {
        return GraphQLInputObjectType.newInputObject()
                .name(INPUT_TYPE_TRANSFER_DURATION)
                .field(newInputObjectField()
                        .name(ID)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name("defaultDuration")
                        .type(GraphQLInt)
                        .description("Default duration in seconds"))
                .field(newInputObjectField()
                        .name("frequentTravellerDuration")
                        .type(GraphQLInt)
                        .description("Frequent traveller duration in seconds"))
                .field(newInputObjectField()
                        .name("occasionalTravellerDuration")
                        .type(GraphQLInt)
                        .description("Occasional traveller duration in seconds"))
                .build();
    }
}

