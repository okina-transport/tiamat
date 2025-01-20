package org.rutebanken.tiamat.rest.stopPlacesNetex;

import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class ImportStopPlacesNetexResourceTest extends TiamatIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void geocodeResourceTest() {
        List<String> input = List.of("MOBIITI:Quay:9", "MOBIITI:Quay:755");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<List<String>> requestEntity = new HttpEntity<>(input, headers);

        ResponseEntity<Void> response = restTemplate.exchange("/services/stop_places/netex_stops/geocode", HttpMethod.POST, requestEntity, Void.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void geocodeResourceInvalidPayloadTest() {
        String input = "MOBIITI:Quay:9, MOBIITI:Quay:755";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<String> requestEntity = new HttpEntity<>(input, headers);

        ResponseEntity<Void> response = restTemplate.exchange("/services/stop_places/netex_stops/geocode", HttpMethod.POST, requestEntity, Void.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

}