package org.rutebanken.tiamat.rest.dto;

import org.rutebanken.tiamat.dtoassembling.dto.IdMappingDto;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

@Component
@Produces("application/json")
@Path("/quay")
public class DtoQuayResource {


    private final StopPlaceRepository stopPlaceRepository;

    @Autowired
    public DtoQuayResource(StopPlaceRepository stopPlaceRepository) {
        this.stopPlaceRepository = stopPlaceRepository;
    }

    @GET
    @Produces("text/plain")
    @Path("/id_mapping")
    public Response getIdMapping(@DefaultValue(value = "20000") @QueryParam(value = "recordsPerRoundTrip") int recordsPerRoundTrip) {

        return  Response.ok().entity((StreamingOutput) output -> {

            int recordPosition = 0;
            boolean lastEmpty = false;

            try ( PrintWriter writer = new PrintWriter( new BufferedWriter( new OutputStreamWriter( output ) ) ) ) {
                while (!lastEmpty) {

                    List<IdMappingDto> quayMappings = stopPlaceRepository.findKeyValueMappingsForQuay(recordPosition, recordsPerRoundTrip);
                    for (IdMappingDto mapping : quayMappings) {
                        writer.println(mapping.toCsvString());
                        recordPosition ++;
                    }
                    writer.flush();
                    if(quayMappings.isEmpty()) lastEmpty = true;
                }
                writer.close();
            }
        }).build();
    }

}
