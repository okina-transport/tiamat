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

import graphql.schema.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

@Component
public class ClusterMarkerInterfaceCreator {

    public List<GraphQLFieldDefinition> createCommonInterfaceFields() {
        List<GraphQLFieldDefinition> clusterMarkerFields = new ArrayList<>();
        clusterMarkerFields.add(newFieldDefinition()
                .name(CLUSTER_ID)
                .type(GraphQLInt)
                .build());

        clusterMarkerFields.add(newFieldDefinition()
                .name(LONGITUDE)
                .type(GraphQLFloat)
                .build());

        clusterMarkerFields.add(newFieldDefinition()
                .name(LATITUDE)
                .type(GraphQLFloat)
                .build());

        clusterMarkerFields.add(newFieldDefinition()
                .name(SIZE)
                .type(GraphQLInt)
                .build());

        return clusterMarkerFields;
    }


    public GraphQLInterfaceType createInterface(List<GraphQLFieldDefinition> commonFieldsList,
                                                TypeResolver clusterMarkerResolver) {
        return newInterface()
                .name(OUTPUT_TYPE_CLUSTER_MARKER_INTERFACE)
                .fields(commonFieldsList)
                .typeResolver(clusterMarkerResolver)
                .build();
    }

}
