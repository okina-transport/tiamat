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

package org.rutebanken.tiamat.importer.matching;


import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.importer.ImportParams;
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
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(locations = "classpath:application-ISOLATEDMODE.properties")
public class MatchingAppendingImporterIsolatedModeTest extends TiamatIntegrationTest {

    @Autowired
    private TransactionalMatchingAppendingStopPlaceImporter importer;

    double longitude = 1.885889;
    double latitude = 48.695513;
    String importedId = "PROV1:StopPlace:stop1";
    String name = "stop1";
    String quayImportedId = "PROV1:Quay:quay1";
    String provider = "PROV1";


    @Test
    public void importSimpleStop() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");
        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        ImportParams params = buildImportParams(false, false, false, false);
        importer.findAppendAndAdd(stopPlace, matchedStopPlaces, counter, params);


        //StopPlace checks
        assertEquals(1, matchedStopPlaces.size());
        org.rutebanken.netex.model.StopPlace importedStopPlace = matchedStopPlaces.get(0);
        assertEquals(name, importedStopPlace.getName().getValue());
        assertEquals("Wrong longitude", longitude, importedStopPlace.getCentroid().getLocation().getLongitude().doubleValue(), 0.0d);
        assertEquals("Wrong latitude", latitude, importedStopPlace.getCentroid().getLocation().getLatitude().doubleValue(), 0.0d);
        Optional<String> importedIdOpt = NetexMapper.getImportedId(importedStopPlace);
        assertTrue(importedIdOpt.isPresent());
        assertEquals(importedId, importedIdOpt.get());

        //Quay checks
        assertEquals(1, importedStopPlace.getQuays().getQuayRefOrQuay().size());
        org.rutebanken.netex.model.Quay quay1 = (org.rutebanken.netex.model.Quay) importedStopPlace.getQuays().getQuayRefOrQuay().get(0).getValue();
        assertEquals(name, quay1.getName().getValue());
        assertEquals("Wrong longitude", longitude, quay1.getCentroid().getLocation().getLongitude().doubleValue(), 0.0d);
        assertEquals("Wrong latitude", latitude, quay1.getCentroid().getLocation().getLatitude().doubleValue(), 0.0d);
        Optional<String> quayImportedIdOpt = NetexMapper.getImportedId(quay1);
        assertTrue(quayImportedIdOpt.isPresent());
        assertEquals(quayImportedId, quayImportedIdOpt.get());
    }


    @Test
    public void checkStopPlaceNOTRecoveredFromAnotherProvider() throws TiamatBusinessException {
        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();

        //Import a first point on a first provider
        double longitude = 1.985889;
        double latitude = 48.595513;
        String importedId = "PROV1:StopPlace:stop3";
        String name = "stopName";
        String quayImportedId = "PROV1:Quay:quay3";

        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        ImportParams params = buildImportParams(false, false, false, false);
        importer.findAppendAndAdd(stopPlace, matchedStopPlaces, counter, params);

        assertEquals(1, matchedStopPlaces.size());
        org.rutebanken.netex.model.StopPlace importedStopPlaceOnProv1 = matchedStopPlaces.get(0);

        matchedStopPlaces.clear();

        //Import a second point on another provider
        //Expected : on "isolated" mode, TIAMAT must NOT recover the point integrated previously on provider PROV1
        String importedIdPt2 = "PROV2:StopPlace:stop4";
        String quayImportedIdPt2 = "PROV2:Quay:quay4";
        AccessibilityAssessment assessment1 = setupAccessibility(LimitationStatusEnumeration.FALSE);
        StopPlace stopPlacePt2 = createStopPlaceWithQuay(name, longitude, latitude, importedIdPt2, quayImportedIdPt2, "PROV2", assessment1, tariffZoneRef, privateCodeStructure);
        importer.findAppendAndAdd(stopPlacePt2, matchedStopPlaces, counter, params);

        assertEquals(1, matchedStopPlaces.size());
        org.rutebanken.netex.model.StopPlace importedStopPlaceOnProv2 = matchedStopPlaces.get(0);
        assertNotEquals(importedStopPlaceOnProv1.getId(), importedStopPlaceOnProv2.getId());
        Optional<String> stop2Opt = NetexMapper.getImportedId(importedStopPlaceOnProv2);
        assertTrue(stop2Opt.isPresent());
        //The new point must contain in "imported-id" value both values from point1 ID + point2 ID
        assertEquals(importedIdPt2, stop2Opt.get());

        assertEquals(1, importedStopPlaceOnProv2.getQuays().getQuayRefOrQuay().size());
        org.rutebanken.netex.model.Quay newQuay = (org.rutebanken.netex.model.Quay) importedStopPlaceOnProv2.getQuays().getQuayRefOrQuay().get(0).getValue();
        assertEquals("Wrong longitude", longitude, newQuay.getCentroid().getLocation().getLongitude().doubleValue(), 0.0d);
        assertEquals("Wrong latitude", latitude, newQuay.getCentroid().getLocation().getLatitude().doubleValue(), 0.0d);
        Optional<String> newQuayImportedIdOpt = NetexMapper.getImportedId(newQuay);
        assertTrue(newQuayImportedIdOpt.isPresent());
        assertEquals(quayImportedIdPt2, newQuayImportedIdOpt.get());
    }


    @Test
    @Ignore //in isolated mode, no nearby recovery is done, even for the same provider
    public void checkStopPlaceRecoveredFromSameProvider() throws TiamatBusinessException {
        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();

        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);

        ImportParams params = buildImportParams(false, false, false, false);
        importer.findAppendAndAdd(stopPlace, matchedStopPlaces, counter, params);

        assertEquals(1, matchedStopPlaces.size());
        org.rutebanken.netex.model.StopPlace importedStopPlaceOnProv1 = matchedStopPlaces.get(0);

        matchedStopPlaces.clear();

        //Import a second point on another provider
        //Expected : TIAMAT must recover previously imported point on the same provider
        String importedIdPt2 = "PROV1:StopPlace:stop2";
        String quayImportedIdPt2 = "PROV1:Quay:quay2";
        StopPlace stopPlacePt2 = createStopPlaceWithQuay(name, longitude, latitude, importedIdPt2, quayImportedIdPt2, provider, assessment, tariffZoneRef, privateCodeStructure);

        importer.findAppendAndAdd(stopPlacePt2, matchedStopPlaces, counter, params);

        assertEquals(1, matchedStopPlaces.size());
        org.rutebanken.netex.model.StopPlace importedStopPlaceOnProv2 = matchedStopPlaces.get(0);
        assertEquals(importedStopPlaceOnProv1.getId(), importedStopPlaceOnProv2.getId());
        Optional<String> stop2Opt = NetexMapper.getImportedId(importedStopPlaceOnProv2);
        assertTrue(stop2Opt.isPresent());
        //The new point must contain in "imported-id" value both values from point1 ID + point2 ID
        assertEquals(importedId + "," + importedIdPt2, stop2Opt.get());

        assertEquals(1, importedStopPlaceOnProv2.getQuays().getQuayRefOrQuay().size());
        org.rutebanken.netex.model.Quay newQuay = (org.rutebanken.netex.model.Quay) importedStopPlaceOnProv2.getQuays().getQuayRefOrQuay().get(0).getValue();
        assertEquals("Wrong longitude", longitude, newQuay.getCentroid().getLocation().getLongitude().doubleValue(), 0.0d);
        assertEquals("Wrong latitude", latitude, newQuay.getCentroid().getLocation().getLatitude().doubleValue(), 0.0d);
        Optional<String> newQuayImportedIdOpt = NetexMapper.getImportedId(newQuay);
        assertTrue(newQuayImportedIdOpt.isPresent());
        assertEquals(quayImportedId + "," + quayImportedIdPt2, newQuayImportedIdOpt.get());
    }

    @Test
    public void testNoChanges_ShouldNotCreateNewVersion() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        ImportParams params = buildImportParams(false, false, false, false);

        StopPlace stopPlaceIncoming = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        importer.findAppendAndAdd(stopPlaceIncoming, matchedStopPlaces, counter, params);

        //StopPlace checks
        List<StopPlace> stopPlaces =  stopPlaceRepository.findAll();
        assertEquals(1, stopPlaces.size());
        assertEquals(1, stopPlaces.get(0).getVersion());

        //Quay checks
        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(1, stopPlaceSaved.getQuays().size());
        assertEquals(1, stopPlaceSaved.getQuays().stream().findFirst().get().getVersion());
    }

    @Test
    public void testStopPlaceNameChange_ShouldCreateNewVersion() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        ImportParams params = buildImportParams(false, false, false, false);

        String name2 = "stop2";
        StopPlace stopPlaceIncoming = createStopPlaceWithQuay(name2, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        importer.findAppendAndAdd(stopPlaceIncoming, matchedStopPlaces, counter, params);

        //StopPlace checks
        List<StopPlace> stopPlaces =  stopPlaceRepository.findAll();
        assertEquals(2, stopPlaces.size());

        //Quay checks
        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(2, stopPlaceSaved.getVersion());
        assertEquals(1, stopPlaceSaved.getQuays().size());
        assertEquals(2, stopPlaceSaved.getQuays().stream().findFirst().get().getVersion());
    }

    @Test
    public void testStopPlaceNameChangeKeepStopNamesTrue_ShouldNotCreateNewVersion() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        ImportParams params = buildImportParams(false, false, true, false);

        String name2 = "stop2";
        StopPlace stopPlaceIncoming = createStopPlaceWithQuay(name2, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        importer.findAppendAndAdd(stopPlaceIncoming, matchedStopPlaces, counter, params);

        //StopPlace checks
        List<StopPlace> stopPlaces =  stopPlaceRepository.findAll();
        assertEquals(1, stopPlaces.size());
        assertEquals(1, stopPlaces.get(0).getVersion());

        //Quay checks
        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(1, stopPlaceSaved.getQuays().size());
        assertEquals(1, stopPlaceSaved.getQuays().stream().findFirst().get().getVersion());
    }


    @Test
    public void testCentroidChange_ShouldCreateNewVersion() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        ImportParams params = buildImportParams(false, false, false, false);

        double longitude2 = 1.885889;
        double latitude2 = 48.795513;
        StopPlace stopPlaceIncoming = createStopPlaceWithQuay(name, longitude2, latitude2, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        importer.findAppendAndAdd(stopPlaceIncoming, matchedStopPlaces, counter, params);

        //StopPlace checks
        List<StopPlace> stopPlaces =  stopPlaceRepository.findAll();
        assertEquals(2, stopPlaces.size());

        //Quay checks
        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(2, stopPlaceSaved.getVersion());
        assertEquals(1, stopPlaceSaved.getQuays().size());
        assertEquals(2, stopPlaceSaved.getQuays().stream().findFirst().get().getVersion());
    }

    @Test
    public void testCentroidChangeKeepStopGeolocTrue_ShouldNotCreateNewVersion() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        ImportParams params = buildImportParams(true, false, false, false);

        double longitude2 = 1.885889;
        double latitude2 = 48.795513;
        StopPlace stopPlaceIncoming = createStopPlaceWithQuay(name, longitude2, latitude2, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        importer.findAppendAndAdd(stopPlaceIncoming, matchedStopPlaces, counter, params);

        //StopPlace checks
        List<StopPlace> stopPlaces =  stopPlaceRepository.findAll();
        assertEquals(1, stopPlaces.size());
        assertEquals(1, stopPlaces.get(0).getVersion());

        //Quay checks
        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(1, stopPlaceSaved.getQuays().size());
        assertEquals(1, stopPlaceSaved.getQuays().stream().findFirst().get().getVersion());
    }

    @Test
    public void testAccessibilityChange_ShouldNotCreateNewVersion() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        ImportParams params = buildImportParams(false, false, false, false);

        AccessibilityAssessment assessment1 = setupAccessibility(LimitationStatusEnumeration.FALSE);
        StopPlace stopPlaceIncoming = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment1, tariffZoneRef, privateCodeStructure);
        importer.findAppendAndAdd(stopPlaceIncoming, matchedStopPlaces, counter, params);

        //StopPlace checks
        List<StopPlace> stopPlaces =  stopPlaceRepository.findAll();
        assertEquals(1, stopPlaces.size());
        assertEquals(1, stopPlaces.get(0).getVersion());

        //Quay checks
        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(1, stopPlaceSaved.getQuays().size());
        assertEquals(1, stopPlaceSaved.getQuays().stream().findFirst().get().getVersion());
    }

    @Test
    public void testAccessibilityChangeStopAccessibilityTrue_ShouldCreateNewVersion() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        ImportParams params = buildImportParams(false, true, false, false);

        AccessibilityAssessment assessment1 = setupAccessibility(LimitationStatusEnumeration.TRUE);
        StopPlace stopPlaceIncoming = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment1, tariffZoneRef, privateCodeStructure);
        importer.findAppendAndAdd(stopPlaceIncoming, matchedStopPlaces, counter, params);


        //StopPlace checks
        List<StopPlace> stopPlaces =  stopPlaceRepository.findAll();
        assertEquals(2, stopPlaces.size());

        //Quay checks
        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(2, stopPlaceSaved.getVersion());
        assertEquals(1, stopPlaceSaved.getQuays().size());
        assertEquals(2, stopPlaceSaved.getQuays().stream().findFirst().get().getVersion());
    }

    @Test
    public void testTariffZoneChange_ShouldCreateNewVersion() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        ImportParams params = buildImportParams(false, false, false, false);

        TariffZoneRef tariffZoneRef1 = setupTariffZone("TEST1");
        StopPlace stopPlaceIncoming = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef1, privateCodeStructure);
        importer.findAppendAndAdd(stopPlaceIncoming, matchedStopPlaces, counter, params);

        //StopPlace checks
        List<StopPlace> stopPlaces =  stopPlaceRepository.findAll();
        assertEquals(2, stopPlaces.size());

        //Quay checks
        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(2, stopPlaceSaved.getVersion());
        assertEquals(1, stopPlaceSaved.getQuays().size());
        assertEquals(2, stopPlaceSaved.getQuays().stream().findFirst().get().getVersion());
    }

    @Test
    public void testPrivateCodeChange_ShouldCreateNewVersion() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        ImportParams params = buildImportParams(false, false, false, false);

        PrivateCodeStructure privateCodeStructure1 = setupPrivateCode("TEST_VALUE_PRIVATE_CODE_1", "TEST_TYPE_PRIVATE_CODE_1");
        StopPlace stopPlaceIncoming = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure1);
        importer.findAppendAndAdd(stopPlaceIncoming, matchedStopPlaces, counter, params);

        //StopPlace checks
        List<StopPlace> stopPlaces =  stopPlaceRepository.findAll();
        assertEquals(2, stopPlaces.size());

        //Quay checks
        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(2, stopPlaceSaved.getVersion());
        assertEquals(1, stopPlaceSaved.getQuays().size());
        assertEquals(2, stopPlaceSaved.getQuays().stream().findFirst().get().getVersion());
    }

    @Test
    public void testTransportModeChange_ShouldCreateNewVersion() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("TEST_VALUE_PRIVATE_CODE", "TEST_TYPE_PRIVATE_CODE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        ImportParams params = buildImportParams(false, false, false, false);

        StopPlace stopPlaceIncoming = createStopPlaceWithQuay(name, longitude, latitude, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);
        stopPlaceIncoming.setStopPlaceType(StopTypeEnumeration.TRAM_STATION);
        stopPlaceIncoming.setTransportMode(VehicleModeEnumeration.TRAM);
        importer.findAppendAndAdd(stopPlaceIncoming, matchedStopPlaces, counter, params);

        //StopPlace checks
        List<StopPlace> stopPlaces =  stopPlaceRepository.findAll();
        assertEquals(2, stopPlaces.size());

        //Quay checks
        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(2, stopPlaceSaved.getVersion());
        assertEquals(1, stopPlaceSaved.getQuays().size());
        assertEquals(2, stopPlaceSaved.getQuays().stream().findFirst().get().getVersion());
    }

    @Test
    public void testCentroidRecomputeFromQuays_ShouldPrioritizeChildrenLocation() throws TiamatBusinessException {
        double originalLat = 0.0;
        double originalLon = 0.0;

        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("CODE", "TYPE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithQuay(name, originalLon, originalLat, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);

        Quay existingQuay = stopPlace.getQuays().iterator().next();
        double childLat = 10.0;
        double childLon = 10.0;
        existingQuay.setCentroid(createPoint(childLon, childLat));

        saveStopPlace(stopPlace);

        List<org.rutebanken.netex.model.StopPlace> matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();

        double incomingLat = 20.0;
        double incomingLon = 20.0;
        StopPlace stopPlaceIncoming = createStopPlaceWithQuay(name, incomingLon, incomingLat, importedId, quayImportedId, provider, assessment, tariffZoneRef, privateCodeStructure);

        ImportParams params = buildImportParams(false, false, false, true);

        importer.findAppendAndAdd(stopPlaceIncoming, matchedStopPlaces, counter, params);

        List<StopPlace> stopPlaces = stopPlaceRepository.findAll();
        assertEquals(2, stopPlaces.size());

        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(2, stopPlaceSaved.getVersion());

        assertEquals(incomingLat, stopPlaceSaved.getCentroid().getCoordinate().y, 0.0001);
        assertEquals(incomingLon, stopPlaceSaved.getCentroid().getCoordinate().x, 0.0001);
    }

    @Test
    public void testCentroidKeepGeolocTrue_ShouldDoNothingEvenWithMultipleQuays() throws TiamatBusinessException {
        double originalLon = 0.0;
        double originalLat = 0.0;

        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("CODE", "TYPE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");


        StopPlace stopPlace = createStopPlaceWithTwoQuays(importedId, originalLon, originalLat, quayImportedId, 10.0, 10.0, quayImportedId + "2", 20.0, 20.0, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        StopPlace stopPlaceIncoming = createStopPlaceWithTwoQuays(importedId, 30.0, 30.0, quayImportedId, 40.0, 40.0, quayImportedId + "2", 50.0, 50.0, assessment, tariffZoneRef, privateCodeStructure);

        ImportParams paramsA = buildImportParams(true, false, false, false);
        importer.findAppendAndAdd(stopPlaceIncoming, new ArrayList<>(), new AtomicInteger(), paramsA);

        ImportParams paramsB = buildImportParams(true, false, false, true);
        importer.findAppendAndAdd(stopPlaceIncoming, new ArrayList<>(), new AtomicInteger(), paramsB);

        List<StopPlace> stopPlaces = stopPlaceRepository.findAll();
        assertEquals(1, stopPlaces.size());

        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(1, stopPlaceSaved.getVersion());

        assertEquals(originalLat, stopPlaceSaved.getCentroid().getCoordinate().y, 0.0001);
        assertEquals(originalLon, stopPlaceSaved.getCentroid().getCoordinate().x, 0.0001);
    }

    @Test
    public void testCentroidNoRecompute_ShouldUseIncomingParentCentroid() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("CODE", "TYPE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithTwoQuays(importedId, 0.0, 0.0, quayImportedId, 10.0, 10.0, quayImportedId + "2", 20.0, 20.0, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        double incomingLon = 30.0;
        double incomingLat = 30.0;
        StopPlace stopPlaceIncoming = createStopPlaceWithTwoQuays(importedId, incomingLon, incomingLat, quayImportedId, 40.0, 40.0, quayImportedId + "2", 50.0, 50.0, assessment, tariffZoneRef, privateCodeStructure);

        ImportParams params = buildImportParams(false, false, false, false);
        importer.findAppendAndAdd(stopPlaceIncoming, new ArrayList<>(), new AtomicInteger(), params);

        List<StopPlace> stopPlaces = stopPlaceRepository.findAll();
        assertEquals(2, stopPlaces.size());

        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(2, stopPlaceSaved.getVersion());

        assertEquals(incomingLat, stopPlaceSaved.getCentroid().getCoordinate().y, 0.0001);
        assertEquals(incomingLon, stopPlaceSaved.getCentroid().getCoordinate().x, 0.0001);
    }

    @Test
    public void testCentroidRecomputeTrue_ShouldRecalculateBarycenterFromUpdatedQuays() throws TiamatBusinessException {
        AccessibilityAssessment assessment = setupAccessibility(LimitationStatusEnumeration.FALSE);
        PrivateCodeStructure privateCodeStructure = setupPrivateCode("CODE", "TYPE");
        TariffZoneRef tariffZoneRef = setupTariffZone("TEST");

        StopPlace stopPlace = createStopPlaceWithTwoQuays(importedId, 0.0, 0.0, quayImportedId, 10.0, 10.0, quayImportedId + "2", 20.0, 20.0, assessment, tariffZoneRef, privateCodeStructure);
        saveStopPlace(stopPlace);

        double q1IncomingLon = 40.0;
        double q2IncomingLon = 50.0;
        double expectedLon = (q1IncomingLon + q2IncomingLon) / 2.0;
        double expectedLat = expectedLon;

        StopPlace stopPlaceIncoming = createStopPlaceWithTwoQuays(importedId, 30.0, 30.0, quayImportedId, q1IncomingLon, expectedLat, quayImportedId + "2", q2IncomingLon, expectedLat, assessment, tariffZoneRef, privateCodeStructure);

        ImportParams params = buildImportParams(false, false, false, true);
        importer.findAppendAndAdd(stopPlaceIncoming, new ArrayList<>(), new AtomicInteger(), params);

        List<StopPlace> stopPlaces = stopPlaceRepository.findAll();
        assertEquals(2, stopPlaces.size());

        StopPlace stopPlaceSaved = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(stopPlaces.get(0).getNetexId());
        assertEquals(2, stopPlaceSaved.getVersion());

        assertEquals(expectedLat, stopPlaceSaved.getCentroid().getCoordinate().y, 0.0001);
        assertEquals(expectedLon, stopPlaceSaved.getCentroid().getCoordinate().x, 0.0001);
    }

    private StopPlace createStopPlaceWithTwoQuays(String spId, double spLon, double spLat, String q1Id, double q1Lon, double q1Lat, String q2Id, double q2Lon, double q2Lat, AccessibilityAssessment assessment, TariffZoneRef tariffZoneRef, PrivateCodeStructure privateCodeStructure) {
        StopPlace stopPlace = createStopPlaceWithQuay(name, spLon, spLat, spId, q1Id, provider, assessment, tariffZoneRef, privateCodeStructure);

        Quay existingQuay1 = stopPlace.getQuays().iterator().next();
        existingQuay1.setCentroid(createPoint(q1Lon, q1Lat));

        Quay quay2 = createQuay(name + " Quay 2", q2Lon, q2Lat, q2Id);
        stopPlace.getQuays().add(quay2);

        return stopPlace;
    }

    private void saveStopPlace(StopPlace stopPlace) {
        stopPlaceVersionedSaverService.saveNewVersion(stopPlace);
    }

    private StopPlace createStopPlaceWithQuay(String name, double longitude, double latitude, String stopPlaceId, String quayId, String provider, AccessibilityAssessment assessment, TariffZoneRef tariffZoneRef, PrivateCodeStructure privateCodeStructure) {
        StopPlace stopPlace = createStopPlace(name, longitude, latitude, stopPlaceId, provider, assessment, tariffZoneRef, privateCodeStructure);
        stopPlace.getQuays().add(createQuay(name, longitude, latitude, quayId));
        return stopPlace;
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

    private Point createPoint(double longitude, double latitude) {
        return
                geometryFactory.createPoint(
                        new Coordinate(longitude, latitude));
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

    @NotNull
    private ImportParams buildImportParams(boolean keepStopGeolocalisation,
                                           boolean updateAccessibility,
                                           boolean keepNames,
                                           boolean recomputeStopPlacesLocation) {
        ImportParams params = new ImportParams();
        params.keepStopGeolocalisation = keepStopGeolocalisation;
        params.updateStopAccessibility = updateAccessibility;
        params.keepStopNames = keepNames;
        params.recomputeStopPlacesLocation = recomputeStopPlacesLocation;
        return params;
    }
}
