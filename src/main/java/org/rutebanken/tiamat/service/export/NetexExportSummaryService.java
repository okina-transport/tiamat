package org.rutebanken.tiamat.service.export;

import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.repository.JobSpecification;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.DtoNetexExportSummary;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.mapper.NetexExportSummaryMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.rutebanken.tiamat.repository.JobSpecification.jobActionFilter;
import static org.rutebanken.tiamat.repository.JobSpecification.jobTypeFilter;

@Service
public class NetexExportSummaryService {

    private final JobRepository jobRepository;

    private final NetexExportSummaryMapper netexExportSummaryMapper;

    public NetexExportSummaryService(JobRepository jobRepository,
                                     NetexExportSummaryMapper netexExportSummaryMapper) {
        this.jobRepository = jobRepository;
        this.netexExportSummaryMapper = netexExportSummaryMapper;
    }

    public List<DtoNetexExportSummary> getNetexStopExportSummary(String providerName, int size) {
        Specification<Job> jobActionFilter = jobActionFilter(JobAction.EXPORT);
        Specification<Job> jobTypeFilter = jobTypeFilter(JobType.NETEX_STOP_PLACE_QUAY);
        Specification<Job> providerFilter = JobSpecification.providerFilter(providerName);
        Specification<Job> postProcessFilter = JobSpecification.postProcessFilter();

        Specification<Job> combinedFilter = Specification.allOf(jobActionFilter,
                jobTypeFilter,
                providerFilter,
                postProcessFilter);

        Pageable pageable = PageRequest.of(0, size, Sort.by("started"));
        List<Job> jobs = jobRepository.findAll(combinedFilter, pageable).getContent();
        List<Job> jobsWithOperators = jobRepository.findJobsWithOperatorsFetching(
                jobs.stream().map(Job::getId).toList()
        );
        return netexExportSummaryMapper.mapJobToExportSummary(jobsWithOperators);
    }
}
