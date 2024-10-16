package org.rutebanken.tiamat.rest.netex.publicationdelivery.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.DtoNetexExportSummary;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NetexExportSummaryMapperTest {

    private static final String USERNAME = "toto";
    private static final String USERNAME2 = "toto2";
    private static final String FILENAME = "/home/tmp";
    private static final String FILENAME2= "/home/tmp2";
    private static final Instant DATE_CREATION = Instant.now();

    @InjectMocks
    private NetexExportSummaryMapper mapper;

    @Test
    void mapJobToExportSummary_jobUserName_test() {
        Job input = buildJob();

        DtoNetexExportSummary exportSummary = mapper.mapJobToExportSummary(input);

        assertThat(exportSummary).isNotNull();
        assertThat(exportSummary.getUserName()).isEqualTo(input.getUserName());
    }

    @Test
    void mapJobToExportSummary_jobFileName_test() {
        Job input = buildJob();

        DtoNetexExportSummary exportSummary = mapper.mapJobToExportSummary(input);

        assertThat(exportSummary).isNotNull();
        assertThat(exportSummary.getFileName()).isEqualTo(input.getFileName());
    }

    @Test
    void mapJobToExportSummary_jobcreationDate_test() {
        Job input = buildJob();

        DtoNetexExportSummary exportSummary = mapper.mapJobToExportSummary(input);

        assertThat(exportSummary).isNotNull();
        assertThat(exportSummary.getCreationDate()).isEqualTo(input.getStarted());
    }

    private Job buildJob() {
        Job job = new Job();
        job.setUserName(USERNAME);
        job.setFileName(FILENAME);
        job.setStarted(DATE_CREATION);
        return job;
    }

    @Test
    void mapJobToExportSummary_shoudFilterNullObject_test() {
        Job input = buildJob();
        Job input2 = buildJob();
        input2.setUserName(USERNAME2);
        input2.setFileName(FILENAME2);
        Job input3 = null;
        List<Job> jobs = Arrays.asList(input, input2, input3);

        List<DtoNetexExportSummary> result = mapper.mapJobToExportSummary(jobs);

        assertThat(result).isNotNull().isNotEmpty().hasSize(2);
        assertThat(result).extracting("userName").containsExactlyInAnyOrder(input.getUserName(), input2.getUserName());
        assertThat(result).extracting("fileName").containsExactlyInAnyOrder(input.getFileName(), input2.getFileName());
        assertThat(result).extracting("creationDate").containsExactlyInAnyOrder(input.getStarted(), input2.getStarted());
    }


}