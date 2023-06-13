package org.rutebanken.tiamat.rest.tad;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.general.PointOfInterestCSVHelper;
import org.rutebanken.tiamat.general.TadCSVHelper;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Point;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.rest.dto.DtoPointOfInterest;
import org.rutebanken.tiamat.rest.dto.DtoTadStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Path("tad")
public class ImportTADRessource {

    private static final Logger logger = LoggerFactory.getLogger(ImportTADRessource.class);

    @Autowired
    TadCSVHelper tadHelper;

    @POST
    @Path("/tad_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importTADFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {

        logger.info("Import TAD par " + user + " du fichier " + fileName);

        List<DtoTadStop> dtoTads = tadHelper.parseDocument(inputStream);
        tadHelper.checkDuplicatedTads(dtoTads);

        try {
            tadHelper.persistTad(dtoTads);
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            throw e;
        }

        return Response.status(200).build();
    }

}
