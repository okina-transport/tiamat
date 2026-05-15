package org.rutebanken.tiamat.netex.id;

import ma.glasnost.orika.MappingContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.netex.mapping.mapper.ParkingMapper;
import org.rutebanken.tiamat.netex.mapping.mapper.PointOfInterestMapper;
import org.springframework.security.access.method.P;

import java.util.HashMap;
import java.util.Map;

import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;


public class AlternateSuperIdTest {

    @Test
    public void testParkingSuperIdAppliedToPostalAddress() {
        ParkingMapper parkingMapper = new ParkingMapper("ALTID");

        Parking tiamatParkingToExport = new Parking();
        tiamatParkingToExport.setNetexId("ALTID:Parking:3");
        tiamatParkingToExport.getKeyValues().put(ORIGINAL_ID_KEY, new Value("PROV:Parking:toto"));
        tiamatParkingToExport.setAddress("253 rue machin");

        org.rutebanken.netex.model.Parking convertedNetex = new org.rutebanken.netex.model.Parking();
        Map<Object, Object> globalProperties = new HashMap<>();
        MappingContext mappingContext = new MappingContext(globalProperties);
        parkingMapper.mapBtoA(tiamatParkingToExport, convertedNetex, mappingContext);
        Assertions.assertTrue(convertedNetex.getPostalAddress().getId().startsWith("ALTID:PostalAddress:"));
    }

    @Test
    public void testPoiSuperIdAppliedTo()  {

        PointOfInterest tiamatPoiToExport = new PointOfInterest();
        tiamatPoiToExport.setNetexId("ALTID:PointOfInterest:1234");
        tiamatPoiToExport.setInseeCode("1234");

        org.rutebanken.netex.model.PointOfInterest convertedNetex = new org.rutebanken.netex.model.PointOfInterest();
        Map<Object, Object> globalProperties = new HashMap<>();
        MappingContext mappingContext = new MappingContext(globalProperties);
        PointOfInterestMapper poiMapper = new PointOfInterestMapper();
        poiMapper.mapBtoA(tiamatPoiToExport, convertedNetex,mappingContext);
        Assertions.assertTrue(convertedNetex.getPostalAddress().getId().startsWith("ALTID:PostalAddress:"));

    }

}
