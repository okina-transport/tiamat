package org.rutebanken.tiamat.rest.dto;

import org.rutebanken.tiamat.dtoassembling.dto.IdMappingDto;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;

import static org.rutebanken.tiamat.config.JerseyConfig.SERVICES_PATH;

@Component
@Produces("application/json")
@Path(SERVICES_PATH + "/stop_places/mapping/stop_place")
@Transactional
public class DtoStopPlaceResource {

    private static final Logger logger = LoggerFactory.getLogger(DtoStopPlaceResource.class);

    private final StopPlaceRepository stopPlaceRepository;

    @Autowired
    public DtoStopPlaceResource(StopPlaceRepository stopPlaceRepository) {
        this.stopPlaceRepository = stopPlaceRepository;
    }

    @GET
    @Produces("text/plain")
    public Response getIdMapping(@DefaultValue(value = "300000") @QueryParam(value = "recordsPerRoundTrip") int recordsPerRoundTrip,
                                            @QueryParam("includeStopType") boolean includeStopType) {

        logger.info("Fetching StopPlace mapping table...");

        return Response.ok().entity((StreamingOutput) output -> {

            int recordPosition = 0;
            boolean lastEmpty = false;
            Instant now = Instant.now();
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)))) {
                while (!lastEmpty) {

                    List<IdMappingDto> stopPlaceMappings = stopPlaceRepository.findKeyValueMappingsForStop(now, recordPosition, recordsPerRoundTrip);
                    for (IdMappingDto mapping : stopPlaceMappings) {
                        writer.println(mapping.toCsvString(includeStopType));
                        recordPosition++;
                    }
                    writer.flush();
                    if (stopPlaceMappings.isEmpty()) lastEmpty = true;
                }
                writer.close();
            }
        }).build();
    }
}
