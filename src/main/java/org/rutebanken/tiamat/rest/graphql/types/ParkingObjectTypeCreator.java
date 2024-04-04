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
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.SiteRefStructure;
import org.springframework.stereotype.Component;

import java.util.List;

import static graphql.Scalars.GraphQLBigInteger;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.CARPOOLING_AVAILABLE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.CARSHARING_AVAILABLE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.COVERED;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.OUTPUT_TYPE_PARKING;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PARENT_SITE_REF;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PARKING_AREAS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PARKING_LAYOUT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PARKING_PAYMENT_PROCESS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PARKING_PROPERTIES;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PARKING_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PARKING_VEHICLE_TYPES;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PRINCIPAL_CAPACITY;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.RECHARGING_AVAILABLE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SECURE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.TOTAL_CAPACITY;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.TYPE_OF_PARKING_REF;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.*;

@Component
public class ParkingObjectTypeCreator {

    public GraphQLObjectType create(GraphQLInterfaceType parkingInterface, List<GraphQLFieldDefinition> parkingInterfaceFields, List<GraphQLFieldDefinition> commonFieldsList) {
        return newObject()
                .name(OUTPUT_TYPE_PARKING)
                .withInterface(parkingInterface)
                .fields(parkingInterfaceFields)
                .fields(commonFieldsList)
                .field(newFieldDefinition()
                        .name(PARKING_TYPE)
                        .type(parkingTypeEnum))
                .field(newFieldDefinition()
                        .name(TOTAL_CAPACITY)
                        .type(GraphQLBigInteger))
                .field(newFieldDefinition()
                        .name(PARKING_VEHICLE_TYPES)
                        .type(new GraphQLList(parkingVehicleEnum)))
                .field(newFieldDefinition()
                        .name(COVERED)
                        .type(parkingCoveredEnum))
                .field(newFieldDefinition()
                        .name(SECURE)
                        .type(GraphQLBoolean))
                .field(newFieldDefinition()
                        .name(TYPE_OF_PARKING_REF)
                        .type(typeOfParkingRefEnumeration))
                .field(newFieldDefinition()
                        .name(PARKING_LAYOUT)
                        .type(parkingLayoutEnum))
                .field(newFieldDefinition()
                        .name(PARKING_PAYMENT_PROCESS)
                        .type(new GraphQLList(parkingPaymentProcessEnum)))
                .field(newFieldDefinition()
                        .name(RECHARGING_AVAILABLE)
                        .type(GraphQLBoolean))
                .field(newFieldDefinition()
                        .name(CARPOOLING_AVAILABLE)
                        .type(GraphQLBoolean))
                .field(newFieldDefinition()
                        .name(CARSHARING_AVAILABLE)
                        .type(GraphQLBoolean))
                .field(newFieldDefinition()
                        .name(PARKING_PROPERTIES)
                        .type(new GraphQLList(parkingPropertiesObjectType)))
                .field(newFieldDefinition()
                        .name(PARKING_AREAS)
                        .type(new GraphQLList(parkingAreaObjectType)))
                .field(newFieldDefinition()
                        .name(PARENT_SITE_REF)
                        .type(GraphQLString)
                        .dataFetcher(env -> {
                            SiteRefStructure parentSiteRef = ((Parking) env.getSource()).getParentSiteRef();
                            if (parentSiteRef != null) {
                                return parentSiteRef.getRef();
                            }
                            return null;
                        })
                )
                .build();
    }
}
