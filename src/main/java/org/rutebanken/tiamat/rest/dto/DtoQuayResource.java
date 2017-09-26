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

package org.rutebanken.tiamat.rest.dto;

import org.rutebanken.tiamat.dtoassembling.dto.IdMappingDto;
import org.rutebanken.tiamat.dtoassembling.dto.JbvCodeMappingDto;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;

import static org.rutebanken.tiamat.repository.QuayRepositoryImpl.JBV_CODE;

@Component
@Produces("application/json")
@Path("/quay")
public class DtoQuayResource {

    private static final Logger logger = LoggerFactory.getLogger(DtoQuayResource.class);

    private final QuayRepository quayRepository;
    private final StopPlaceRepository stopPlaceRepository;

    @Autowired
    public DtoQuayResource(QuayRepository quayRepository, StopPlaceRepository stopPlaceRepository) {
        this.quayRepository = quayRepository;
        this.stopPlaceRepository = stopPlaceRepository;
    }

    @GET
    @Produces("text/plain")
    @Path("/id_mapping")
    public Response getIdMapping(@DefaultValue(value = "300000") @QueryParam(value = "recordsPerRoundTrip") int recordsPerRoundTrip,
                                        @QueryParam("includeStopType") boolean includeStopType) {

        logger.info("Fetching Quay mapping table...");

        return Response.ok().entity((StreamingOutput) output -> {

            int recordPosition = 0;
            boolean lastEmpty = false;
            Instant now = Instant.now();
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)))) {
                while (!lastEmpty) {
                    List<IdMappingDto> quayMappings = quayRepository.findKeyValueMappingsForQuay(now, recordPosition, recordsPerRoundTrip);
                    for (IdMappingDto mapping : quayMappings) {
                        writer.println(mapping.toCsvString(includeStopType));
                        recordPosition++;
                    }
                    writer.flush();
                    if (quayMappings.isEmpty()) lastEmpty = true;
                }
                writer.close();
            } catch (Exception e) {
                logger.warn("Catched exception when streaming id map for quay: {}", e.getMessage(), e);
                throw e;
            }
        }).build();
    }


    @GET
    @Produces("text/plain")
    @Path("/jbv_code_mapping")
    public Response getJbvCodeMapping() {

        logger.info("Fetching Quay mapping table for all Quays containg keyValue {}...", JBV_CODE);

        return Response.ok().entity((StreamingOutput) output -> {

            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)))) {
                    List<JbvCodeMappingDto> quayMappings = quayRepository.findJbvCodeMappingsForQuay();
                    for (JbvCodeMappingDto mapping : quayMappings) {
                        writer.println(mapping.toCsvString());
                    }
                    List<JbvCodeMappingDto> stopPlaceMappings = stopPlaceRepository.findJbvCodeMappingsForStopPlace();
                    for (JbvCodeMappingDto mapping : stopPlaceMappings) {
                        writer.println(mapping.toCsvString());
                    }
                    writer.flush();
                writer.close();
            } catch (Exception e) {
                logger.warn("Catched exception when streaming id map for quay: {}", e.getMessage(), e);
                throw e;
            }
        }).build();
    }

}
