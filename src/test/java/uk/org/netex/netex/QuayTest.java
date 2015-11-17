package uk.org.netex.netex;

import no.rutebanken.tiamat.TiamatApplication;
import no.rutebanken.tiamat.repository.ifopt.LocationRepository;
import no.rutebanken.tiamat.repository.ifopt.QuayRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TiamatApplication.class)
public class QuayTest {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    public QuayRepository quayRepository;

    /**
     * Using example data from https://github.com/StichtingOpenGeo/NeTEx/blob/master/examples/functions/stopPlace/Netex_10_StopPlace_uk_ComplexStation_Wimbledon_1.xml
     * @throws ParseException
     */
    @Test
    public void persistExampleQuay() throws ParseException {

        Quay quay = new Quay();
        quay.setVersion("001");
        quay.setCreated(dateFormat.parse("2010-04-17T09:30:47Z"));
        quay.setDataSourceRef("nptg:DataSource:NaPTAN");
        quay.setResponsibilitySetRef("nptg:ResponsibilitySet:082");
        //quay.setId("napt:Quay:490000272P");

        quay.setName(new MultilingualString("Wimbledon, Stop P", "en", ""));
        quay.setShortName(new MultilingualString("Wimbledon", "en", ""));
        quay.setDescription(new MultilingualString("Stop P  is paired with Stop C outside the station", "en", ""));

        //quay.setTypes();
        roadAddress(quay);
        accessibilityAssessment(quay);

        quay.setCovered(CoveredEnumeration.COVERED);

     //   siteReference(quay);
        levelReference(quay);

        quay.setBoardingUse(true);
        quay.setAlightingUse(true);
        quay.setLabel(new MultilingualString("Stop P", "en", ""));
        quay.setPublicCode("1-2345");
        //quay.setDestinationDisplayView

        quay.setCompassOctant(CompassBearing8Enumeration.W);
        quay.setQuayType(QuayTypeEnumeration.BUS_STOP);

        quayRepository.save(quay);

        Quay actualQuay = quayRepository.findOne(quay.getId());

        assertThat(actualQuay).isNotNull();
        assertThat(actualQuay.getId()).isEqualTo(quay.getId());
    }

    @Test
    public void persistQuayWithLocation() {
        Quay quay = new Quay();
        Location location = new Location();
        BigDecimal longitude = new BigDecimal("-0.2068758371").setScale(10, BigDecimal.ROUND_CEILING);

        BigDecimal latitude = new BigDecimal("51.4207729447").setScale(10, BigDecimal.ROUND_CEILING);

        location.setLongitude(longitude);
        location.setLatitude(latitude);

        SimplePoint_VersionStructure simplePoint = new SimplePoint_VersionStructure();
        simplePoint.setLocation(location);
        quay.setCentroid(simplePoint);

        quayRepository.save(quay);
        Quay actualQuay = quayRepository.findOne(quay.getId());

        assertThat(actualQuay).isNotNull();
        assertThat(actualQuay.getCentroid()).isNotNull();
        assertThat(actualQuay.getCentroid().getLocation()).isNotNull();
        assertThat(actualQuay.getCentroid().getLocation().getLatitude()).isEqualTo(latitude);
        assertThat(actualQuay.getCentroid().getLocation().getLongitude()).isEqualTo(longitude);

    }

    private void siteReference(Quay quay) {
        //Reference to stop place
        SiteRefStructure siteRefStructure = new SiteRefStructure();
        siteRefStructure.setVersion("001");
        siteRefStructure.setValue("napt:StopPlace:490G00272P");

        quay.setSiteRef(siteRefStructure);
    }

    private void levelReference(Quay quay) {
        LevelRefStructure levelRefStructure = new LevelRefStructure();
        levelRefStructure.setVersion("001");
        levelRefStructure.setRef("tbd:Level:9100WIMBLDN_Lvl_ST");

        quay.setLevelRef(levelRefStructure);
    }

    private void accessibilityAssessment(Quay quay) {
        AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
        accessibilityAssessment.setVersion("any");
        accessibilityAssessment.setId("tbd:AccessibilityAssessment:490000272P");
        accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.TRUE);
        quay.setAccessibilityAssessment(accessibilityAssessment);
    }

    private void roadAddress(Quay quay) {
        RoadAddress roadAddress = new RoadAddress();
        roadAddress.setId("tbd:RoadAddress:Rd_Addr_03");
        roadAddress.setVersion("any");
        roadAddress.setRoadName(new MultilingualString("Wimbledon Bridge", "en", ""));
        roadAddress.setBearingCompass("W");
        quay.setRoadAddress(roadAddress);
    }
}