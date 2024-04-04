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

package org.rutebanken.tiamat.rest.graphql.types;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.SiteRefStructure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.parkingReservationEnum;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.parkingTypeEnum;

@Component
public class ParkingInterfaceCreator {

    public List<GraphQLFieldDefinition> createCommonInterfaceFields(GraphQLObjectType topographicPlaceObjectType,
                                                                    GraphQLObjectType validBetweenObjectType) {
        List<GraphQLFieldDefinition> parkingInterfaceFields = new ArrayList<>();
        parkingInterfaceFields.add(newFieldDefinition()
                .name(VERSION_COMMENT)
                .type(GraphQLString)
                .build());
        parkingInterfaceFields.add(newFieldDefinition()
                .name(CHANGED_BY)
                .type(GraphQLString).build());
        parkingInterfaceFields.add(newFieldDefinition()
                .name(TOPOGRAPHIC_PLACE)
                .type(topographicPlaceObjectType).build());
        parkingInterfaceFields.add(newFieldDefinition()
                .name(VALID_BETWEEN)
                .type(validBetweenObjectType).build());
        parkingInterfaceFields.add(newFieldDefinition()
                .name(PRINCIPAL_CAPACITY)
                .type(GraphQLBigInteger).build());
        parkingInterfaceFields.add(newFieldDefinition()
                .name(PARKING_TYPE)
                .type(parkingTypeEnum).build());
        parkingInterfaceFields.add(newFieldDefinition()
                .name(OVERNIGHT_PARKING_PERMITTED)
                .type(GraphQLBoolean).build());
        parkingInterfaceFields.add(newFieldDefinition()
                .name(REAL_TIME_OCCUPANCY_AVAILABLE)
                .type(GraphQLBoolean).build());
        parkingInterfaceFields.add(newFieldDefinition()
                .name(PARKING_RESERVATION)
                .type(parkingReservationEnum).build());
        parkingInterfaceFields.add(newFieldDefinition()
                .name(BOOKING_URL)
                .type(GraphQLString).build());
        parkingInterfaceFields.add(newFieldDefinition()
                .name(FREE_PARKING_OUT_OF_HOURS)
                .type(GraphQLBoolean).build());
        parkingInterfaceFields.add(newFieldDefinition()
                .name(PARENT_SITE_REF)
                .type(GraphQLString)
                .dataFetcher(env -> {
                    SiteRefStructure parentSiteRef = ((Parking) env.getSource()).getParentSiteRef();
                    if (parentSiteRef != null) {
                        return parentSiteRef.getRef();
                    }
                    return null;
                })

                .build());
        return parkingInterfaceFields;
    }


    public GraphQLInterfaceType createInterface(List<GraphQLFieldDefinition> parkingInterfaceFields,
                                                List<GraphQLFieldDefinition> commonFieldsList,
                                                TypeResolver parkingTypeResolver) {
        return newInterface()
                .name(OUTPUT_TYPE_PARKING_INTERFACE)
                .fields(commonFieldsList)
                .fields(parkingInterfaceFields)
                .typeResolver(parkingTypeResolver)
                .build();
    }

}
