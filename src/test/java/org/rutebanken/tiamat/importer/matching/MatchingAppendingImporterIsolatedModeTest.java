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


import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;


@TestPropertySource(locations="classpath:application-ISOLATEDMODE.properties")
public class MatchingAppendingImporterIsolatedModeTest extends TiamatIntegrationTest {

    @Autowired
    private TransactionalMatchingAppendingStopPlaceImporter importer;


    @Test
    public void importSimpleStop() throws TiamatBusinessException {

        double longitude = 1.885889;
        double latitude = 48.695513;
        String importedId = "PROV1:StopPlace:stop1";
        String name = "stop1";
        String quayImportedId = "PROV1:Quay:quay1";
        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude,latitude,importedId,quayImportedId);
        stopPlace.setProvider("PROV1");

        List<org.rutebanken.netex.model.StopPlace > matchedStopPlaces = new ArrayList<>();

        AtomicInteger counter = new AtomicInteger();
        importer.findAppendAndAdd(stopPlace,matchedStopPlaces,counter,false);


        //StopPlace checks
        assertTrue(matchedStopPlaces.size() == 1);
        org.rutebanken.netex.model.StopPlace importedStopPlace = matchedStopPlaces.get(0);
        assertEquals(name,importedStopPlace.getName().getValue());
        assertEquals("Wrong longitude",importedStopPlace.getCentroid().getLocation().getLongitude().doubleValue(),longitude,0.0d);
        assertEquals("Wrong latitude",importedStopPlace.getCentroid().getLocation().getLatitude().doubleValue(),latitude,0.0d);
        Optional<String> importedIdOpt = NetexMapper.getImportedId(importedStopPlace);
        assertTrue(importedIdOpt.isPresent());
        assertEquals(importedId,importedIdOpt.get());

        //Quay checks
        assertTrue(importedStopPlace.getQuays().getQuayRefOrQuay().size() == 1);
        org.rutebanken.netex.model.Quay quay1 = (org.rutebanken.netex.model.Quay) importedStopPlace.getQuays().getQuayRefOrQuay().get(0);
        assertEquals(name,quay1.getName().getValue());
        assertEquals("Wrong longitude",quay1.getCentroid().getLocation().getLongitude().doubleValue(),longitude,0.0d);
        assertEquals("Wrong latitude",quay1.getCentroid().getLocation().getLatitude().doubleValue(),latitude,0.0d);
        Optional<String> quayImportedIdOpt = NetexMapper.getImportedId(quay1);
        assertTrue(quayImportedIdOpt.isPresent());
        assertEquals(quayImportedId,quayImportedIdOpt.get());
    }


    @Test
    public void checkStopPlaceNOTRecoveredFromAnotherProvider() throws TiamatBusinessException {
        List<org.rutebanken.netex.model.StopPlace > matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();

        //Import a first point on a first provider
        double longitude = 1.985889;
        double latitude = 48.595513;
        String importedId = "PROV1:StopPlace:stop3";
        String name = "stopName";
        String quayImportedId = "PROV1:Quay:quay3";
        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude,latitude,importedId,quayImportedId);
        stopPlace.setProvider("PROV1");
        importer.findAppendAndAdd(stopPlace,matchedStopPlaces,counter,false);

        assertTrue(matchedStopPlaces.size() == 1);
        org.rutebanken.netex.model.StopPlace importedStopPlaceOnProv1 = matchedStopPlaces.get(0);

        matchedStopPlaces.clear();

        //Import a second point on another provider
        //Expected : on "isolated" mode, TIAMAT must NOT recover the point integrated previously on provider PROV1
        String importedIdPt2 = "PROV2:StopPlace:stop4";
        String quayImportedIdPt2 = "PROV2:Quay:quay4";
        StopPlace stopPlacePt2 = createStopPlaceWithQuay(name, longitude,latitude,importedIdPt2,quayImportedIdPt2);
        stopPlacePt2.setProvider("PROV2");
        importer.findAppendAndAdd(stopPlacePt2,matchedStopPlaces,counter,false);

        assertTrue(matchedStopPlaces.size() == 1);
        org.rutebanken.netex.model.StopPlace importedStopPlaceOnProv2 = matchedStopPlaces.get(0);
        assertNotEquals(importedStopPlaceOnProv1.getId(),importedStopPlaceOnProv2.getId());
        Optional<String> stop2Opt = NetexMapper.getImportedId(importedStopPlaceOnProv2);
        assertTrue(stop2Opt.isPresent());
        //The new point must contain in "imported-id" value both values from point1 ID + point2 ID
        assertEquals(importedIdPt2,stop2Opt.get());

        assertTrue(importedStopPlaceOnProv2.getQuays().getQuayRefOrQuay().size() == 1);
        org.rutebanken.netex.model.Quay newQuay = (org.rutebanken.netex.model.Quay) importedStopPlaceOnProv2.getQuays().getQuayRefOrQuay().get(0);
        assertEquals("Wrong longitude",newQuay.getCentroid().getLocation().getLongitude().doubleValue(),longitude,0.0d);
        assertEquals("Wrong latitude",newQuay.getCentroid().getLocation().getLatitude().doubleValue(),latitude,0.0d);
        Optional<String> newQuayImportedIdOpt = NetexMapper.getImportedId(newQuay);
        assertTrue(newQuayImportedIdOpt.isPresent());
        assertEquals(quayImportedIdPt2,newQuayImportedIdOpt.get());
    }

    @Test
    @Ignore //in isolated mode, no nearby recovery is done, even for the same provider
    public void checkStopPlaceRecoveredFromSameProvider() throws TiamatBusinessException {
        List<org.rutebanken.netex.model.StopPlace > matchedStopPlaces = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();

        //Import a first point on a first provider
        double longitude = 1.885889;
        double latitude = 48.695513;
        String importedId = "PROV1:StopPlace:stop1";
        String name = "stopName";
        String quayImportedId = "PROV1:Quay:quay1";
        StopPlace stopPlace = createStopPlaceWithQuay(name, longitude,latitude,importedId,quayImportedId);
        stopPlace.setProvider("PROV1");
        importer.findAppendAndAdd(stopPlace,matchedStopPlaces,counter,false);

        assertTrue(matchedStopPlaces.size() == 1);
        org.rutebanken.netex.model.StopPlace importedStopPlaceOnProv1 = matchedStopPlaces.get(0);

        matchedStopPlaces.clear();

        //Import a second point on another provider
        //Expected : TIAMAT must recover previously imported point on the same provider
        String importedIdPt2 = "PROV1:StopPlace:stop2";
        String quayImportedIdPt2 = "PROV1:Quay:quay2";
        StopPlace stopPlacePt2 = createStopPlaceWithQuay(name, longitude,latitude,importedIdPt2,quayImportedIdPt2);
        stopPlacePt2.setProvider("PROV1");
        importer.findAppendAndAdd(stopPlacePt2,matchedStopPlaces,counter,false);

        assertTrue(matchedStopPlaces.size() == 1);
        org.rutebanken.netex.model.StopPlace importedStopPlaceOnProv2 = matchedStopPlaces.get(0);
        assertEquals(importedStopPlaceOnProv1.getId(),importedStopPlaceOnProv2.getId());
        Optional<String> stop2Opt = NetexMapper.getImportedId(importedStopPlaceOnProv2);
        assertTrue(stop2Opt.isPresent());
        //The new point must contain in "imported-id" value both values from point1 ID + point2 ID
        assertEquals(importedId+","+importedIdPt2,stop2Opt.get());

        assertTrue(importedStopPlaceOnProv2.getQuays().getQuayRefOrQuay().size() == 1);
        org.rutebanken.netex.model.Quay newQuay = (org.rutebanken.netex.model.Quay) importedStopPlaceOnProv2.getQuays().getQuayRefOrQuay().get(0);
        assertEquals("Wrong longitude",newQuay.getCentroid().getLocation().getLongitude().doubleValue(),longitude,0.0d);
        assertEquals("Wrong latitude",newQuay.getCentroid().getLocation().getLatitude().doubleValue(),latitude,0.0d);
        Optional<String> newQuayImportedIdOpt = NetexMapper.getImportedId(newQuay);
        assertTrue(newQuayImportedIdOpt.isPresent());
        assertEquals(quayImportedId+","+quayImportedIdPt2,newQuayImportedIdOpt.get());
    }


    private StopPlace createStopPlaceWithQuay(String name, double longitude, double latitude, String stopPlaceId, String quayId) {
        StopPlace stopPlace = createStopPlace(name, longitude, latitude, stopPlaceId);
        stopPlace.getQuays().add(createQuay(name, longitude, latitude, quayId));
        return stopPlace;
    }

    private StopPlace createStopPlace(String name, double longitude, double latitude, String importedId) {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setCentroid(createPoint(longitude, latitude));
        stopPlace.setName(new EmbeddableMultilingualString(name, "FR"));
        stopPlace.setStopPlaceType(StopTypeEnumeration.ONSTREET_BUS);
        Value value = new Value(importedId);
        stopPlace.getKeyValues().put(NetexIdMapper.ORIGINAL_ID_KEY,value);
        return stopPlace;
    }

    private Quay createQuay(String name, double longitude, double latitude, String importedId) {
        Quay quay = new Quay();
        quay.setName(new EmbeddableMultilingualString(name, "FR"));
        quay.setCentroid(createPoint(longitude, latitude));
        Value value = new Value(importedId);
        quay.getKeyValues().put(NetexIdMapper.ORIGINAL_ID_KEY,value);

        Value importedNameValue = new Value(name);
        quay.getKeyValues().put(NetexIdMapper.ORIGINAL_NAME_KEY,importedNameValue);
        return quay;
    }

    private Point createPoint(double longitude, double latitude) {
        return
                geometryFactory.createPoint(
                        new Coordinate(longitude, latitude));
    }
}