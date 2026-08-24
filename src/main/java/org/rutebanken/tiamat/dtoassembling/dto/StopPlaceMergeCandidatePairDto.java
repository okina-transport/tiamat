package org.rutebanken.tiamat.dtoassembling.dto;

public class StopPlaceMergeCandidatePairDto {

    private final StopPlaceMergeCandidateDto base;
    private final StopPlaceMergeCandidateDto candidate;

    public StopPlaceMergeCandidatePairDto(StopPlaceMergeCandidateDto base, StopPlaceMergeCandidateDto candidate) {
        this.base = base;
        this.candidate = candidate;
    }

    public StopPlaceMergeCandidateDto getBase() {
        return base;
    }

    public StopPlaceMergeCandidateDto getCandidate() {
        return candidate;
    }
}
