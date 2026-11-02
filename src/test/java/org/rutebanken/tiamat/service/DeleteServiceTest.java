package org.rutebanken.tiamat.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.ParkingAreasRepository;
import org.rutebanken.tiamat.repository.ParkingBaysRepository;
import org.rutebanken.tiamat.repository.ParkingPlaceEquipmentsRepository;
import org.rutebanken.tiamat.service.delete.DeleteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DeleteServiceTest extends TiamatIntegrationTest {

    @Autowired
    private DeleteService deleteService;
    @Autowired
    private ParkingAreasRepository parkingAreasRepository;
    @Autowired
    private ParkingBaysRepository parkingBaysRepository;
    @Autowired
    private ParkingPlaceEquipmentsRepository parkingPlaceEquipmentsRepository;

    @Test
    public void deleteAllPoi() throws Exception {

    }

    private void newParking() {
        StopPlace stopPlace = stopPlaceRepository.saveAndFlush(new StopPlace());

        Parking parking = parkingRepository.save(new Parking());
        parking.setVersion(1L);
        parking.setParentSiteRef(new SiteRefStructure(stopPlace.getNetexId()));

        parking.getKeyValues().put("clé",new Value("valeur1","valeur2"));

        parking.setPlaceEquipments(parkingPlaceEquipmentsRepository.save(new PlaceEquipment()));

        ArrayList<ParkingArea> areas = new ArrayList<>();

        ParkingArea area = parkingAreasRepository.save(new ParkingArea());
        area.getKeyValues().put("clé",new Value("valeur1","valeur2"));
        area.setPlaceEquipments(parkingPlaceEquipmentsRepository.save(new PlaceEquipment()));

        ArrayList<ParkingBay> bays = new ArrayList<>();
        ParkingBay bay = parkingBaysRepository.save(new ParkingBay());
        bays.add(bay);
        bay.getKeyValues().put("clé",new Value("valeur1","valeur2"));
        bay.setParkingArea(area);
        bay.setPlaceEquipments(parkingPlaceEquipmentsRepository.save(new PlaceEquipment()));

        area.setBays(bays);
        areas.add(area);

        parking.setParkingAreas(areas);
    }

    @Test
    @Transactional
    public void deleteAllParkings() throws Exception {
        for(int i=0; i<100;i++) {
            newParking();
        }

        List<Parking> parkings = parkingRepository.findAll();
        assertThat(parkings).isNotEmpty();
        List<ParkingBay> parkingBays = parkingBaysRepository.findAll();
        assertThat(parkingBays).isNotEmpty();
        List<ParkingArea> parkingAreas = parkingAreasRepository.findAll();
        assertThat(parkingAreas).isNotEmpty();

        deleteService.deleteAllParkings();

        parkings = parkingRepository.findAll();
        assertThat(parkings).isEmpty();

        parkingAreas = parkingAreasRepository.findAll();
        assertThat(parkingAreas).isEmpty();

        parkingBays = parkingBaysRepository.findAll();
        assertThat(parkingBays).isEmpty();
    }
}
