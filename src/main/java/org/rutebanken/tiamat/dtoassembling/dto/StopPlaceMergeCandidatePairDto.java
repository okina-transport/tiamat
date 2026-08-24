package org.rutebanken.tiamat.dtoassembling.dto;

public class StopPlaceMergeCandidatePairDto {

    private final StopPlaceMergeCandidateDto base;
    private final StopPlaceMergeCandidateDto candidate;

    public StopPlaceMergeCandidatePairDto(Object[] row) {
        this.base = new StopPlaceMergeCandidateDto(row, 0);
        this.candidate = new StopPlaceMergeCandidateDto(row, 6);
    }

    public StopPlaceMergeCandidateDto getBase() {
        return base;
    }

    public StopPlaceMergeCandidateDto getCandidate() {
        return candidate;
    }
}
