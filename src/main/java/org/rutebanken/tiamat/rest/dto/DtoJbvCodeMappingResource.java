package org.rutebanken.tiamat.rest.dto;

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

import static org.rutebanken.tiamat.repository.QuayRepositoryImpl.JBV_CODE;

@Component
@Produces("application/json")
@Path("/admin/jbv_code_mapping")
public class DtoJbvCodeMappingResource {

    private static final Logger logger = LoggerFactory.getLogger(DtoJbvCodeMappingResource.class);

    private final QuayRepository quayRepository;

    @Autowired
    public DtoJbvCodeMappingResource(QuayRepository quayRepository) {
        this.quayRepository = quayRepository;
    }


    @GET
    @Produces("text/plain")
    public Response getJbvCodeMapping() {

        logger.info("Fetching JBV mapping table for all Quays containg keyValue {}...", JBV_CODE);

        return Response.ok().entity((StreamingOutput) output -> {

            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)))) {
                    List<JbvCodeMappingDto> quayMappings = quayRepository.findJbvCodeMappingsForQuay();
                    for (JbvCodeMappingDto mapping : quayMappings) {
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
