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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

@Component
public class PointOfInterestInterfaceCreator {

    public List<GraphQLFieldDefinition> createCommonInterfaceFields(GraphQLObjectType topographicPlaceObjectType,
                                                                    GraphQLObjectType validBetweenObjectType) {
        List<GraphQLFieldDefinition> pointOfInterestInterfaceFields = new ArrayList<>();
        pointOfInterestInterfaceFields.add(newFieldDefinition()
                .name(VERSION_COMMENT)
                .type(GraphQLString)
                .build());
        pointOfInterestInterfaceFields.add(newFieldDefinition()
                .name(CHANGED_BY)
                .type(GraphQLString)
                .build());
        pointOfInterestInterfaceFields.add(newFieldDefinition()
                .name(TOPOGRAPHIC_PLACE)
                .type(topographicPlaceObjectType)
                .build());
        pointOfInterestInterfaceFields.add(newFieldDefinition()
                .name(VALID_BETWEEN)
                .type(validBetweenObjectType)
                .build());
        return pointOfInterestInterfaceFields;
    }


    public GraphQLInterfaceType createInterface(List<GraphQLFieldDefinition> pointOfInterestInterfaceFields,
                                                List<GraphQLFieldDefinition> commonFieldsList,
                                                TypeResolver pointOfInterestTypeResolver) {
        return newInterface()
                .name(OUTPUT_TYPE_POINT_OF_INTEREST_INTERFACE)
                .fields(commonFieldsList)
                .fields(pointOfInterestInterfaceFields)
                .typeResolver(pointOfInterestTypeResolver)
                .build();
    }

}