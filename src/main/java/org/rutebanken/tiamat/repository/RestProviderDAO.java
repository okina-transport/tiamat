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
 *
 */

package org.rutebanken.tiamat.repository;

import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class RestProviderDAO {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${providers.api.url}")
    private String restServiceUrl;


    @Autowired
    private TokenService tokenService;


    public Collection<Provider> getProviders() {
/*
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<List<Provider>> rateResponse =
                restTemplate.exchange(restServiceUrl,
                        HttpMethod.GET, getEntityWithAuthenticationToken(), new ParameterizedTypeReference<List<Provider>>() {
                        });
        return rateResponse.getBody();
*/
        return getFakeProviders();
    }

    /**
     * Recupere une liste de faux 'providers' de donnees
     * @return une liste de faux 'providers' de donnees
     */
    private List<Provider> getFakeProviders() {
        List<Provider> listFakeProviders = new ArrayList<>();
        listFakeProviders.add(getFakeProvider(1L, "PROVIDER1"));
        listFakeProviders.add(getFakeProvider(2L, "PROVIDER2"));
        return listFakeProviders;
    }

    /**
     * Cree un faux 'provider" de donnee
     * @param rId : identifiant
     * @param rName : Nom du provider
     * @return la classe Provider
     */
    private Provider getFakeProvider(Long rId, String rName) {
        Provider fakeProvider = new Provider();
        fakeProvider.id = rId;
        fakeProvider.name = rName;
        return fakeProvider;
    }

    private HttpEntity<String> getEntityWithAuthenticationToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tokenService.getToken());
        return new HttpEntity<>(headers);
    }

}

