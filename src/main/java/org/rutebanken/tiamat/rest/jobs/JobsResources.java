package org.rutebanken.tiamat.rest.jobs;

import com.amazonaws.services.directory.model.ServiceException;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobInfo;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.Link;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.service.job.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
@Path("/jobs")
public class JobsResources {

    private static final Logger logger = LoggerFactory.getLogger(JobsResources.class);

    private final JobService jobService;

    @Context
    UriInfo uriInfo;
    private final JobRepository jobRepository;

    public JobsResources(JobService jobService,
                         JobRepository jobRepository){
        this.jobService = jobService;
        this.jobRepository = jobRepository;
    }

    @GET
    @Path("/{ref}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response jobs(@PathParam("ref") String referential,
                         @QueryParam("action") final List<String> actions,
                         @QueryParam("status") final JobStatus status) {

        try {
            String refDescription = referential == null ? "all referentials" : "referential = " + referential;
            logger.info(Color.CYAN + "Call jobs = " + refDescription + ", actions = " + actions + ", status = " + status);

            // create jobs listing
            List<JobInfo> result = new ArrayList<>();

            {
                List<Job> jobs = jobService.jobs(referential, actions, status);
                for (Job job : jobs) {
                    JobInfo jobInfo = new JobInfo(job, true, uriInfo);
                    result.add(jobInfo);
                }
                jobs.clear();
            }
            // cache control
            Response.ResponseBuilder builder = Response.ok(result);
            return builder.build();
        } catch (ServiceException e) {
            logger.error("Code = " + e.getErrorCode() + ", Message = " + e.getMessage());
            throw new ServiceException("error" + e);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new WebApplicationException("INTERNAL_ERROR", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/{ref}/scheduled_jobs/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response scheduledJob(@PathParam("ref") String referential, @PathParam("id") Long id) {
        try {
            logger.debug(Color.CYAN + "Call scheduledJob referential = " + referential + ", id = " + id);

            Response result = null;
            Response.ResponseBuilder builder = null;

            {
                Job job = jobService.scheduledJob(referential, id);

                // build response
                if (job.getStatus().ordinal() <= JobInfo.STATUS.STARTED.ordinal()) {
                    JobInfo info = new JobInfo(job, true, uriInfo);
                    builder = Response.ok(info);
                } else {
                    builder = Response.seeOther(URI.create("/terminated_jobs/" + job.getId()));
                }

                // add links
                for (Link link : job.getLinks()) {
                    URI uri = URI.create(uriInfo.getBaseUri() + link.getHref());
                    builder.link(URI.create(uri.toASCIIString()), link.getRel());
                }
            }

            result = builder.build();
            return result;

        } catch (ServiceException e) {
            throw new ServiceException("Code = " + e.getErrorCode() + ", Message = " + e.getMessage());
        } catch (Exception ex) {
            throw new WebApplicationException("INTERNAL_ERROR", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/{ref}/terminated_jobs/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response terminatedJob(@PathParam("ref") String referential, @PathParam("id") Long id) {
        try {
            logger.info(Color.CYAN + "Call terminatedJob referential = " + referential + ", id = " + id);

            Response.ResponseBuilder builder = null;
            {
                Job job = jobRepository.terminatedJob(referential, id);

                JobInfo info = new JobInfo(job, true, uriInfo);
                builder = Response.ok(info);

                // cache control
                CacheControl cc = new CacheControl();
                cc.setMaxAge(Integer.MAX_VALUE);
                builder.cacheControl(cc);

                // add links
                for (Link link : job.getLinks()) {
                    URI uri = URI.create(uriInfo.getBaseUri() + link.getHref());
                    builder.link(URI.create(uri.toASCIIString()), link.getRel());
                }
            }

            return builder.build();

        } catch (ServiceException ex) {
            throw new ServiceException("Code = " + ex.getErrorCode() + ", Message = " + ex.getMessage());
        } catch (Exception ex) {
            throw new WebApplicationException("INTERNAL_ERROR", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
