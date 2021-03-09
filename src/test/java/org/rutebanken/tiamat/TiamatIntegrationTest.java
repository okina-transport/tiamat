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

import com.hazelcast.core.HazelcastInstance;
import org.locationtech.jts.geom.GeometryFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.rutebanken.tiamat.domain.ChouetteInfo;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.netex.id.GeneratedIdState;
import org.rutebanken.tiamat.repository.CacheProviderRepository;
import org.rutebanken.tiamat.repository.GroupOfStopPlacesRepository;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.repository.PathJunctionRepository;
import org.rutebanken.tiamat.repository.PathLinkRepository;
import org.rutebanken.tiamat.repository.ProviderRepository;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.repository.TagRepository;
import org.rutebanken.tiamat.repository.TariffZoneRepository;
import org.rutebanken.tiamat.repository.TopographicPlaceRepository;
import org.rutebanken.tiamat.service.BlobStoreService;
import org.rutebanken.tiamat.service.TariffZonesLookupService;
import org.rutebanken.tiamat.service.TopographicPlaceLookupService;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.GroupOfStopPlacesSaverService;
import org.rutebanken.tiamat.versioning.save.ParkingVersionedSaverService;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.rutebanken.tiamat.netex.id.GaplessIdGeneratorService.INITIAL_LAST_ID;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TiamatTestApplication.class)
@ActiveProfiles("geodb")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public abstract class TiamatIntegrationTest {

    private static Logger logger = LoggerFactory.getLogger(TiamatIntegrationTest.class);

    @Autowired
    protected GroupOfStopPlacesRepository groupOfStopPlacesRepository;

    @Autowired
    protected GroupOfStopPlacesSaverService groupOfStopPlacesSaverService;

    @Autowired
    protected StopPlaceRepository stopPlaceRepository;

    @Autowired
    protected StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    @Autowired
    protected ParkingRepository parkingRepository;

    @Autowired
    protected ParkingVersionedSaverService parkingVersionedSaverService;

    @Autowired
    protected TopographicPlaceRepository topographicPlaceRepository;

    @Autowired
    protected PathLinkRepository pathLinkRepository;

    @Autowired
    protected PathJunctionRepository pathJunctionRepository;

    @Autowired
    protected QuayRepository quayRepository;

    @Autowired
    protected GeometryFactory geometryFactory;

    @Autowired
    protected TariffZoneRepository tariffZoneRepository;

    @Autowired
    protected HazelcastInstance hazelcastInstance;

    @Autowired
    protected GeneratedIdState generatedIdState;

    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    @Autowired
    private TopographicPlaceLookupService topographicPlaceLookupService;

    @Autowired
    private TariffZonesLookupService tariffZonesLookupService;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    protected VersionCreator versionCreator;

    @Autowired
    protected RoleAssignmentExtractor roleAssignmentExtractor;

    @MockBean
    protected CacheProviderRepository providerRepository ;

    @MockBean
    protected BlobStoreService blobStoreService ;

    @Value("${local.server.port}")
    protected int port;

    @Before
    public void initProviderRepository() {
        ChouetteInfo chouetteInfo = new ChouetteInfo();
        chouetteInfo.codeIdfm = "test";
        chouetteInfo.id = 1L;
        chouetteInfo.xmlns = "xmlns";
        chouetteInfo.xmlnsurl = "http://xmlns";
        chouetteInfo.referential = "test";
        chouetteInfo.organisation = "test";
        chouetteInfo.idfm = true;
        chouetteInfo.codeIdfm = "test";

        Provider provider = new Provider();
        provider.id=1L;
        provider.name = "test";
        provider.chouetteInfo = chouetteInfo;

        List<Provider> providers = new ArrayList<>();
        providers.add(provider);

        when(providerRepository.getProviders()).thenReturn(providers);
        when(providerRepository.getProvider(anyLong())).thenReturn(providers.get(0));


        doNothing().when(blobStoreService).upload(isA(String.class), isA(File.class));


    }

    @Before
    @After
    public void clearRepositories() {

        groupOfStopPlacesRepository.flush();
        groupOfStopPlacesRepository.deleteAll();

        stopPlaceRepository.flush();

        pathLinkRepository.deleteAll();
        pathLinkRepository.flush();

        stopPlaceRepository.findAll().stream()
                .filter(StopPlace::isParentStopPlace)
                .forEach(sp -> {
                    stopPlaceRepository.delete(sp);
                });
        stopPlaceRepository.flush();
        stopPlaceRepository.deleteAll();

        stopPlaceRepository.flush();

        quayRepository.deleteAll();
        quayRepository.flush();

        topographicPlaceRepository.deleteAll();
        topographicPlaceRepository.flush();
        topographicPlaceLookupService.reset();

        parkingRepository.deleteAll();
        parkingRepository.flush();

        tariffZoneRepository.deleteAll();
        tariffZoneRepository.flush();
        tariffZonesLookupService.reset();

        clearIdGeneration();

        tagRepository.deleteAll();
        tagRepository.flush();
    }

    /**
     * Clear id_generator table and reset available ID queues and last IDs for entities.
     */
    private void clearIdGeneration() {

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        generatedIdState.getRegisteredEntityNames().forEach(entityName -> {
            hazelcastInstance.getQueue(entityName).clear();
            generatedIdState.setLastIdForEntity(entityName, INITIAL_LAST_ID);
            generatedIdState.getClaimedIdListForEntity(entityName).clear();
        });

        int updated = entityManager.createNativeQuery("DELETE FROM id_generator").executeUpdate();
        logger.debug("Cleared id generator table. deleted: {}", updated);
        transaction.commit();
        entityManager.close();
    }
}
