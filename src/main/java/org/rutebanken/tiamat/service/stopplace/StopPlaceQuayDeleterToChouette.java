/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.service.stopplace;

import org.rutebanken.tiamat.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Component
@Transactional
public class StopPlaceQuayDeleterToChouette {

    @Value("${chouette.url}")
    private String chouetteUrl;

    @Autowired
    private TokenService tokenService;

    private HttpEntity<String> getEntityWithAuthenticationToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tokenService.getToken());
        return new HttpEntity<>(headers);
    }

    public String delete(String netexId) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(chouetteUrl + "stop_place/deleteStopAreas/" + netexId, HttpMethod.DELETE, getEntityWithAuthenticationToken(), String.class);
        return response.getBody();
    }
}
