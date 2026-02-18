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

package org.rutebanken.tiamat;

import org.entur.gbfs.http.GBFSHttpClient;
import org.entur.gbfs.mapper.GBFSMapper;
import org.entur.gbfs.mapper.GBFSMapperImpl;
import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.rutebanken.tiamat.client.mdm.MdmClient;
import org.rutebanken.tiamat.client.mdm.TokenRelayInterceptor;
import org.rutebanken.tiamat.model.StopPlace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@SpringBootApplication
@Configuration
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
@EnableAsync
@EntityScan(basePackageClasses = {StopPlace.class, Jsr310JpaConverters.class})
public class TiamatApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiamatApplication.class, args);
    }

    @Bean
    public GBFSMapper gbfsMapper() {
        return new GBFSMapperImpl();
    }

    @Bean
    public GbfsValidator gbfsValidator() {
        return GbfsValidatorFactory.getGbfsJsonValidator();
    }

    @Bean
    public GBFSHttpClient gbfsHttpClient() {
        return new GBFSHttpClient();
    }

    @Bean
    public MdmClient mdmClient(@Value("${mdmApi.url}") String mdmApiUrl,
                               TokenRelayInterceptor interceptor
    ) {
        RestClient restClient = RestClient.builder()
                .baseUrl(mdmApiUrl)
                .requestInterceptor(interceptor)
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(MdmClient.class);
    }

}

