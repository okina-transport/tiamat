package org.rutebanken.tiamat.rest.merge;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.rutebanken.tiamat.dtoassembling.dto.JobDto;
import org.rutebanken.tiamat.dtoassembling.dto.MergeMode;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeCandidatePageDto;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeCandidatePairDto;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeRequestDto;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.service.stopplace.StopPlaceMergeJobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Produces("application/json")
@Path("/mergeable")
public class MergeableStopPlacesResource {

    private final StopPlaceRepository stopPlaceRepository;
    private final StopPlaceMergeJobService stopPlaceMergeJobService;

    public MergeableStopPlacesResource(StopPlaceRepository stopPlaceRepository, StopPlaceMergeJobService stopPlaceMergeJobService) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.stopPlaceMergeJobService = stopPlaceMergeJobService;
    }

    @GET
    public StopPlaceMergeCandidatePageDto getMergeableStopPlaces(@QueryParam("mode") String modeParam,
                                                                   @QueryParam("provider") String provider,
                                                                   @QueryParam("page") @DefaultValue("0") int page,
                                                                   @QueryParam("size") @DefaultValue("100") int size) {
        MergeMode mode;
        try {
            mode = MergeMode.valueOf(modeParam);
        } catch (Exception e) {
            throw new BadRequestException(
                    "Invalid or missing 'mode' query parameter. Allowed values: SAME_PROVIDER, MULTI_PROVIDER");
        }

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(mode, provider, PageRequest.of(page, size));
        return new StopPlaceMergeCandidatePageDto(result);
    }

    @POST
    @Path("/merge")
    @PreAuthorize("@rolesChecker.hasRoleEdit()")
    @Consumes(MediaType.APPLICATION_JSON)
    public JobDto triggerMerge(List<StopPlaceMergeRequestDto> couples) {
        return JobDto.from(stopPlaceMergeJobService.triggerMerge(couples));
    }

    @GET
    @Path("/merge/{jobId}")
    public JobDto getMergeJobProgress(@PathParam("jobId") long jobId) {
        return JobDto.from(stopPlaceMergeJobService.getMergeJobProgress(jobId));
    }
}
