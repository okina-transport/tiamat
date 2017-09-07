package org.rutebanken.tiamat.service.stopplace;

import com.vividsolutions.jts.geom.Coordinate;
import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.MERGED_ID_KEY;

public class StopPlaceMergerTest extends TiamatIntegrationTest {

    @Autowired
    private StopPlaceMerger stopPlaceQuayMerger;

    @Test
    @Transactional
    public void testMergeStopPlaces() {

        Instant atTestStart = Instant.now();

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.setName(new EmbeddableMultilingualString("Name"));
        fromStopPlace.setCentroid(geometryFactory.createPoint(new Coordinate(11.1, 60.1)));
        fromStopPlace.getOriginalIds().add("TEST:StopPlace:1234");
        fromStopPlace.getOriginalIds().add("TEST:StopPlace:5678");


        PlaceEquipment fromPlaceEquipment = new PlaceEquipment();
        GeneralSign generalSign = new GeneralSign();
        generalSign.setSignContentType(SignContentEnumeration.TRANSPORT_MODE);
        generalSign.setPublicCode(new PrivateCodeStructure("111", "111111"));
        fromPlaceEquipment.getInstalledEquipment().add(generalSign);
        fromStopPlace.setPlaceEquipments(fromPlaceEquipment);

        String testKey = "testKey";
        String testValue = "testValue";
        fromStopPlace.getKeyValues().put(testKey, new Value(testValue));

        Quay fromQuay = new Quay();
        fromQuay.setCompassBearing(new Float(90));
        fromQuay.setCentroid(geometryFactory.createPoint(new Coordinate(11.2, 60.2)));
        fromQuay.getOriginalIds().add("TEST:Quay:123401");
        fromQuay.getOriginalIds().add("TEST:Quay:567801");

        fromStopPlace.getQuays().add(fromQuay);

        String oldVersionComment = "Old version deleted";
        fromStopPlace.setVersionComment(oldVersionComment);

        fromStopPlace.setTransportMode(VehicleModeEnumeration.BUS);

        stopPlaceVersionedSaverService.saveNewVersion(fromStopPlace);

        StopPlace toStopPlace = new StopPlace();
        toStopPlace.setName(new EmbeddableMultilingualString("Name 2"));
        toStopPlace.setCentroid(geometryFactory.createPoint(new Coordinate(11.11, 60.11)));
        toStopPlace.getOriginalIds().add("TEST:StopPlace:4321");
        toStopPlace.getOriginalIds().add("TEST:StopPlace:8765");
        // Old version of toStopPlace
        Instant toStopPlaceOriginalFromDate = Instant.EPOCH;
        toStopPlace.setValidBetween(new ValidBetween(toStopPlaceOriginalFromDate));

        Quay toQuay = new Quay();
        toQuay.setCompassBearing(new Float(90));
        toQuay.setCentroid(geometryFactory.createPoint(new Coordinate(11.21, 60.21)));
        toQuay.getOriginalIds().add("TEST:Quay:432101");
        toQuay.getOriginalIds().add("TEST:Quay:876501");

        toStopPlace.getQuays().add(toQuay);

        stopPlaceVersionedSaverService.saveNewVersion(toStopPlace);

        // Act
        StopPlace mergedStopPlace = stopPlaceQuayMerger.mergeStopPlaces(fromStopPlace.getNetexId(), toStopPlace.getNetexId(), null, null, false);

        assertThat(mergedStopPlace).isNotNull();

        assertThat(fromStopPlace.getOriginalIds()).isNotEmpty();
        assertThat(toStopPlace.getOriginalIds()).isNotEmpty();

        assertThat(mergedStopPlace.getOriginalIds()).hasSize(fromStopPlace.getOriginalIds().size() + toStopPlace.getOriginalIds().size());

        assertThat(mergedStopPlace.getOriginalIds().containsAll(fromStopPlace.getOriginalIds()));

        assertThat(mergedStopPlace.getKeyValues().get(testKey)).isNotNull();
        assertThat(mergedStopPlace.getKeyValues().get(testKey).getItems()).hasSize(1);
        assertThat(mergedStopPlace.getKeyValues().get(testKey).getItems()).contains(testValue);

        assertThat(mergedStopPlace.getName().getValue()).matches(toStopPlace.getName().getValue());

        assertThat(mergedStopPlace.getVersionComment()).isNull();
        assertThat(mergedStopPlace.getTransportMode()).isEqualTo(VehicleModeEnumeration.BUS);

        // Equipment
        PlaceEquipment placeEquipment = mergedStopPlace.getPlaceEquipments();
        assertThat(placeEquipment).isNotNull();
        List<InstalledEquipment_VersionStructure> equipment = placeEquipment.getInstalledEquipment();
        assertThat(equipment).hasSize(1);
        assertThat(equipment).doesNotContain(generalSign); // Result from merge does not contain same object
        assertThat(equipment.get(0)).isInstanceOf(GeneralSign.class);
        assertThat(((GeneralSign)equipment.get(0)).getSignContentType()).isEqualTo(SignContentEnumeration.TRANSPORT_MODE);

        // assertQuays
        assertThat(mergedStopPlace.getQuays()).hasSize(2);
        mergedStopPlace.getQuays().forEach(quay -> {
            if (quay.getNetexId().equals(fromQuay.getNetexId())) {

                //The from-Quay has increased its version twice - once for terminating 'from', once for adding to 'to'
                assertThat(quay.getVersion()).isEqualTo(1 + fromQuay.getVersion());
                assertThat(quay.equals(fromQuay));

            } else if (quay.getNetexId().equals(toQuay.getNetexId())){

                assertThat(quay.getVersion()).isEqualTo(1 + toQuay.getVersion());
                assertThat(quay.equals(toQuay));

            } else {
                fail("Unknown Quay has been added");
            }
        });

        StopPlace stopPlaceBeforeMerging = stopPlaceRepository.findFirstByNetexIdAndVersion(toStopPlace.getNetexId(), toStopPlace.getVersion());

        assertThat(mergedStopPlace.getValidBetween().getFromDate())
                .as("merged stop place from date")
                .isEqualTo(stopPlaceBeforeMerging.getValidBetween().getToDate());

        assertThat(mergedStopPlace.getValidBetween().getFromDate())
                .as("merged stop place from date should have version from date after test started")
                .isAfterOrEqualTo(atTestStart);

        assertThat(stopPlaceBeforeMerging.getValidBetween().getFromDate())
                .as("old version of to-stopplace should not have changed from date")
                .isEqualTo(toStopPlaceOriginalFromDate);

        assertThat(stopPlaceBeforeMerging.getValidBetween().getToDate())
                .as("old version of to-stopplace should have its to date updated")
                .isAfterOrEqualTo(atTestStart);


    }

    @Test
    @Transactional
    public void testMergeStopPlacesWithTariffZones() {

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.setName(new EmbeddableMultilingualString("Name"));
        
        Set<TariffZoneRef> fromTzSet = new HashSet<>();
        TariffZoneRef fromTz = new TariffZoneRef();
        fromTz.setRef("NSR:TZ:1");
        fromTz.setVersion("1");
        fromTzSet.add(fromTz);
        fromStopPlace.setTariffZones(fromTzSet);


        StopPlace toStopPlace = new StopPlace();
        toStopPlace.setName(new EmbeddableMultilingualString("Name 2"));

        Set<TariffZoneRef> toTzSet = new HashSet<>();
        TariffZoneRef toTz = new TariffZoneRef();
        toTz.setRef("NSR:TZ:2");
        toTz.setVersion("2");
        toTzSet.add(toTz);
        toStopPlace.setTariffZones(toTzSet);

        stopPlaceVersionedSaverService.saveNewVersion(fromStopPlace);
        stopPlaceVersionedSaverService.saveNewVersion(toStopPlace);

        StopPlace mergedStopPlace = stopPlaceQuayMerger.mergeStopPlaces(fromStopPlace.getNetexId(), toStopPlace.getNetexId(), null, null, false);

        assertThat(mergedStopPlace.getTariffZones()).hasSize(2);

    }


    @Test
    @Transactional
    public void testMergeStopPlacesShouldIgnoreValidBetween() {

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.setName(new EmbeddableMultilingualString("Name"));
        ValidBetween fromValidBetween = new ValidBetween(Instant.now().minusSeconds(3600));
        fromStopPlace.setValidBetween(fromValidBetween);

        StopPlace toStopPlace = new StopPlace();
        toStopPlace.setName(new EmbeddableMultilingualString("Name 2"));

        ValidBetween toValidBetween = new ValidBetween(Instant.now().minusSeconds(1800));
        toStopPlace.setValidBetween(toValidBetween);


        stopPlaceVersionedSaverService.saveNewVersion(fromStopPlace);
        stopPlaceVersionedSaverService.saveNewVersion(toStopPlace);

        StopPlace mergedStopPlace = stopPlaceQuayMerger.mergeStopPlaces(fromStopPlace.getNetexId(), toStopPlace.getNetexId(), null, null, false);

        assertThat(mergedStopPlace.getValidBetween()).isNotNull();
        assertThat(mergedStopPlace.getValidBetween().getFromDate()).isNotNull();
        assertThat(mergedStopPlace.getValidBetween().getToDate()).isNull();

    }

    @Test
    @Transactional
    public void testMergeStopPlacesWithAlternativeNames() {

        StopPlace fromStopPlace = new StopPlace();
        fromStopPlace.setName(new EmbeddableMultilingualString("Name"));

        Set<TariffZoneRef> fromTzSet = new HashSet<>();
        TariffZoneRef fromTz = new TariffZoneRef();
        fromTz.setRef("NSR:TZ:1");
        fromTz.setVersion("1");
        fromTzSet.add(fromTz);
        fromStopPlace.setTariffZones(fromTzSet);

        AlternativeName fromAlternativeName = new AlternativeName();
        fromAlternativeName.setName(new EmbeddableMultilingualString("FROM-alternative"));
        fromStopPlace.getAlternativeNames().add(fromAlternativeName);

        StopPlace toStopPlace = new StopPlace();
        toStopPlace.setName(new EmbeddableMultilingualString("Name 2"));

        Set<TariffZoneRef> toTzSet = new HashSet<>();
        TariffZoneRef toTz = new TariffZoneRef();
        toTz.setRef("NSR:TZ:2");
        toTz.setVersion("2");
        toTzSet.add(toTz);
        toStopPlace.setTariffZones(toTzSet);

        AlternativeName toAlternativeName = new AlternativeName();
        toAlternativeName.setName(new EmbeddableMultilingualString("TO-alternative"));
        toStopPlace.getAlternativeNames().add(toAlternativeName);

        stopPlaceVersionedSaverService.saveNewVersion(fromStopPlace);
        stopPlaceVersionedSaverService.saveNewVersion(toStopPlace);

        StopPlace mergedStopPlace = stopPlaceQuayMerger.mergeStopPlaces(fromStopPlace.getNetexId(), toStopPlace.getNetexId(), null, null, false);

        //AlternativeName
        assertThat(mergedStopPlace.getAlternativeNames()).isNotNull();
        assertThat(mergedStopPlace.getAlternativeNames()).hasSize(2);
    }
}
