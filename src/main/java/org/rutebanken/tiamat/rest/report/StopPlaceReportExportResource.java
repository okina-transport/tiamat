package org.rutebanken.tiamat.rest.report;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.rutebanken.tiamat.service.stopplace.report.StopPlaceReportCsvExporter;
import org.rutebanken.tiamat.service.stopplace.report.StopPlaceReportExportRequest;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@Path("/report")
public class StopPlaceReportExportResource {

    private static final String CSV_MEDIA_TYPE = "text/csv; charset=UTF-8";

    private final StopPlaceReportCsvExporter stopPlaceReportCsvExporter;

    public StopPlaceReportExportResource(StopPlaceReportCsvExporter stopPlaceReportCsvExporter) {
        this.stopPlaceReportCsvExporter = stopPlaceReportCsvExporter;
    }

    @POST
    @Path("/csv")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(CSV_MEDIA_TYPE)
    public Response exportCsv(StopPlaceReportExportRequest request) {
        if (request == null) {
            throw new BadRequestException("Missing export request");
        }
        String csv;
        try {
            csv = stopPlaceReportCsvExporter.export(request);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        return Response.ok(csv.getBytes(StandardCharsets.UTF_8), CSV_MEDIA_TYPE).build();
    }
}
