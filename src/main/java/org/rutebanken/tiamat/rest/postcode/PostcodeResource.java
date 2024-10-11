package org.rutebanken.tiamat.rest.postcode;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.service.batch.MissingPostCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Transactional
@Path("/get_missing_postcode")
public class PostcodeResource {

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    @Autowired
    private MissingPostCodeService missingPostCodeService;

    @Autowired
    JobRepository jobRepository;

    @POST
    @PreAuthorize("@rolesChecker.hasRoleEdit()")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMissingPostcode() throws IllegalArgumentException, IOException {
        Job job = new Job();
        job.setType(JobType.MISSING_POSTAL_CODE);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);

        ImportJobWorker importJobWorker = new ImportJobWorker(job, jobRepository);
        importJobWorker.setMissingPostalCodeService(missingPostCodeService);
        importService.submit(importJobWorker);

        return Response.status(200).build();
    }
}
