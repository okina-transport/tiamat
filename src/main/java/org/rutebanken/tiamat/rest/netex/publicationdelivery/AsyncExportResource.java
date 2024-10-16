/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.rest.netex.publicationdelivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.rutebanken.tiamat.exporter.AsyncPublicationDeliveryExporter;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.repository.JobSpecification;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.mapper.NetexExportSummaryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import static org.rutebanken.tiamat.repository.JobSpecification.jobActionFilter;
import static org.rutebanken.tiamat.repository.JobSpecification.jobTypeFilter;
import static org.rutebanken.tiamat.rest.netex.publicationdelivery.AsyncExportResource.ASYNC_EXPORT_JOB_PATH;

/**
 * Export publication delivery data to google cloud storage. Some parts like stops and parking asynchronously
 */
@Api(tags = {"Async export resource"}, produces = "application/xml")
@Produces(MediaType.APPLICATION_XML + "; charset=UTF-8")
@Path("/netex/" + ASYNC_EXPORT_JOB_PATH)
public class AsyncExportResource {

    private static final Logger logger = LoggerFactory.getLogger(AsyncExportResource.class);

    public static final String ASYNC_EXPORT_JOB_PATH = "export";

    private final AsyncPublicationDeliveryExporter asyncPublicationDeliveryExporter;

    private final JobRepository jobRepository;

    private final NetexExportSummaryMapper netexExportSummaryMapper;

    @Autowired
    public AsyncExportResource(AsyncPublicationDeliveryExporter asyncPublicationDeliveryExporter, JobRepository jobRepository, NetexExportSummaryMapper netexExportSummaryMapper) {
        this.asyncPublicationDeliveryExporter = asyncPublicationDeliveryExporter;
        this.jobRepository = jobRepository;
        this.netexExportSummaryMapper = netexExportSummaryMapper;
    }

    @GET
    public Collection<Job> getAsyncExportJobs() {
        return asyncPublicationDeliveryExporter.getJobs();
    }

    @GET
    @Path("{id}/status")
    public Response getAsyncExportJob(@PathParam(value = "id") long exportJobId) {

        Job job = asyncPublicationDeliveryExporter.getExportJob(exportJobId);

        if (job == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        logger.info("Returning job {}", job);
        return Response.ok(job).build();
    }

    @GET
    @Path("{id}/content")
    public Response getAsyncExportJobContents(@PathParam(value = "id") long exportJobId) {

        Job job = asyncPublicationDeliveryExporter.getExportJob(exportJobId);

        if (job == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        logger.info("Returning result of job {}", job);
        if (!job.getStatus().equals(JobStatus.FINISHED)) {
            return Response.accepted("Job status is not FINISHED for job: " + job).build();
        }

        InputStream inputStream = asyncPublicationDeliveryExporter.getJobFileContent(job);
        return Response.ok(inputStream).build();
    }

    @GET
    @Path("initiate")
    public Response asyncExport(@HeaderParam("RutebankenUser") String username, @BeanParam ExportParams exportParams) {
        Job job = asyncPublicationDeliveryExporter.startExportJob(username, exportParams);
        return Response.ok(job).build();
    }

    @GET
    @Path("poi")
    public Response asyncPOIExport(@BeanParam ExportParams exportParams) {
        Job job = asyncPublicationDeliveryExporter.startPOIExportJob(exportParams);
        return Response.ok(job).build();
    }

    @GET
    @Path("parkings")
    public Response asyncParkingsExport(@BeanParam ExportParams exportParams) {
        Job job = asyncPublicationDeliveryExporter.startParkingsExportJob(exportParams);
        return Response.ok(job).build();
    }


    @GET
    @Path("stop-place-file-list-by-provider-name/{providerName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response asyncGetSopPlaceFileList(@PathParam(value = "providerName") String providerName, @HeaderParam("maxNbResults") Integer maxNbResults) {
        List<String> stopPlaceFileList = asyncPublicationDeliveryExporter.getStopPlaceFileListByProviderName(providerName,maxNbResults);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString="";
        try {
            jsonString = objectMapper.writeValueAsString(stopPlaceFileList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return Response.ok(jsonString).build();
    }

    @GET
    @Path("stop-place-summary/{providerName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllStopPlaceExports(@PathParam("providerName") String providerName, @HeaderParam("maxNbResults") Integer maxNbResults) {
        try {
            Specification<Job> jobActionFilter = jobActionFilter(JobAction.EXPORT);
            Specification<Job> jobTypeFilter = jobTypeFilter(JobType.NETEX_STOP_PLACE_QUAY);
            Specification<Job> providerFilter = JobSpecification.providerFilter(providerName);

            Specification<Job> combinedFilter = Specification.where(jobActionFilter)
                    .and(jobTypeFilter)
                    .and(providerFilter);

            Pageable pageable = PageRequest.of(0, maxNbResults, Sort.Direction.DESC, "started");
            Page<Job> foundJobs = jobRepository.findAll(combinedFilter, pageable);
            return Response.ok(netexExportSummaryMapper.mapJobToExportSummary(foundJobs.toList())).build();
        } catch(Exception e) {
            logger.error("Error getting netex stop exports for provider {}", providerName, e);
        }

        return Response.status(500).build();
    }

    @GET
    @Path("poi-file-list-by-provider-name/{providerName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response asyncGetPOIFileList(@PathParam(value = "providerName") String providerName, @HeaderParam("maxNbResults") Integer maxNbResults) {
        List<String> pointsOfInterestFileList = asyncPublicationDeliveryExporter.getPointsOfInterestFileListByProviderName(providerName,maxNbResults);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString="";
        try {
            jsonString = objectMapper.writeValueAsString(pointsOfInterestFileList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return Response.ok(jsonString).build();
    }

    @GET
    @Path("parkings-file-list-by-provider-name/{providerName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response asyncGetParkingsFileList(@PathParam(value = "providerName") String providerName, @HeaderParam("maxNbResults") Integer maxNbResults) {
        List<String> parkingsOfInterestFileList = asyncPublicationDeliveryExporter.getParkingsFileListByProviderName(providerName,maxNbResults);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString="";
        try {
            jsonString = objectMapper.writeValueAsString(parkingsOfInterestFileList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return Response.ok(jsonString).build();
    }

    @GET
    @Path("stop-place-file-download/{providerName}/{fileName : .+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response asyncGetSopPlaceFileList(@PathParam("providerName") String providerName, @PathParam("fileName") String fileName) {
        File file = asyncPublicationDeliveryExporter.getJobFileContent(providerName,fileName);
        return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                .header("filename", file.getName() )
                .build();
    }
}
