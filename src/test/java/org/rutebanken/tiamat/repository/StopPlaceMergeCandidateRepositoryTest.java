package org.rutebanken.tiamat.repository;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.dtoassembling.dto.MergeMode;
import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceMergeCandidatePairDto;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.ValidBetween;
import org.rutebanken.tiamat.model.VehicleModeEnumeration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@TestPropertySource(locations = "classpath:application.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StopPlaceMergeCandidateRepositoryTest extends TiamatIntegrationTest {

    private static final Pageable PAGE = PageRequest.of(0, 100);

    private StopPlace createStopPlace(String name, double longitude, double latitude, VehicleModeEnumeration mode, String provider) {
        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString(name, ""));
        stopPlace.setCentroid(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        stopPlace.setTransportMode(mode);
        stopPlace.setProvider(provider);
        return stopPlaceRepository.save(stopPlace);
    }

    private Set<String> pairKeys(Page<StopPlaceMergeCandidatePairDto> page) {
        return page.getContent().stream()
                .map(p -> Set.of(p.getBase().getNetexId(), p.getCandidate().getNetexId()).toString())
                .collect(Collectors.toSet());
    }

    @Test
    void sameProviderMode_matchesOnExactRoundedPosition() {
        StopPlace s1 = createStopPlace("Gare A", 10.123456, 59.123456, VehicleModeEnumeration.BUS, "prov1");
        StopPlace s2 = createStopPlace("Gare B", 10.123459, 59.123459, VehicleModeEnumeration.BUS, "prov1");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.SAME_PROVIDER, null, PAGE);

        assertThat(pairKeys(result)).contains(Set.of(s1.getNetexId(), s2.getNetexId()).toString());
    }

    @Test
    void sameProviderMode_matchesOnSameNameWithin100m() {
        StopPlace s1 = createStopPlace("Gare Centrale", 10.500000, 59.500000, VehicleModeEnumeration.BUS, "prov1");
        // longitude delta of 0.0007 degrees at latitude 59.5 N is roughly 39.5m geodesic distance,
        // well under the 100m ST_DWithin threshold.
        StopPlace s2 = createStopPlace("gare centrale ", 10.50070, 59.500000, VehicleModeEnumeration.BUS, "prov1");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.SAME_PROVIDER, null, PAGE);

        assertThat(pairKeys(result)).contains(Set.of(s1.getNetexId(), s2.getNetexId()).toString());
    }

    @Test
    void sameProviderMode_excludesDifferentProvider() {
        createStopPlace("Gare A", 11.123456, 60.123456, VehicleModeEnumeration.BUS, "prov1");
        createStopPlace("Gare A", 11.123456, 60.123456, VehicleModeEnumeration.BUS, "prov2");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.SAME_PROVIDER, null, PAGE);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void sameProviderMode_scopedToProvider_onlyShowsThatProvider() {
        StopPlace provAS1 = createStopPlace("Gare A", 23.123456, 73.123456, VehicleModeEnumeration.BUS, "provA");
        StopPlace provAS2 = createStopPlace("Gare A", 23.123456, 73.123456, VehicleModeEnumeration.BUS, "provA");
        createStopPlace("Gare B", 24.123456, 74.123456, VehicleModeEnumeration.BUS, "provB");
        createStopPlace("Gare B", 24.123456, 74.123456, VehicleModeEnumeration.BUS, "provB");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.SAME_PROVIDER, "provA", PAGE);

        assertThat(pairKeys(result)).containsExactly(Set.of(provAS1.getNetexId(), provAS2.getNetexId()).toString());
    }

    @Test
    void multiProviderMode_matchesAcrossProviders() {
        StopPlace s1 = createStopPlace("Gare A", 12.123456, 61.123456, VehicleModeEnumeration.BUS, "prov1");
        StopPlace s2 = createStopPlace("Gare A", 12.123456, 61.123456, VehicleModeEnumeration.BUS, "prov2");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.MULTI_PROVIDER, null, PAGE);

        assertThat(pairKeys(result)).contains(Set.of(s1.getNetexId(), s2.getNetexId()).toString());
    }

    @Test
    void multiProviderMode_scopedToProvider_requiresAtLeastOneSideInProvider() {
        StopPlace a = createStopPlace("Gare A", 22.123456, 72.123456, VehicleModeEnumeration.BUS, "provA");
        StopPlace b = createStopPlace("Gare A", 22.123456, 72.123456, VehicleModeEnumeration.BUS, "provB");
        StopPlace c = createStopPlace("Gare A", 22.123456, 72.123456, VehicleModeEnumeration.BUS, "provC");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.MULTI_PROVIDER, "provA", PAGE);

        Set<String> keys = pairKeys(result);
        assertThat(keys).contains(Set.of(a.getNetexId(), b.getNetexId()).toString());
        assertThat(keys).contains(Set.of(a.getNetexId(), c.getNetexId()).toString());
        assertThat(keys).doesNotContain(Set.of(b.getNetexId(), c.getNetexId()).toString());
    }

    @Test
    void excludesDifferentTransportMode() {
        createStopPlace("Gare A", 13.123456, 62.123456, VehicleModeEnumeration.BUS, "prov1");
        createStopPlace("Gare A", 13.123456, 62.123456, VehicleModeEnumeration.RAIL, "prov1");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.MULTI_PROVIDER, null, PAGE);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void excludesExpiredStopPlace() {
        StopPlace expired = createStopPlace("Gare A", 14.123456, 63.123456, VehicleModeEnumeration.BUS, "prov1");
        expired.setValidBetween(new ValidBetween(Instant.EPOCH, Instant.now().minusSeconds(1000)));
        stopPlaceRepository.save(expired);
        createStopPlace("Gare A", 14.123456, 63.123456, VehicleModeEnumeration.BUS, "prov1");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.MULTI_PROVIDER, null, PAGE);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void excludesParentStopPlace() {
        StopPlace parent = createStopPlace("Gare A", 15.123456, 64.123456, VehicleModeEnumeration.BUS, "prov1");
        parent.setParentStopPlace(true);
        stopPlaceRepository.save(parent);
        createStopPlace("Gare A", 15.123456, 64.123456, VehicleModeEnumeration.BUS, "prov1");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.MULTI_PROVIDER, null, PAGE);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void threeMutuallyMergeableStopsProduceThreePairs() {
        StopPlace s1 = createStopPlace("Gare A", 16.123456, 65.123456, VehicleModeEnumeration.BUS, "prov1");
        StopPlace s2 = createStopPlace("Gare A", 16.123456, 65.123456, VehicleModeEnumeration.BUS, "prov1");
        StopPlace s3 = createStopPlace("Gare A", 16.123456, 65.123456, VehicleModeEnumeration.BUS, "prov1");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.SAME_PROVIDER, null, PAGE);

        List<String> ids = List.of(s1.getNetexId(), s2.getNetexId(), s3.getNetexId());
        long matchingPairs = result.getContent().stream()
                .filter(p -> ids.contains(p.getBase().getNetexId()) && ids.contains(p.getCandidate().getNetexId()))
                .count();
        assertThat(matchingPairs).isEqualTo(3);
    }

    @Test
    void sortsResultsByProviderThenNetexIdAscending() {
        // Created in z-then-a order (so ids/insertion order would put the z pair first) to prove
        // the sort groups by provider name, not by id/creation order.
        createStopPlace("Gare Z", 20.0, 70.0, VehicleModeEnumeration.BUS, "zProvider");
        createStopPlace("Gare Z", 20.0, 70.0, VehicleModeEnumeration.BUS, "zProvider");
        createStopPlace("Gare A", 21.0, 71.0, VehicleModeEnumeration.BUS, "aProvider");
        createStopPlace("Gare A", 21.0, 71.0, VehicleModeEnumeration.BUS, "aProvider");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.MULTI_PROVIDER, null, PAGE);

        List<String> providersInOrder = result.getContent().stream()
                .map(p -> p.getBase().getProvider())
                .filter(provider -> provider.equals("aProvider") || provider.equals("zProvider"))
                .distinct()
                .collect(Collectors.toList());

        assertThat(providersInOrder).containsExactly("aProvider", "zProvider");
    }

    @Test
    void noMatchReturnsEmptyPage() {
        createStopPlace("Gare A", 17.123456, 66.123456, VehicleModeEnumeration.BUS, "prov1");

        Page<StopPlaceMergeCandidatePairDto> result = stopPlaceRepository.findMergeableStopPlaces(MergeMode.SAME_PROVIDER, null, PAGE);

        assertThat(result.getContent()).isEmpty();
    }
}
