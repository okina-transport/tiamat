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

package org.rutebanken.tiamat.rest.graphql.operations;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import org.rutebanken.tiamat.service.parking.ParkingDeleter;
import org.rutebanken.tiamat.service.poi.PointOfInterestDeleter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

@Component
public class PointOfInterestOperationsBuilder {

    @Autowired
    private PointOfInterestDeleter pointOfInterestDeleter;

    public List<GraphQLFieldDefinition> getPointOfInterestOperations() {
        List<GraphQLFieldDefinition> operations = new ArrayList<>();

        //Delete PointOfInterest
        operations.add(newFieldDefinition()
                .type(GraphQLBoolean)
                .name(DELETE_POI)
                .description("!!! Deletes all versions of PointOfInterest from database - use with caution !!!")
                .argument(newArgument().name(POI_ID).type(new GraphQLNonNull(GraphQLString)))
                .dataFetcher(environment -> pointOfInterestDeleter.deletePointOfInterest(environment.getArgument(POI_ID)))
                .build());

        return operations;
    }

}
