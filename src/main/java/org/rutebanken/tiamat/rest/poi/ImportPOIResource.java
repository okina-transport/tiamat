package org.rutebanken.tiamat.rest.poi;



import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.rutebanken.tiamat.general.PointOfInterestCSVHelper;
import org.rutebanken.tiamat.model.PointOfInterest;
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
@Path("poi")
public class ImportPOIResource {

    private static final Logger logger = LoggerFactory.getLogger(ImportPOIResource.class);

    @Autowired
    PointOfInterestCSVHelper poiHelper;



    @POST
    @Path("/poi_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importPOIFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {

        logger.info("Import POI par " + user + " du fichier " + fileName);
        poiHelper.clearClassificationCache();

        List<DtoPointOfInterest> dtoPointOfInterest = poiHelper.parseDocument(inputStream);
        PointOfInterestCSVHelper.checkDuplicatedPois(dtoPointOfInterest);
        List<DtoPointOfInterest> poiWithClassification = filterPoisWithClassification(dtoPointOfInterest);

        //poiHelper.clearPOIExceptShop();

        try {
            poiHelper.persistPointsOfInterest(poiWithClassification);
        }catch(Exception e){
            logger.error(e.getMessage(),e);
        }

        logger.info("Import POI par " + user + " du fichier " + fileName + " terminé");

        return Response.status(200).build();
    }

    @POST
    @Path("/shop_import_csv")
    @Consumes({MediaType.MULTIPART_FORM_DATA + "; charset=UTF-8"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response importShopCsvFile(@FormDataParam("file") InputStream inputStream, @FormDataParam("file_name") String fileName, @FormDataParam("user") String user) throws IOException, IllegalArgumentException {

        logger.info("Import points de vente par " + user + " du fichier " + fileName);

        List<DtoPointOfInterest> dtoPointOfInterest = poiHelper.parseDocument(inputStream);
        PointOfInterestCSVHelper.checkDuplicatedPois(dtoPointOfInterest);
        checkShops(dtoPointOfInterest);

        //poiHelper.clearPOIForShopClassification();

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

    private List<DtoPointOfInterest> filterPoisWithClassification(List<DtoPointOfInterest> dtoPointOfInterest){
        return dtoPointOfInterest.stream()
                .filter(poi -> StringUtils.isNotEmpty(poi.getAmenity()) ||  StringUtils.isNotEmpty(poi.getBuilding()) || StringUtils.isNotEmpty(poi.getHistoric())
                                                ||  StringUtils.isNotEmpty(poi.getLanduse()) ||  StringUtils.isNotEmpty(poi.getLeisure())
                                                ||  StringUtils.isNotEmpty(poi.getTourism()) ||StringUtils.isNotEmpty(poi.getOffice()))
                .collect(Collectors.toList());



    }



}
