package org.rutebanken.tiamat.feign.mdm;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Set;

@FeignClient(value = "mdmClient", url = "${spring.cloud.openfeign.client.config.mdmClient.url}")
public interface MdmFeignClient {

    @PostMapping(value = "/stops")
    List<OkinaIdentifier> generateStopIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @GetMapping(value = "/stops")
    List<OkinaIdentifier> getStopPlaceIdentifiers(@RequestBody List<Long> spIds);

    @GetMapping(value = "/stops/byOriginalId")
    OkinaIdentifier getStopPlaceIdentifiersByOriginalId(@RequestBody OkinaIdentifier stopPlaceIdentifier);

    @GetMapping(value = "/stops/byDataset")
    Set<Long> getStopPlaceIdentifiersByDataset(@RequestBody String dataset);

    @PostMapping(value = "/stops/createOrUpdate")
    void createOrUpdateStopIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @PostMapping(value = "/stops/updateImportedIds")
    void updateStopImportedIds(@RequestBody List<OkinaIdentifier> identifiers);

    @PostMapping(value = "/stops/merge")
    void mergeStopIdentifiers(@RequestBody MergeIdentifier mergeIdentifier);

    @PostMapping(value = "/quays")
    List<OkinaIdentifier> generateQuayIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @GetMapping(value = "/quays")
    List<OkinaIdentifier> getQuayIdentifiers(@RequestBody List<Long> quayIds);

    @GetMapping(value = "/quays/byOriginalId")
    List<OkinaIdentifier> getQuayIdentifiersByOriginalId(@RequestBody List<OkinaIdentifier> quayIdentifiers);

    @PostMapping(value = "/quays/createOrUpdate")
    void createOrUpdateQuayIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @PostMapping(value = "/quays/updateImportedIds")
    void updateQuaysImportedIds(@RequestBody List<OkinaIdentifier> identifiers);

    @PostMapping(value = "/pois")
    List<OkinaIdentifier> generatePoiIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @GetMapping(value = "/pois")
    List<OkinaIdentifier> getPoisIdentifiers(@RequestBody List<Long> poiIds);

    @GetMapping(value = "/pois/byOriginalId")
    List<OkinaIdentifier> getPoiIdentifiersByOriginalId(@RequestBody List<OkinaIdentifier> quayIdentifiers);

    @PostMapping(value = "/pois/createOrUpdate")
    void createOrUpdatePoiIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @PostMapping(value = "/pois/updateImportedIds")
    void updatePoiImportedIds(@RequestBody List<OkinaIdentifier> identifiers);

    @PostMapping(value = "/parkings")
    List<OkinaIdentifier> generateParkingIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @GetMapping(value = "/parkings")
    List<OkinaIdentifier> getParkingIdentifiers(@RequestBody List<Long> parkingIds);

    @GetMapping(value = "/parkings/byOriginalId")
    List<OkinaIdentifier> getParkingsIdentifiersByOriginalId(@RequestBody List<OkinaIdentifier> quayIdentifiers);

    @PostMapping(value = "/parkings/updateImportedIds")
    void updateParkingsImportedIds(@RequestBody List<OkinaIdentifier> identifiers);

}
