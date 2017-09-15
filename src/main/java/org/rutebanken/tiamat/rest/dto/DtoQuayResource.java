package org.rutebanken.tiamat.rest.dto;

import io.swagger.annotations.Api;
import org.rutebanken.tiamat.dtoassembling.dto.IdMappingDto;
import org.rutebanken.tiamat.dtoassembling.dto.JbvCodeMappingDto;
import org.rutebanken.tiamat.repository.QuayRepository;
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

import static org.rutebanken.tiamat.config.JerseyConfig.SERVICES_PATH;
import static org.rutebanken.tiamat.repository.QuayRepositoryImpl.JBV_CODE;

@Component
@Api
@Produces("application/json")
@Path(SERVICES_PATH + "/stop_places/mapping/quay")
public class DtoQuayResource {

    private static final Logger logger = LoggerFactory.getLogger(DtoQuayResource.class);

    private final QuayRepository quayRepository;

    @Autowired
    public DtoQuayResource(QuayRepository quayRepository) {
        this.quayRepository = quayRepository;
    }

    @GET
    @Produces("text/plain")
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
}
