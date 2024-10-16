package org.rutebanken.tiamat.rest.netex.publicationdelivery.mapper;

import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.DtoNetexExportSummary;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class NetexExportSummaryMapper {

    public List<DtoNetexExportSummary> mapJobToExportSummary(List<Job> jobs) {
        List<DtoNetexExportSummary> exportNetexSummary = Collections.emptyList();
        if (!CollectionUtils.isEmpty(jobs)) {
            exportNetexSummary = jobs.stream()
                    .map(this::mapJobToExportSummary)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return exportNetexSummary ;
    }

    protected DtoNetexExportSummary mapJobToExportSummary(Job job) {
        DtoNetexExportSummary dtoNetexExportSummary = null;
        if (job != null) {
            dtoNetexExportSummary = new DtoNetexExportSummary();
            dtoNetexExportSummary.setFileName(job.getFileName());
            dtoNetexExportSummary.setUserName(job.getUserName());
            dtoNetexExportSummary.setCreationDate(job.getStarted());
        }
        return dtoNetexExportSummary;
    }
}
