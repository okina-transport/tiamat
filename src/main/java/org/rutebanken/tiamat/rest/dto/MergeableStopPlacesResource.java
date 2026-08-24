package org.rutebanken.tiamat.rest.dto;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.rutebanken.tiamat.dtoassembling.dto.MergeMode;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeCandidatePageDto;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeCandidatePairDto;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Produces("application/json")
@Path("/mergeable")
public class MergeableStopPlacesResource {

    private final StopPlaceRepository stopPlaceRepository;

    public MergeableStopPlacesResource(StopPlaceRepository stopPlaceRepository) {
        this.stopPlaceRepository = stopPlaceRepository;
    }

    @GET
    public Response getMergeableStopPlaces(@QueryParam("mode") String modeParam,
                                            @QueryParam("provider") String provider,
                                            @QueryParam("page") @DefaultValue("0") int page,
                                            @QueryParam("size") @DefaultValue("100") int size) {
        MergeMode mode;
        try {
            mode = MergeMode.valueOf(modeParam);
        } catch (Exception e) {
            throw new WebApplicationException(
                    "Invalid or missing 'mode' query parameter. Allowed values: SAME_PROVIDER, MULTI_PROVIDER",
                    Response.Status.BAD_REQUEST);
        }

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(mode, provider, PageRequest.of(page, size));
        return Response.ok(new StopPlaceMergeCandidatePageDto(result)).build();
    }
}
