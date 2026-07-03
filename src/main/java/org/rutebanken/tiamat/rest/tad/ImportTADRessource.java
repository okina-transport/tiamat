package org.rutebanken.tiamat.rest.tad;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.changelog.LoggingService;
import org.rutebanken.tiamat.general.TadCSVHelper;
import org.rutebanken.tiamat.rest.dto.DtoTadStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@Path("tad")
public class ImportTADRessource {

    private static final Logger logger = LoggerFactory.getLogger(ImportTADRessource.class);

    @Autowired
    private TadCSVHelper tadHelper;

    @Autowired
    private LoggingService loggingService;

    @POST
    @Path("/tad_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importTADFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {

        logger.info("Import TAD par {} du fichier {}", user, fileName);
        loggingService.logTadImport(user, fileName);

        List<DtoTadStop> dtoTads = tadHelper.parseDocument(inputStream);
        tadHelper.checkDuplicatedTads(dtoTads);

        try {
            tadHelper.persistTad(dtoTads);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Response.status(500).build();
        }


        return Response.status(200).build();
    }

}
