package uk.org.netex.netex;

import no.rutebanken.tiamat.TiamatApplication;
import no.rutebanken.tiamat.repository.ifopt.AccessSpaceRepository;
import no.rutebanken.tiamat.repository.ifopt.StopPlaceRepository;
import no.rutebanken.tiamat.repository.ifopt.TariffZoneRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TiamatApplication.class)
public class StopPlaceTest {

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private TariffZoneRepository tariffZoneRepository;

    @Autowired
    private AccessSpaceRepository accessSpaceRepository;

    @Test
    public void persistStopPlace() {

        StopPlace stopPlace = new StopPlace();
        stopPlace.setPublicCode("public-code");
        MultilingualString shortName = new MultilingualString();
        shortName.setLang("no");
        shortName.setValue("Skjervik");
        stopPlace.setShortName(shortName);
        stopPlace.setPublicCode("publicCode");

        stopPlace.setStopPlaceType(StopTypeEnumeration.RAIL_STATION);

        stopPlace.setTransportMode(VehicleModeEnumeration.RAIL);
        stopPlace.setAirSubmode(AirSubmodeEnumeration.UNDEFINED);
        stopPlace.setCoachSubmode(CoachSubmodeEnumeration.REGIONAL_COACH);
        stopPlace.setFunicularSubmode(FunicularSubmodeEnumeration.UNKNOWN);
        stopPlace.getOtherTransportModes().add(VehicleModeEnumeration.AIR);
        stopPlace.setLimitedUse(LimitedUseTypeEnumeration.LONG_WALK_TO_ACCESS);

        AccessSpace accessSpace = new AccessSpace();
        accessSpace.setShortName(new MultilingualString("Østbanehallen", "no", ""));
        accessSpace.setAccessSpaceType(AccessSpaceTypeEnumeration.CONCOURSE);
        accessSpaceRepository.save(accessSpace);

        List<AccessSpace> accessSpaces = new ArrayList<>();
        accessSpaces.add(accessSpace);
        stopPlace.setAccessSpaces(accessSpaces);

        StopPlace anotherStopPlace = new StopPlace();
        anotherStopPlace.setStopPlaceType(StopTypeEnumeration.BUS_STATION);
        anotherStopPlace.setVersion("001");
        stopPlaceRepository.save(anotherStopPlace);

        StopPlaceReference stopPlaceReference = new StopPlaceReference();
        stopPlaceReference.setRef(anotherStopPlace.getId());
        stopPlaceReference.setVersion(anotherStopPlace.getVersion());

        stopPlace.setParentSiteRef(stopPlaceReference);

        TariffZone tariffZone = new TariffZone();
        tariffZone.setShortName(new MultilingualString("V2", "no", "type"));

        tariffZoneRepository.save(tariffZone);

        List<TariffZone> tariffZones = new ArrayList<>();
        tariffZones.add(tariffZone);
        stopPlace.setTariffZones(tariffZones);

        stopPlace.setWeighting(InterchangeWeightingEnumeration.RECOMMENDED_INTERCHANGE);

        stopPlaceRepository.save(stopPlace);


        StopPlace actualStopPlace = stopPlaceRepository.getOne(stopPlace.getId());

        assertThat(actualStopPlace.getPublicCode()).isEqualTo(stopPlace.getPublicCode());
        assertThat(actualStopPlace.getStopPlaceType()).isEqualTo(stopPlace.getStopPlaceType());

        assertThat(actualStopPlace.getId()).isEqualTo(stopPlace.getId());
        assertThat(actualStopPlace.getTransportMode()).isEqualTo(stopPlace.getTransportMode());
        assertThat(actualStopPlace.getAirSubmode()).isEqualTo(stopPlace.getAirSubmode());
        assertThat(actualStopPlace.getCoachSubmode()).isEqualTo(stopPlace.getCoachSubmode());
        assertThat(actualStopPlace.getFunicularSubmode()).isEqualTo(stopPlace.getFunicularSubmode());
        assertThat(actualStopPlace.getOtherTransportModes()).contains(VehicleModeEnumeration.AIR);
        assertThat(actualStopPlace.getTariffZones()).isNotEmpty();
        assertThat(actualStopPlace.getWeighting()).isEqualTo(stopPlace.getWeighting());
        assertThat(actualStopPlace.getTariffZones().get(0).getId()).isEqualTo(tariffZone.getId());
        assertThat(actualStopPlace.getParentSiteRef().getRef()).isEqualTo(anotherStopPlace.getId());
        assertThat(actualStopPlace.getLimitedUse()).isEqualTo(stopPlace.getLimitedUse());

        assertThat(actualStopPlace.getAccessSpaces().get(0)).isNotNull();
    }
}