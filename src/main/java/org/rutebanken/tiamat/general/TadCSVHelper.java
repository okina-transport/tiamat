package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.externalapis.ApiProxyService;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.rest.dto.DtoTadStop;
import org.rutebanken.tiamat.service.Preconditions;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

@Component
public class TadCSVHelper {

    public static final Logger logger = LoggerFactory.getLogger(TadCSVHelper.class);

    @Autowired
    StopPlaceVersionedSaverService sopStopPlaceVersionedSaverService;

    @Autowired
    StopPlaceRepository stopPlaceRepository;

    @Autowired
    private VersionCreator versionCreator;

    private static GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();

    private ApiProxyService apiProxyService = new ApiProxyService();

    private final static Pattern patternXlongYlat = Pattern.compile("^-?([0-9]*)\\.{1}\\d{1,20}");

    /**
     * Parse the CSV file and converts each line to a DTO object
     *
     * @param csvFile input file uploaded by user
     * @return a list of DTO object
     * @throws IllegalArgumentException exception in case of error in the data
     */
    public List<DtoTadStop> parseDocument(InputStream csvFile) throws IllegalArgumentException, IOException {
        Iterable<CSVRecord> records = CSVHelper.getRecords(csvFile);

        return StreamSupport.stream(records.spliterator(), false)
                .map(this::convertToDTO)
                .collect(Collectors.toList());

    }


    /**
     * Converts a raw string from CSV to a DTO object
     *
     * @param rawString line from CSV file
     * @return a DTO object with data from the CSV file
     */
    private DtoTadStop convertToDTO(CSVRecord rawString) {

        DtoTadStop tadDTO = new DtoTadStop(rawString);
        validateTad(tadDTO);
        return tadDTO;
    }

    private void validateTad(DtoTadStop tadDTO) {
        Preconditions.checkArgument(!tadDTO.getStopId().isEmpty(), "ID is required in all TAD");
        Preconditions.checkArgument(!tadDTO.getStopName().isEmpty(), "NAME is required for TAD :" + tadDTO.getStopId());
        Preconditions.checkArgument(patternXlongYlat.matcher(tadDTO.getStopLat()).matches(), "latitude is required for TAD: " + tadDTO.getStopId());
        Preconditions.checkArgument(patternXlongYlat.matcher(tadDTO.getStopLon()).matches(), "longitude is required for TAD: " + tadDTO.getStopId());
    }

    /**
     * Removes duplicate Point of interest from the input
     *
     * @param tadList
     * @throws IllegalArgumentException
     */
    public static void checkDuplicatedTads(List<DtoTadStop> tadList) throws IllegalArgumentException {
        List<String> compositeKey = tadList.stream()
                .map(poi -> poi.getStopId() + poi.getStopName())
                .collect(Collectors.toList());

        Set listWithoutDuplicatedValues = new HashSet(compositeKey);

        if (compositeKey.size() > listWithoutDuplicatedValues.size())
            throw new IllegalArgumentException("There are duplicated TAD in your CSV File (With the same ID & Name)");
    }

    /**
     * Persist a list of TAD into database
     *
     * @param tadToPersist
     */
    public void persistTad(List<DtoTadStop> tadToPersist) throws IllegalArgumentException{

        List<StopPlace> stopsToPersist = new ArrayList<>();

        //Trying to do small list with all items with same profil
        Map<Boolean, List<DtoTadStop>> mapQuaysStopPlaces = tadToPersist.stream()
                .collect(Collectors.partitioningBy(
                        tad -> tad.getLocationType() != null && tad.getLocationType().equals("1")
                ));

        //StopPlace already in the csv
        List<DtoTadStop> dtoTadStopPlaces = mapQuaysStopPlaces.get(true);

        //The rest is Quays
        List<DtoTadStop> dtoTadQuays = mapQuaysStopPlaces.get(false);

        //We split Quays with parent and others
        Map<Boolean, List<DtoTadStop>> mapQuaysWithParent = dtoTadQuays.stream()
                .collect(Collectors.partitioningBy(
                        tad -> StringUtils.hasLength(tad.getParentStation())
                ));

        //Quays with parent
        List<DtoTadStop> dtoTadQuaysWithParent = mapQuaysWithParent.get(true);

        //Quays without parent
        List<DtoTadStop> dtoTadQuaysWithoutParent = mapQuaysWithParent.get(false);

        //More convenient case Quays have already their parents defined on the csv
        stopsToPersist.addAll(dtoTadStopPlaces.stream()
                .map(this::mapFromDtoToStopPlaceEntity)
                .map(stopPlace -> {
                    stopPlace.getQuays().addAll(dtoTadQuaysWithParent.stream()
                            .filter(tad -> stopPlace.getKeyValues().get(ORIGINAL_ID_KEY) != null
                                    && stopPlace.getKeyValues().get(ORIGINAL_ID_KEY).getItems().contains(createStopPlaceImportedId(tad.getParentStation())))
                            .map(this::mapFromDtoToQuayEntity)
                            .collect(Collectors.toList()));
                    return stopPlace;
                })
                .collect(Collectors.toList()));

        //Quays have not their parents defined on the csv, but they have a parentStation - Exception
        Map<String, List<DtoTadStop>> quaysByParentStation = dtoTadQuaysWithParent.stream()
                .filter(
                        tad -> StringUtils.hasLength(tad.getParentStation()) && stopsToPersist.stream()
                                .noneMatch(stopPlace -> stopPlace.getKeyValues().get(ORIGINAL_ID_KEY).getItems().contains(createStopPlaceImportedId(tad.getParentStation())))
                )
                .collect(Collectors.groupingBy(DtoTadStop::getParentStation, Collectors.toList()));
        if (!quaysByParentStation.keySet().isEmpty()){
            throw new IllegalArgumentException("There are parentStation " + quaysByParentStation.keySet() + " with no StopPlace defined in your CSV.");
        }

        //Quays without parents, or without parentStation exactly, we create one parent per Quay
        stopsToPersist.addAll(dtoTadQuaysWithoutParent.stream()
                .map(this::mapFromDtoToQuayEntity)
                .map(this::createStopPlaceForQuay)
                .collect(Collectors.toList()));

        logger.info("Mapping completed. Starting to persist TADs");

        for (StopPlace stopPlace : stopsToPersist) {
            StopPlace stopInBdd = retrieveTADinBDD(stopPlace);
            if (stopInBdd != null && stopInBdd.getNetexId() != null) {
                StopPlace updatedStop = versionCreator.createCopy(stopInBdd, StopPlace.class);
                if (populateTAD(updatedStop, stopPlace)) {
                    sopStopPlaceVersionedSaverService.saveNewVersion(updatedStop);
                }
            } else {
                sopStopPlaceVersionedSaverService.saveNewVersion(stopPlace);
            }
        }

    }

    private StopPlace createStopPlaceForQuay(Quay quay) {

        StopPlace stopPlace = new StopPlace();

        Set<Quay> quays = new HashSet<>();
        quays.add(quay);

        String originalId = "";
        List<String[]> id = quay.getOrCreateValues(ORIGINAL_ID_KEY).stream().map(value -> value.split(":")).collect(Collectors.toList());
        if(id.get(0) != null) {
            originalId = id.get(0)[id.get(0).length - 1];
        }

        stopPlace.setName(quay.getName());
        stopPlace.setAccessibilityAssessment(createDefaultAccessibilityAssessment());
        stopPlace.setCentroid(quay.getCentroid());
        //COM_ for stopPlace automatically generated
        stopPlace.getKeyValues().put(ORIGINAL_ID_KEY, new Value(createStopPlaceImportedId("COM_" + originalId)));
        stopPlace.getKeyValues().put("zonalStopPlace", new Value("yes"));
        if(quay.getKeyValues().get("TAD:TarrifZone:" + originalId) != null)
            stopPlace.getKeyValues().put("TAD:TarrifZone:" + originalId, quay.getKeyValues().get("TAD:TarrifZone:" + originalId));

        stopPlace.setQuays(quays);

        return stopPlace;
    }

    /**
     * Converts a DTO from the CSV read
     *
     * @param dtoTadCsv the DTO filled with data from the CSV
     * @return the entity StopPlace
     */
    public StopPlace mapFromDtoToStopPlaceEntity(DtoTadStop dtoTadCsv) {

        StopPlace newStopPlace = new StopPlace();

        newStopPlace.setName(new EmbeddableMultilingualString(dtoTadCsv.getStopName(), "FR"));
        newStopPlace.setCentroid(geometryFactory.createPoint(new Coordinate(Double.parseDouble(dtoTadCsv.getStopLon()), Double.parseDouble(dtoTadCsv.getStopLat()))));
        newStopPlace.getKeyValues().put("zonalStopPlace", new Value("yes"));
        newStopPlace.getKeyValues().put(ORIGINAL_ID_KEY, new Value(createStopPlaceImportedId(dtoTadCsv.getStopId())));
        if(StringUtils.hasLength(dtoTadCsv.getZoneId()))
            newStopPlace.getKeyValues().put("TAD:TarrifZone:" + dtoTadCsv.getStopId(), new Value(dtoTadCsv.getZoneId()));
        newStopPlace.setAccessibilityAssessment(createDefaultAccessibilityAssessment());

        return newStopPlace;
    }

    /**
     * Converts a DTO from the CSV read
     *
     * @param dtoTadCsv the DTO filled with data from the CSV
     * @return the entity Quay
     */
    public Quay mapFromDtoToQuayEntity(DtoTadStop dtoTadCsv) {

        Quay newQuay = new Quay();
        newQuay.getOrCreateValues(ORIGINAL_ID_KEY).add(dtoTadCsv.getStopId());
        newQuay.setName(new EmbeddableMultilingualString(dtoTadCsv.getStopName(), "FR"));
        newQuay.setCentroid(geometryFactory.createPoint(new Coordinate(Double.parseDouble(dtoTadCsv.getStopLon()), Double.parseDouble(dtoTadCsv.getStopLat()))));
        newQuay.getKeyValues().put("zonalStopPlace", new Value("yes"));
        newQuay.getKeyValues().put(ORIGINAL_ID_KEY, new Value(createQuayImportedId(dtoTadCsv.getStopId())));
        if(StringUtils.hasLength(dtoTadCsv.getZoneId()))
            newQuay.getKeyValues().put("TAD:TarrifZone:" + dtoTadCsv.getStopId(), new Value(dtoTadCsv.getZoneId()));

        newQuay.setAccessibilityAssessment(createDefaultAccessibilityAssessment());

        return newQuay;
    }

    private boolean populateTAD(StopPlace existingStopPlace, StopPlace newStopPlace) {
        boolean updated = false;
        AtomicBoolean updatedQuay = new AtomicBoolean(false);

        String originalId = "";
        List<String[]> id = newStopPlace.getOrCreateValues(ORIGINAL_ID_KEY).stream().map(value -> value.split(":")).collect(Collectors.toList());
        if(id.get(0) != null) {
            originalId = id.get(0)[id.get(0).length - 1];
        }
        List<String> zoneId = new ArrayList<>(newStopPlace.getKeyValues().get("TAD:TarrifZone:" + originalId).getItems());
        if (newStopPlace.getKeyValues() != null
                && !newStopPlace.getKeyValues().get("TAD:TarrifZone:"+originalId).equals(existingStopPlace.getKeyValues().get("TAD:TarrifZone:"+originalId))) {
            existingStopPlace.getOrCreateValues("TAD:TarrifZone:"+originalId).add(zoneId.get(0));
            updated = true;
        }
        List<Quay> newQuays = new ArrayList<>(newStopPlace.getQuays());
        Set<Quay> existingQuays = existingStopPlace.getQuays();

        //update quay with new version
        existingQuays.stream().forEach(quay -> {
            Set<String> importedQuayId = quay.getOriginalIds();
            Optional<Quay> matchingQuay = newQuays.stream()
                    .filter(newQuay -> newQuay.getOriginalIds().equals(importedQuayId))
                    .findFirst();
            if(matchingQuay.isPresent()){
                Quay newQuay = matchingQuay.get();
                String originalQuayId = importedQuayId.iterator().next().split(":")[2];
                List<String> zoneQuayId = new ArrayList<>(newQuay.getKeyValues().get("TAD:TarrifZone:" + originalQuayId).getItems());
                quay.getOrCreateValues("TAD:TarrifZone:" + originalQuayId).add(zoneQuayId.get(0));
                updatedQuay.set(true);
            }
        });

        //add eventual new Quays
        newQuays.forEach(newQuay -> {
            boolean isMatched = existingQuays.stream()
                    .anyMatch(quay -> quay.getOriginalIds().equals(newQuay.getOriginalIds()));

            if (!isMatched) {
                existingQuays.add(newQuay);
                updatedQuay.set(true);
            }
        });

        if(updatedQuay.get()){
            updated = true;
            existingStopPlace.setQuays(existingQuays);
        }
        return updated;
    }

    public String createStopPlaceImportedId(String tadId){
        return "TAD:StopPlace:" + tadId;
    }

    private String createQuayImportedId(String stopId) {
        return "TAD:Quay:" + stopId;
    }

    private AccessibilityAssessment createDefaultAccessibilityAssessment() {

        AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
        accessibilityAssessment.setVersion(1);
        accessibilityAssessment.setCreated(Instant.now());
        accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityAssessment.setLimitations(new ArrayList<>());

        AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
        accessibilityLimitation.setCreated(Instant.now());
        accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setAudibleSignalsAvailable(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setEscalatorFreeAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setLiftFreeAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setStepFreeAccess(LimitationStatusEnumeration.UNKNOWN);
        accessibilityLimitation.setVisualSignsAvailable(LimitationStatusEnumeration.UNKNOWN);

        accessibilityAssessment.getLimitations().add(accessibilityLimitation);

        return accessibilityAssessment;
    }

    /**
     * StopPlaces already in database have to be updated on a new version
     * @param stopPlace
     * @return
     */
    private StopPlace retrieveTADinBDD(StopPlace stopPlace) {
        Set<String> originalidsToSearch = new HashSet<>();
        if (stopPlace.getKeyValues().get(ORIGINAL_ID_KEY) != null) {
            stopPlace.getKeyValues().get(ORIGINAL_ID_KEY).getItems().forEach(originalidsToSearch::add);
        }
        String foundTADNetexId = stopPlaceRepository.findFirstByKeyValues(ORIGINAL_ID_KEY, originalidsToSearch);
        if (foundTADNetexId != null) {
            StopPlace foundTAD = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(foundTADNetexId);
            if (foundTAD.getCentroid().equalsExact(stopPlace.getCentroid(), 0.0001)) {
                return foundTAD;
            }
        }
        return null;
    }
}
