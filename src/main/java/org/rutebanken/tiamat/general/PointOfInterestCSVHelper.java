package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.externalapis.ApiProxyService;
import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.PointOfInterestClassification;
import org.rutebanken.tiamat.model.PointOfInterestFacilitySet;
import org.rutebanken.tiamat.model.TicketingFacilityEnumeration;
import org.rutebanken.tiamat.model.TicketingServiceFacilityEnumeration;
import org.rutebanken.tiamat.repository.PointOfInterestClassificationRepository;
import org.rutebanken.tiamat.repository.PointOfInterestFacilitySetRepository;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.rest.dto.DtoPointOfInterest;
import org.rutebanken.tiamat.service.Preconditions;
import org.rutebanken.tiamat.versioning.save.PointOfInterestClassificationVersionedSaverService;
import org.rutebanken.tiamat.versioning.save.PointOfInterestVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

@Component
public class PointOfInterestCSVHelper {

    public static final Logger logger = LoggerFactory.getLogger(PointOfInterestCSVHelper.class);
    public final static String SHOP_CLASSIFICATION_NAME = "shop";
    public final static String AMENITY_CLASSIFICATION_NAME = "amenity";
    public final static String BUILDING_CLASSIFICATION_NAME = "building";
    public final static String HISTORIC_CLASSIFICATION_NAME = "historic";
    public final static String LANDUSE_CLASSIFICATION_NAME = "landuse";
    public final static String LEISURE_CLASSIFICATION_NAME = "leisure";
    public final static String TOURISM_CLASSIFICATION_NAME = "tourism";
    public final static String OFFICE_CLASSIFICATION_NAME = "office";

    private final static Pattern patternXlongYlat = Pattern.compile("^-?([0-9]*)\\.{1}\\d{1,20}");

    private static GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();

    @Autowired
    private PointOfInterestClassificationRepository poiClassificationRepo;

    @Autowired
    private PointOfInterestRepository pointOfInterestRepo;

    @Autowired
    private PointOfInterestFacilitySetRepository facilitySetRepo;

    @Autowired
    private UsernameFetcher usernameFetcher;


    @Autowired
    private PointOfInterestVersionedSaverService poiVersionedSaverService;

    @Autowired
    private PointOfInterestClassificationVersionedSaverService poiClassVersionedSaverService;


    //Cache to store parent classifications
    private Map<String, PointOfInterestClassification> parentClassificationCache = new HashMap<>();

    //Cache to store child classifications
    private Map<String, Map<String, PointOfInterestClassification>> childClassificationCache = new HashMap<>();


    private ApiProxyService apiProxyService = new ApiProxyService();


    /**
     * Parse the CSV file and converts each line to a DTO object
     *
     * @param csvFile
     *  input file uploaded by user
     * @return
     *  a list of DTO object
     * @throws IllegalArgumentException
     * exception in case of error in the data
     */
    public List<DtoPointOfInterest> parseDocument(InputStream csvFile) throws IllegalArgumentException, IOException {
        Iterable<CSVRecord> records = CSVHelper.getRecords(csvFile);

        return StreamSupport.stream(records.spliterator(), false)
                                                .map(this::convertToDTO)
                                            .collect(Collectors.toList());

    }


    /**
     * Converts a raw string from CSV to a DTO object
     * @param rawString
     *  line from CSV file
     * @return
     *  a DTO object with data from the CSV file
     */
    private DtoPointOfInterest convertToDTO(CSVRecord rawString){

        DtoPointOfInterest poiDTO = new DtoPointOfInterest(rawString);
        validatePoi(poiDTO);
        return poiDTO;
    }

    /**
     * Checks if Point of interest matches the conditions
     *
     * @param poi
     *  The point of interest to check
     * @throws IllegalArgumentException
     *  Exception if the point of interest does not meet the required conditions
     */
    private static void validatePoi(DtoPointOfInterest poi) throws IllegalArgumentException{
        Preconditions.checkArgument(!poi.getId().isEmpty(),"ID is required in all POI" );
        Preconditions.checkArgument(!poi.getName().isEmpty() ,"NAME is required for POI :" + poi.getId());
        Preconditions.checkArgument(patternXlongYlat.matcher(poi.getLongitude()).matches(),"longitude is required for POI: " + poi.getId());
        Preconditions.checkArgument(patternXlongYlat.matcher(poi.getLatitude()).matches(),"latitude is required for POI: " + poi.getId());
    }


    /**
     * Removes duplicate Point of interest from the input
     * @param pointOfInterestList
     * @throws IllegalArgumentException
     */
    public static void checkDuplicatedPois(List<DtoPointOfInterest> pointOfInterestList) throws IllegalArgumentException{
        List <String> compositeKey = pointOfInterestList.stream()
                                                         .map(poi -> poi.getId()+poi.getName())
                                                         .collect(Collectors.toList());

        Set listWithoutDuplicatedValues = new HashSet(compositeKey);

        if(compositeKey.size() > listWithoutDuplicatedValues.size())
                throw new IllegalArgumentException("There are duplicated POI in your CSV File (With the same ID & Name)");
    }


    /**
     * Removes PointOfInterest from database for shop classification
     *
     */
    public void clearPOIForShopClassification(){
        pointOfInterestRepo.clearPOIForClassification(SHOP_CLASSIFICATION_NAME);
    }

    /**
     * Removes PointOfInterest from database for all classifications EXCEPT shop
     *
     */
    public void clearPOIExceptShop(){
        pointOfInterestRepo.clearPOIExceptClassification(SHOP_CLASSIFICATION_NAME);
    }


    /**
     * Persist a list of POI into database
     * @param dtoPoiList
     */
    public void persistPointsOfInterest(List<DtoPointOfInterest> dtoPoiList){

        List<PointOfInterest> poiToPersist = dtoPoiList.stream()
                                                        .map(this::mapFromDtoToEntity)
                                                        .collect(Collectors.toList());

        logger.info("Mapping completed. Starting to persist POIs");

        for (PointOfInterest pointOfInterest : poiToPersist) {
            poiVersionedSaverService.saveNewVersion(pointOfInterest);
        }

    }


    /**
     * Converts a DTO from the CSV read
     * @param dtoPoiCSV
     *     the DTO filled with data from the CSV
     * @return
     *     the entity PointOfInterest
     */
    public PointOfInterest mapFromDtoToEntity(DtoPointOfInterest dtoPoiCSV){

        PointOfInterest newPointOfInterest = new PointOfInterest();
        newPointOfInterest.getOrCreateValues(ORIGINAL_ID_KEY).add(dtoPoiCSV.getId());
        newPointOfInterest.setName(new EmbeddableMultilingualString(dtoPoiCSV.getName(),"FR"));
        newPointOfInterest.setCentroid(geometryFactory.createPoint(new Coordinate(Double.valueOf(dtoPoiCSV.getLongitude()), Double.valueOf(dtoPoiCSV.getLatitude()))));
        try {
            DtoGeocode geocodeData = apiProxyService.getGeocodeDataByReverseGeocoding(new BigDecimal(dtoPoiCSV.getLatitude(), MathContext.DECIMAL64), new BigDecimal(dtoPoiCSV.getLongitude(), MathContext.DECIMAL64));
            newPointOfInterest.setZipCode(geocodeData.getCityCode());
            newPointOfInterest.setPostalCode(StringUtils.isEmpty(dtoPoiCSV.getPostCode()) ? geocodeData.getPostCode() : dtoPoiCSV.getPostCode());
        } catch (Exception e) {
            logger.error("Unable to get zip code for poi:" + dtoPoiCSV.getId());
        }


        PointOfInterestClassification classification = getChildClassification(dtoPoiCSV);
        newPointOfInterest.getClassifications().add(classification);


        if (StringUtils.isNotEmpty(dtoPoiCSV.getShop())){
            PointOfInterestFacilitySet facilitySet = createFacilitySetForShopImport();
            newPointOfInterest.setPointOfInterestFacilitySet(facilitySet);
        }

        if (dtoPoiCSV.getTags().size() > 0){
            for (Map.Entry<String, String> tagEntry : dtoPoiCSV.getTags().entrySet()) {
                newPointOfInterest.getOrCreateValues(tagEntry.getKey()).add(tagEntry.getValue());
            }
        }


        return newPointOfInterest;
    }


    /**
     * Recovers the parent classification of the POI
     *
     * @param dtoPoiCSV
     *  POI comming from input file
     * @return
     *  The parent classification
     */
    private PointOfInterestClassification getParentClassification(DtoPointOfInterest dtoPoiCSV){


        if (StringUtils.isNotEmpty(dtoPoiCSV.getShop())){
            return getOrCreateParentClassification(SHOP_CLASSIFICATION_NAME);
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getAmenity())){
            return getOrCreateParentClassification(AMENITY_CLASSIFICATION_NAME);
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getBuilding())){
            return getOrCreateParentClassification(BUILDING_CLASSIFICATION_NAME);
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getHistoric())){
            return getOrCreateParentClassification(HISTORIC_CLASSIFICATION_NAME);
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getLanduse())){
            return getOrCreateParentClassification(LANDUSE_CLASSIFICATION_NAME);
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getLeisure())){
            return getOrCreateParentClassification(LEISURE_CLASSIFICATION_NAME);
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getTourism())){
            return getOrCreateParentClassification(TOURISM_CLASSIFICATION_NAME);
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getOffice())){
            return getOrCreateParentClassification(OFFICE_CLASSIFICATION_NAME);
        }

        throw new IllegalArgumentException("Unable to find classification for id :" + dtoPoiCSV.getId());
    }


    /**
     * Create a facility set for SHOP import
     * @return
     *  the new facility set
     */
    private PointOfInterestFacilitySet createFacilitySetForShopImport(){
        PointOfInterestFacilitySet newFacilitySet = new PointOfInterestFacilitySet();
        newFacilitySet.setTicketingServiceFacility( TicketingServiceFacilityEnumeration.PURCHASE);
        newFacilitySet.setTicketingFacility(TicketingFacilityEnumeration.TICKET_MACHINES);
        newFacilitySet.setVersion(1);
        facilitySetRepo.save(newFacilitySet);
        return newFacilitySet;
    }


    /**
     * Recovers a child classification  from cache or creates it
     * @param pointOfInterest
     *
     * @return
     *      The classification
     */
    private PointOfInterestClassification getChildClassification(DtoPointOfInterest pointOfInterest){

        PointOfInterestClassification parentClassification = getParentClassification(pointOfInterest);
        String parentName = parentClassification.getName().getValue();

        if (!childClassificationCache.containsKey(parentName)){
            Map<String, PointOfInterestClassification>  newMap = new HashMap<>();
            childClassificationCache.put(parentName, newMap);
        }

        Map<String, PointOfInterestClassification> childrenClassificationMap = childClassificationCache.get(parentName);
        String childClassificationName = getChildClassificationName(pointOfInterest);

        if (childrenClassificationMap.containsKey(childClassificationName)){
            return childrenClassificationMap.get(childClassificationName);
        }


        Optional<String> netexClassOpt = poiClassificationRepo.getClassification(childClassificationName, parentClassification.getId());
        PointOfInterestClassification childClassification;

        if (netexClassOpt.isPresent()){
            childClassification =  poiClassificationRepo.findFirstByNetexIdOrderByVersionDesc(netexClassOpt.get());

        }else{

            PointOfInterestClassification newClass = new PointOfInterestClassification();
            newClass.setName(new EmbeddableMultilingualString(childClassificationName,"FR"));
            newClass.setChangedBy(usernameFetcher.getUserNameForAuthenticatedUser());
            newClass.setParent(parentClassification);
            childClassification =  poiClassVersionedSaverService.saveNewVersion(newClass);
            childClassification = poiClassificationRepo.findFirstByNetexIdOrderByVersionDesc(childClassification.getNetexId());
        }
        childrenClassificationMap.put(childClassificationName, childClassification);
        return childClassification;
    }

    /**
     * Determines the classification name from the POI read in input file
     * @param dtoPoiCSV
     *      POI from the input file
     * @return
     *      the classification name
     */
    private String getChildClassificationName(DtoPointOfInterest dtoPoiCSV){

        if (StringUtils.isNotEmpty(dtoPoiCSV.getShop())){
            return dtoPoiCSV.getShop();
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getAmenity())){
            return dtoPoiCSV.getAmenity();
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getBuilding())){
            return dtoPoiCSV.getBuilding();
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getHistoric())){
            return dtoPoiCSV.getHistoric();
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getLanduse())){
            return dtoPoiCSV.getLanduse();
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getLeisure())){
            return dtoPoiCSV.getLeisure();
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getTourism())){
            return dtoPoiCSV.getTourism();
        }else if (StringUtils.isNotEmpty(dtoPoiCSV.getOffice())){
            return dtoPoiCSV.getOffice();
        }
        throw new IllegalArgumentException("Unable to find classification for id :" + dtoPoiCSV.getId());
    }


    /**
     * Recovers a parent classification from cache or creates it
     * (a parent classification is a classification with no other classification above. The root level of classification)
     * @param parentClassificationName
     *         The parent classification name to recover
     * @return
     *         The classification
     */
    private PointOfInterestClassification getOrCreateParentClassification(String parentClassificationName){

        if (parentClassificationCache.containsKey(parentClassificationName)){
            return parentClassificationCache.get(parentClassificationName);
        }

        Optional<String> netexClassOpt = poiClassificationRepo.getClassification(parentClassificationName, null);
        PointOfInterestClassification parentClassification;


        if (netexClassOpt.isPresent()){
             parentClassification = poiClassificationRepo.findFirstByNetexIdOrderByVersionDesc(netexClassOpt.get());
        }else{
            PointOfInterestClassification newClass = new PointOfInterestClassification();
            newClass.setName(new EmbeddableMultilingualString(parentClassificationName,"FR"));
            newClass.setChangedBy(usernameFetcher.getUserNameForAuthenticatedUser());
            parentClassification = poiClassVersionedSaverService.saveNewVersion(newClass);
            parentClassification = poiClassificationRepo.findFirstByNetexIdOrderByVersionDesc(parentClassification.getNetexId());
        }
        parentClassificationCache.put(parentClassificationName, parentClassification);
        return parentClassification;

    }









}
