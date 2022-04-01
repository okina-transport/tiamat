package org.rutebanken.tiamat.rest.poi;



import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.general.PointOfInterestCSVHelper;
import org.rutebanken.tiamat.rest.dto.DtoPointOfInterest;
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
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/shop_import_csv")
public class ImportShopResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportShopResource.class);

    @Autowired
    PointOfInterestCSVHelper poiHelper;



    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importShopCsvFile(@FormDataParam("file") InputStream inputStream) throws IOException, IllegalArgumentException {


        String csvFile = null;

        csvFile = new String(ByteStreams.toByteArray(inputStream));
        List<DtoPointOfInterest> dtoPointOfInterest = poiHelper.parseDocument(csvFile);
        PointOfInterestCSVHelper.checkDuplicatedPois(dtoPointOfInterest);
        checkShops(dtoPointOfInterest);

        poiHelper.clearAllPois();

        try {
            poiHelper.persistPointsOfInterest(dtoPointOfInterest);
        }catch(Exception e){
            logger.error(e.getMessage(),e);
        }

        return Response.status(200).build();
    }


    private void checkShops( List<DtoPointOfInterest> dtoPointOfInterest){
        List<DtoPointOfInterest> nonShopPOI = dtoPointOfInterest.stream()
                                                                .filter(poi -> StringUtils.isEmpty(poi.getShop()))
                                                                .collect(Collectors.toList());

        String nonShopString = nonShopPOI.stream()
                                        .map(DtoPointOfInterest::getId)
                                        .collect(Collectors.joining(","));

        if (!nonShopPOI.isEmpty()){
            String errorMsg = "Non shops POIs have been found in shop import:" + nonShopString;
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

    }



}
