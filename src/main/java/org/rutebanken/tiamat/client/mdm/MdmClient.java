package org.rutebanken.tiamat.client.mdm;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Set;

@HttpExchange
public interface MdmClient {

    @PostExchange("/stops")
    List<OkinaIdentifier> generateStopIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @GetExchange("/stops")
    List<OkinaIdentifier> getStopPlaceIdentifiers(@RequestBody List<Long> spIds);

    @GetExchange("/stops/byOriginalId")
    OkinaIdentifier getStopPlaceIdentifiersByOriginalId(@RequestBody OkinaIdentifier stopPlaceIdentifier);

    @GetExchange("/stops/byDataset")
    Set<Long> getStopPlaceIdentifiersByDataset(@RequestBody String dataset);

    @DeleteExchange("/stops/byDataset/{dataset}")
    void deleteStopPlacesByDataset(@PathVariable String dataset);

    @DeleteExchange("/stops/bySuperId/{superId}")
    void deleteStopPlaceBySuperId(@PathVariable String superId);

    @PostExchange("/stops/createOrUpdate")
    void createOrUpdateStopIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @PostExchange("/stops/updateImportedIds")
    void updateStopImportedIds(@RequestBody List<OkinaIdentifier> identifiers);

    @PostExchange("/stops/merge")
    void mergeStopIdentifiers(@RequestBody MergeIdentifier mergeIdentifier);

    @PostExchange("/quays")
    List<OkinaIdentifier> generateQuayIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @GetExchange("/quays")
    List<OkinaIdentifier> getQuayIdentifiers(@RequestBody List<Long> quayIds);

    @GetExchange("/quays/byOriginalId")
    List<OkinaIdentifier> getQuayIdentifiersByOriginalId(@RequestBody List<OkinaIdentifier> quayIdentifiers);

    @DeleteExchange("/quays/byDataset/{dataset}")
    void deleteQuaysByDataset(@PathVariable String dataset);

    @DeleteExchange("/quays/bySuperId/{superId}")
    void deleteQuaysBySuperId(@PathVariable String superId);

    @PostExchange("/quays/createOrUpdate")
    void createOrUpdateQuayIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @PostExchange("/quays/updateImportedIds")
    void updateQuaysImportedIds(@RequestBody List<OkinaIdentifier> identifiers);

    @PostExchange("/pois")
    List<OkinaIdentifier> generatePoiIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @GetExchange("/pois")
    List<OkinaIdentifier> getPoisIdentifiers(@RequestBody List<Long> poiIds);

    @DeleteExchange("/pois")
    void deleteAllPoisIds();

    @DeleteExchange("/pois/bySuperId/{superId}")
    void deletePoisBySuperId(@PathVariable String superId);

    @GetExchange("/pois/byOriginalId")
    List<OkinaIdentifier> getPoiIdentifiersByOriginalId(@RequestBody List<OkinaIdentifier> quayIdentifiers);

    @PostExchange("/pois/createOrUpdate")
    void createOrUpdatePoiIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @PostExchange("/pois/updateImportedIds")
    void updatePoiImportedIds(@RequestBody List<OkinaIdentifier> identifiers);

    @PostExchange("/parkings")
    List<ParkingIdentifier> generateParkingIdentifiers(@RequestBody List<ParkingIdentifier> identifiers);

    @GetExchange("/parkings")
    List<ParkingIdentifier> getParkingIdentifiers(@RequestBody List<String> parkingIds);

    @DeleteExchange("/parkings")
    void deleteAllParkingIds();


    @DeleteExchange("/parkings/bySuperId/{superId}")
    void deleteParkingsBySuperId(@PathVariable String superId);

    @GetExchange("/parkings/byOperatorAndOriginalId")
    ParkingIdentifier getParkingIdentifierbyOperatorAndOriginalId(@RequestParam("operator") String operator,
                                                                  @RequestParam("originalId") String originalId);

    @PostExchange("/parkings/updateImportedIds")
    void updateParkingsImportedIds(@RequestBody List<ParkingIdentifier> identifiers);

    @PostExchange("/organisations")
    OkinaIdentifier generateOrganisationIdentifier(@RequestBody OkinaIdentifier identifier);

    @GetExchange("/organisations/byOriginalId")
    OkinaIdentifier getOrganisationsIdentifierByOriginalId(@RequestParam("originalId") String originalId);

    @PostExchange("/organisations/updateImportedIds")
    void updateOrganisationsImportedIds(@RequestBody List<OkinaIdentifier> identifiers);

}
