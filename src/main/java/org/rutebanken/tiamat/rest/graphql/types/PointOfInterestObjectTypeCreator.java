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
import org.springframework.stereotype.Component;

import java.util.List;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.*;

@Component
public class PointOfInterestObjectTypeCreator {

    public GraphQLObjectType create(GraphQLInterfaceType pointOfInterestInterface, List<GraphQLFieldDefinition> pointOfInterestInterfaceFields, List<GraphQLFieldDefinition> commonFieldsList) {
        return newObject()
                .name(OUTPUT_TYPE_POINT_OF_INTEREST)
                .withInterface(pointOfInterestInterface)
                .fields(pointOfInterestInterfaceFields)
                .fields(commonFieldsList)
                .field(newFieldDefinition()
                        .name(ZIP_CODE)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(ADDRESS)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(CITY)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(POSTAL_CODE)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(PARENT_SITE_REF)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(POI_FACILITY_SET)
                        .type(pointOfInterestFacilitySetObjectType))
                .field(newFieldDefinition()
                        .name(POI_CLASSIFICATIONS)
                        .type(new GraphQLList(pointOfInterestClassificationObjectType)))
                .build();
    }
}
