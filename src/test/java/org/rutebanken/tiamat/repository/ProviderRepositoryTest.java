package org.rutebanken.tiamat.repository;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.Provider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ProviderRepositoryTest extends TiamatIntegrationTest {

    @Autowired
    private ProviderRepository providerRepository;

    @Test
    public void findByName() {
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setName("yolo");
        providerRepository.save(provider);

        List<Provider> foundProvider = providerRepository.findByName("yolo");

        Assertions.assertThat(foundProvider.get(0)).isEqualToComparingFieldByField(provider);
    }
}