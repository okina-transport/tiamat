package org.rutebanken.tiamat.importers;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.junit.Before;
import org.rutebanken.tiamat.TiamatApplication;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rutebanken.tiamat.repository.TopographicPlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test stop place importer with geodb and repository.
 * See also {@link DefaultStopPlaceImporterTest}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TiamatApplication.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ActiveProfiles("geodb")
@Transactional
public class DefaultStopPlaceImporterWithGeoDBTest {

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private QuayRepository quayRepository;

    @Autowired
    private GeometryFactory geometryFactory;

    @Autowired
    private DefaultStopPlaceImporter defaultStopPlaceImporter;

    @Autowired
    private TopographicPlaceRepository topographicPlaceRepository;


    @Before
    public void cleanRepositories() {
        stopPlaceRepository.deleteAll();
        topographicPlaceRepository.deleteAll();
        quayRepository.deleteAll();
    }

    /**
     * Two stop places with the same name and coordinates should become one stop place.
     * Because the those stop places each have one quay with the same coordinates, they should treated as one quay.
     */
    @Test
    public void quaysWithSameCoordinatesMustNotBeAddedMultipleTimes() throws ExecutionException, InterruptedException {
        String name = "Hestehovveien";

        double stopPlaceLatitude = 59.422556268440956728227320127189159393310546875;
        double stopPlaceLongitude = 5.265704397012616055917533230967819690704345703125;

        double quayLatitude = 59.4221750629462661663637845776975154876708984375;
        double quayLongitude = 5.2646351097871768587310725706629455089569091796875;

        StopPlace firstStopPlace = createStopPlace(name,
                stopPlaceLongitude, stopPlaceLatitude, null);
        firstStopPlace.getQuays().add(createQuay(name, quayLongitude, quayLatitude, null));

        // Another quay with different coordinates
        firstStopPlace.getQuays().add(createQuay(name, quayLongitude + 0.01, quayLatitude + 0.01, null));

        AtomicInteger topographicPlacesCounter = new AtomicInteger();
        SiteFrame siteFrame = new SiteFrame();

        // Import first stop place.
        defaultStopPlaceImporter.importStopPlace(firstStopPlace, siteFrame, topographicPlacesCounter);

        StopPlace secondStopPlace = createStopPlace(name,
                stopPlaceLongitude, stopPlaceLatitude, null);
        secondStopPlace.getQuays().add(createQuay(name, quayLongitude, quayLatitude, null));

        // Import second stop place
        StopPlace importResult = defaultStopPlaceImporter.importStopPlace(secondStopPlace, siteFrame, topographicPlacesCounter);

        assertThat(importResult.getId()).isEqualTo(importResult.getId());
        assertThat(importResult.getQuays()).hasSize(2);

        assertThat(importResult.getQuays().iterator().next().getName().getValue()).isEqualTo(name);

    }

    @Test
    public void addQuaysToStopPlaceWithoutQuays() throws ExecutionException, InterruptedException {
        String name = "Eselbergveien";

        double longitude = 5;
        double latitude = 71;

        StopPlace firstStopPlace = createStopPlace(name, longitude, latitude, null);

        AtomicInteger topographicPlacesCounter = new AtomicInteger();
        SiteFrame siteFrame = new SiteFrame();

        // Import first stop place.
        defaultStopPlaceImporter.importStopPlace(firstStopPlace, siteFrame, topographicPlacesCounter);

        StopPlace secondStopPlace = createStopPlace(name, longitude, latitude, null);
        secondStopPlace.getQuays().add(createQuay(name, longitude, latitude, null));

        // Import second stop place
        StopPlace importResult = defaultStopPlaceImporter.importStopPlace(secondStopPlace, siteFrame, topographicPlacesCounter);

        assertThat(importResult.getId()).isEqualTo(importResult.getId());
        assertThat(importResult.getQuays()).hasSize(1);

        assertThat(importResult.getQuays().iterator().next().getName().getValue()).isEqualTo(name);
    }

    @Test
    public void reproduceIssueWithCollectionNotAssosiatedWithAnySession() throws ExecutionException, InterruptedException {
        String name = "Skillebekkgata";
        StopPlace firstStopPlace = createStopPlaceWithQuay(name,
                6, 60, 11063200L, 11063200L);
        defaultStopPlaceImporter.importStopPlace(firstStopPlace, new SiteFrame(), new AtomicInteger());
        StopPlace secondStopPlace = createStopPlaceWithQuay(name,
                6, 60.0001, 11063198L, 11063198L);
        defaultStopPlaceImporter.importStopPlace(secondStopPlace, new SiteFrame(), new AtomicInteger());
    }

    /**
     * Import two stop places with the same coordinates.
     * Verify that quays are not added multiple times.
     */
    @Test
    public void quaysWithSameCoordinatesMustNotBeAddedMultipleTimes2() throws ExecutionException, InterruptedException {
        String name = "Mjåsund";

        double latitude = 59.32262902417035;
        double longitude = 5.447071920203443;
        long stopPlaceId = 11463484L;
        long quayId = 11463483L;

        StopPlace firstStopPlace = createStopPlaceWithQuay(name,
                longitude, latitude, stopPlaceId, quayId);

        AtomicInteger topographicPlacesCounter = new AtomicInteger();
        SiteFrame siteFrame = new SiteFrame();

        // Import first stop place.
        defaultStopPlaceImporter.importStopPlace(firstStopPlace, siteFrame, topographicPlacesCounter);

        StopPlace secondStopPlace = createStopPlaceWithQuay(name,
                longitude, latitude, stopPlaceId, quayId);

        // Import second stop place with a quay with the same coordinates as second stop place
        StopPlace actualStopPlace = defaultStopPlaceImporter.importStopPlace(secondStopPlace, siteFrame, topographicPlacesCounter);

        assertThat(actualStopPlace.getQuays()).hasSize(1);
    }

    @Test
    public void reproduceIssueWithDuplicateCountiesAndMunicipalities() {

        List<StopPlace> stopPlaces = new ArrayList<>();

        for(int i = 0; i < 10; i++) {
            StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("Stop place " + i));
            stopPlace.setId(Long.valueOf(i));
            stopPlace.setCentroid(geometryFactory.createPoint(new Coordinate(10.0393763, 59.750071)));
            stopPlaces.add(stopPlace);
        }

        AtomicInteger topographicPlacesConter = new AtomicInteger();

        stopPlaces.parallelStream()
                .forEach(stopPlace -> {
                    try {

                        defaultStopPlaceImporter.importStopPlace(stopPlace, new SiteFrame(), topographicPlacesConter);
                    } catch (ExecutionException|InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });

        Iterable<TopographicPlace> topographicPlaces = topographicPlaceRepository.findAll();

        final int[] size = new int[1];
        topographicPlaces.forEach(tp -> {
            size[0]++;
            System.out.println("Topographic place repo contains " + tp.getName());
        });

        assertThat(size[0])
                .as("Number of topographic places in the repository is not as expected")
                .isEqualTo(2);

        List<TopographicPlace> counties = topographicPlaceRepository
                .findByNameValueAndCountryRefRefAndTopographicPlaceType(
                        "Buskerud",
                        IanaCountryTldEnumeration.NO,
                        TopographicPlaceTypeEnumeration.COUNTY);

        assertThat(counties).hasSize(1);

        List<TopographicPlace> municipalities = topographicPlaceRepository
                .findByNameValueAndCountryRefRefAndTopographicPlaceType(
                        "Nedre Eiker",
                        IanaCountryTldEnumeration.NO,
                        TopographicPlaceTypeEnumeration.TOWN);

        assertThat(municipalities).hasSize(1);

        assertThat(topographicPlacesConter.get()).isEqualTo(2);
    }


    private Point point(double longitude, double latitude) {
        return 
                geometryFactory.createPoint(
                        new Coordinate(longitude, latitude));
    }

    private StopPlace createStopPlaceWithQuay(String name, double longitude, double latitude, Long stopPlaceId, Long quayId) {
        StopPlace stopPlace = createStopPlace(name, longitude, latitude, stopPlaceId);
        stopPlace.getQuays().add(createQuay(name, longitude, latitude, quayId));
        return stopPlace;
    }

    private StopPlace createStopPlace(String name, double longitude, double latitude, Long stopPlaceId) {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setCentroid(point(longitude, latitude));
        stopPlace.setName(new EmbeddableMultilingualString(name, ""));
        stopPlace.setId(stopPlaceId);
        return stopPlace;
    }

    private Quay createQuay(String name, double longitude, double latitude, Long id) {
        Quay quay = new Quay();
        quay.setName(new EmbeddableMultilingualString(name, ""));
        quay.setId(id);
        quay.setCentroid(point(longitude, latitude));
        return quay;
    }


}
