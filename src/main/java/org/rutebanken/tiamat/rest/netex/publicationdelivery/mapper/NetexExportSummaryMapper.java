package org.rutebanken.tiamat.rest.netex.publicationdelivery.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.DtoNetexExportSummary;
import org.springframework.stereotype.Component;

import java.util.*;

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
        if (job == null) {
            return null;
        }

        DtoNetexExportSummary dto = new DtoNetexExportSummary();

        dto.setId(job.getId());
        dto.setJobUrl(job.getJobUrl());
        dto.setFileName(job.getFileName());
        dto.setSubFolder(job.getSubFolder());
        dto.setMessage(job.getMessage());
        dto.setStarted(job.getStarted());
        dto.setFinished(job.getFinished());
        dto.setStatus(job.getStatus());
        dto.setType(job.getType());
        dto.setAction(job.getAction());
        dto.setUserName(job.getUserName());
        dto.setLugCompleted(job.getLugCompleted());

        dto.setExportParams(job.getExportParams());
        dto.setImportParams(job.getImportParams());

        if (CollectionUtils.isNotEmpty(job.getOperators())) {
            dto.setOperators(new HashSet<>(job.getOperators()));
        }

        return dto;
    }
}
