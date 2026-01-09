package org.rutebanken.tiamat.service.batch;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.AccessibilityAssessment;
import org.rutebanken.tiamat.model.AccessibilityLimitation;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.LimitationStatusEnumeration;
import org.rutebanken.tiamat.model.PrivateCodeStructure;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.TariffZone;
import org.rutebanken.tiamat.model.TariffZoneRef;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.model.VehicleModeEnumeration;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(locations = "classpath:application-ISOLATEDMODE.properties")
public class MissingInseeCodeServiceTest extends TiamatIntegrationTest {

    double longitude = 1.885889;
    double latitude = 48.695513;
    String importedId = "PROV1:StopPlace:stop1";
    String name = "stop1";
    String quayImportedId = "PROV1:Quay:quay1";
    String provider = "PROV1";


    @Autowired
    private MissingInseeCodeService missingInseeCodeService;

    @Test
    public void getMissingInseeCodeQuays() {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");
        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);
        missingInseeCodeService.getMissingInseeCode();

        //StopPlace checks
        List<StopPlace> stopPlaces =  stopPlaceRepository.findAll();
        StopPlace oldStopPlaceSaved = stopPlaceRepository.findByNetexIdByVersionAndInitialize(stopPlaces.get(0).getNetexId(), 1L);
        StopPlace newStopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        Assertions.assertEquals(1, newStopPlaceSaved.getQuays().size());
        assertNotNull(newStopPlaceSaved.getQuays().stream().findFirst().get().getInseeCode());
        assertNotNull(oldStopPlaceSaved.getValidBetween().getToDate());

    }

    private StopPlace createStopPlace(String name, double longitude, double latitude, String importedId, String provider, AccessibilityAssessment assessment, TariffZoneRef tariffZoneRef, PrivateCodeStructure privateCodeStructure) {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setCentroid(createPoint(longitude, latitude));
        stopPlace.setName(new EmbeddableMultilingualString(name, "FR"));
        stopPlace.setStopPlaceType(StopTypeEnumeration.ONSTREET_BUS);
        stopPlace.setTransportMode(VehicleModeEnumeration.BUS);
        stopPlace.setPrivateCode(privateCodeStructure);
        Value value = new Value(importedId);
        stopPlace.getKeyValues().put(NetexIdMapper.ORIGINAL_ID_KEY, value);
        stopPlace.getTariffZones().add(tariffZoneRef);
        stopPlace.setAccessibilityAssessment(assessment);
        stopPlace.setProvider(provider);
        return stopPlace;
    }

    private Point createPoint(double longitude, double latitude) {
        return
                geometryFactory.createPoint(
                        new Coordinate(longitude, latitude));
    }

    private Quay createQuay(String name, double longitude, double latitude, String importedId) {
        Quay quay = new Quay();
        quay.setName(new EmbeddableMultilingualString(name, "FR"));
        quay.setCentroid(createPoint(longitude, latitude));
        Value value = new Value(importedId);
        quay.getKeyValues().put(NetexIdMapper.ORIGINAL_ID_KEY, value);

        Value importedNameValue = new Value(name);
        quay.getKeyValues().put(NetexIdMapper.ORIGINAL_NAME_KEY, importedNameValue);
        return quay;
    }

    private StopPlace createStopPlaceWithQuay(String name, double longitude, double latitude, String stopPlaceId, String quayId, String provider, AccessibilityAssessment assessment, TariffZoneRef tariffZoneRef, PrivateCodeStructure privateCodeStructure) {
        StopPlace stopPlace = createStopPlace(name, longitude, latitude, stopPlaceId, provider, assessment, tariffZoneRef, privateCodeStructure);
        stopPlace.getQuays().add(createQuay(name, longitude, latitude, quayId));
        return stopPlace;
    }

    @NotNull
    private AccessibilityAssessment setupAccessibility(LimitationStatusEnumeration limitationStatusEnumeration) {
        AccessibilityAssessment assessment = new AccessibilityAssessment();
        AccessibilityLimitation limitation = new AccessibilityLimitation();
        limitation.setWheelchairAccess(limitationStatusEnumeration);
        assessment.setLimitations(List.of(limitation));
        return assessment;
    }

    @NotNull
    private TariffZoneRef setupTariffZone(String ref) {
        Value tariffZoneValue = new Value(ref);
        TariffZoneRef tariffZoneRef = new TariffZoneRef();
        tariffZoneRef.setRef(ref);
        tariffZoneRef.setVersion("1");
        TariffZone tariffZone = new TariffZone();
        tariffZone.getKeyValues().put(NetexIdMapper.FARE_ZONE, tariffZoneValue);
        tariffZoneRepository.save(tariffZone);
        return tariffZoneRef;
    }

    @NotNull
    private PrivateCodeStructure setupPrivateCode(String testValuePrivateCode, String testTypePrivateCode) {
        PrivateCodeStructure privateCodeStructure = new PrivateCodeStructure();
        privateCodeStructure.setValue(testValuePrivateCode);
        privateCodeStructure.setType(testTypePrivateCode);
        return privateCodeStructure;
    }

    private void saveStopPlace(StopPlace stopPlace) {
        stopPlaceVersionedSaverService.saveNewVersion(stopPlace);
    }
}