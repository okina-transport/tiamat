package org.rutebanken.tiamat.feign.mdm;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigInteger;
import java.util.List;

@FeignClient(value = "mdmClient", url = "${spring.cloud.openfeign.client.config.mdmClient.url}")
public interface MdmFeignClient {

    @PostMapping(value = "/stops")
    List<OkinaIdentifier> generateStopIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @PostMapping(value = "/quays")
    List<OkinaIdentifier> generateQuayIdentifiers(@RequestBody List<OkinaIdentifier> identifiers);

    @GetMapping(value = "/quays")
    List<OkinaIdentifier> getQuayIdentifiers(@RequestBody List<Long> quayIds);

    @GetMapping(value = "/stops")
    List<OkinaIdentifier> getStopPlaceIdentifiers(@RequestBody List<Long> quayIds);

    @GetMapping(value = "/stops/byOriginalId")
    OkinaIdentifier getStopPlaceIdentifiersByOriginalId(@RequestBody OkinaIdentifier stopPlaceIdentifier);

    @GetMapping(value = "/quays/byOriginalId")
    List<OkinaIdentifier> getQuayIdentifiersByOriginalId(@RequestBody List<OkinaIdentifier> quayIdentifiers);

}
