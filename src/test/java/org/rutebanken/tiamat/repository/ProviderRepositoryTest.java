package org.rutebanken.tiamat.repository;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.domain.Provider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ProviderRepositoryTest extends TiamatIntegrationTest {

    @Autowired
    private CacheProviderRepository providerRepository;

//    @Test
//    public void findByName() {
//        Provider provider = new Provider(1L, "Provider name");
//        providerRepository.save(provider);
//
//        List<Provider> foundProvider = providerRepository.findByName(provider.getName());
//
//        Assertions.assertThat(foundProvider.get(0)).isEqualToComparingFieldByField(provider);
//    }
}