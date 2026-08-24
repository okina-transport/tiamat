package org.rutebanken.tiamat.dtoassembling.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public class StopPlaceMergeCandidatePageDto {

    public final List<StopPlaceMergeCandidatePairDto> content;
    public final int page;
    public final int size;
    public final boolean hasMore;

    public StopPlaceMergeCandidatePageDto(Page<StopPlaceMergeCandidatePairDto> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.hasMore = page.getContent().size() == page.getSize();
    }
}
