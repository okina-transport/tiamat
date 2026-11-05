package org.rutebanken.tiamat.service;

import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.*;
import org.rutebanken.tiamat.service.delete.DeleteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    @Autowired
    private ParkingEntranceRepository parkingEntranceRepository;

    @Autowired
    private PointOfInterestRepository pointOfInterestRepository;
    @Autowired
    private PointOfInterestClassificationRepository pointOfInterestClassificationRepository;
    @Autowired
    private PointOfInterestFacilitySetRepository pointOfInterestFacilitySetRepository;
    @Autowired
    private PointOfInterestOpeningHoursRepository pointOfInterestOpeningHoursRepository;
    @Autowired
    private AccessibilityAssessmentRepository accessibilityAssessmentRepository;
    @Autowired
    private AccessibilityLimitationRepository accessibilityLimitationRepository;

    private Logger logger = Logger.getLogger(DeleteServiceTest.class);

    private void newPOI() {
        PointOfInterest poi = pointOfInterestRepository.save(new PointOfInterest());

        poi.getAlternativeNames().add(new AlternativeName());

        EquipmentPlace equipmentPlace1 = new EquipmentPlace();
        EquipmentPlace equipmentPlace2 = new EquipmentPlace();
        poi.setEquipmentPlaces( List.of(equipmentPlace1,equipmentPlace2) );

        PointOfInterestFacilitySet pointOfInterestFacilitySet = pointOfInterestFacilitySetRepository.save(new PointOfInterestFacilitySet());
        poi.setPointOfInterestFacilitySet(pointOfInterestFacilitySet);

        PointOfInterestOpeningHours pointOfInterestOpeningHours = pointOfInterestOpeningHoursRepository.save(new PointOfInterestOpeningHours());
        poi.setPointOfInterestOpeningHours(pointOfInterestOpeningHours);

        PointOfInterestClassification pointOfInterestClassification1 = pointOfInterestClassificationRepository.save(new PointOfInterestClassification());
        PointOfInterestClassification pointOfInterestClassification2 = pointOfInterestClassificationRepository.save(new PointOfInterestClassification());
        pointOfInterestClassification2.setParent(pointOfInterestClassification1);
        poi.setClassifications( Set.of(pointOfInterestClassification1,pointOfInterestClassification2) );

        AccessibilityAssessment accessibilityAssessment = accessibilityAssessmentRepository.save(new AccessibilityAssessment());
        AccessibilityLimitation accessibilityLimitation1 = accessibilityLimitationRepository.save(new AccessibilityLimitation());
        AccessibilityLimitation accessibilityLimitation2 = accessibilityLimitationRepository.save(new AccessibilityLimitation());
        accessibilityAssessment.setLimitations( List.of(accessibilityLimitation1,accessibilityLimitation2) );
        poi.setAccessibilityAssessment(accessibilityAssessment);
    }

    @Test
    public void deleteAllPoi() throws Exception {
        for(int i=0; i<100;i++) {
            newPOI();
        }

        assertThat(pointOfInterestRepository.findAll()).isNotEmpty();
        assertThat(pointOfInterestClassificationRepository.findAll()).isNotEmpty();
        assertThat(pointOfInterestFacilitySetRepository.findAll()).isNotEmpty();
        assertThat(pointOfInterestOpeningHoursRepository.findAll()).isNotEmpty();

        deleteService.deleteAllPoi();

        assertThat(pointOfInterestRepository.findAll()).isEmpty();
        assertThat(pointOfInterestClassificationRepository.findAll()).isNotEmpty(); // classifications are kept to be reused
        assertThat(pointOfInterestFacilitySetRepository.findAll()).isEmpty();
        assertThat(pointOfInterestOpeningHoursRepository.findAll()).isEmpty();
    }

    private void newParking() {
        StopPlace stopPlace = stopPlaceRepository.saveAndFlush(new StopPlace());

        Parking parking = parkingRepository.save(new Parking());
        parking.setVersion(1L);
        parking.setParentSiteRef(new SiteRefStructure(stopPlace.getNetexId()));

        ParkingEntrance entrance = parkingEntranceRepository.save(new ParkingEntrance());
        entrance.getKeyValues().put("clé",new Value("valeur1","valeur2"));
        {
            AlternativeName name = new AlternativeName();
            name.setName(new EmbeddableMultilingualString("other name"));
            entrance.getAlternativeNames().add(name);
        }

        {
            ArrayList<EquipmentPlace> equipmentPlaces = new ArrayList<>();
            equipmentPlaces.add(new EquipmentPlace());
            entrance.setEquipmentPlaces(equipmentPlaces);
        }

        {
            ArrayList<CheckConstraint> constraints = new ArrayList<>();
            constraints.add(new CheckConstraint());
            entrance.setCheckConstraints(constraints);
        }

        parking.getVehicleEntrances().add(entrance);

        parking.getKeyValues().put("clé",new Value("valeur1","valeur2"));

        {
            AlternativeName name = new AlternativeName();
            name.setName(new EmbeddableMultilingualString("other name"));
            parking.getAlternativeNames().add(name);
        }

        {
            ArrayList<EquipmentPlace> equipmentPlaces = new ArrayList<>();
            equipmentPlaces.add(new EquipmentPlace());
            parking.setEquipmentPlaces(equipmentPlaces);
        }

        ArrayList<ParkingArea> areas = new ArrayList<>();

        ParkingArea area = parkingAreasRepository.save(new ParkingArea());
        area.getKeyValues().put("clé",new Value("valeur1","valeur2"));
        {
            AlternativeName name = new AlternativeName();
            name.setName(new EmbeddableMultilingualString("other name"));
            area.getAlternativeNames().add(name);
        }


        {
            ArrayList<CheckConstraint> constraints = new ArrayList<>();
            constraints.add(new CheckConstraint());
            area.setCheckConstraints(constraints);
        }

        {
            ArrayList<EquipmentPlace> equipmentPlaces = new ArrayList<>();
            equipmentPlaces.add(new EquipmentPlace());
            area.setEquipmentPlaces(equipmentPlaces);
        }

        ArrayList<ParkingBay> bays = new ArrayList<>();
        ParkingBay bay = parkingBaysRepository.save(new ParkingBay());
        bays.add(bay);
        bay.getKeyValues().put("clé",new Value("valeur1","valeur2"));
        {
            AlternativeName name = new AlternativeName();
            name.setName(new EmbeddableMultilingualString("other name"));
            bay.getAlternativeNames().add(name);
        }
        bay.setParkingArea(area);
        {
            ArrayList<EquipmentPlace> equipmentPlaces = new ArrayList<>();
            equipmentPlaces.add(new EquipmentPlace());
            bay.setEquipmentPlaces(equipmentPlaces);
        }

        {
            ArrayList<CheckConstraint> constraints = new ArrayList<>();
            constraints.add(new CheckConstraint());
            bay.setCheckConstraints(constraints);
        }
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


        assertThat(parkingRepository.findAll()).isNotEmpty();
        assertThat(parkingBaysRepository.findAll()).isNotEmpty();
        assertThat(parkingAreasRepository.findAll()).isNotEmpty();
        assertThat(parkingEntranceRepository.findAll()).isNotEmpty();

        deleteService.deleteAllParkings();

        assertThat(parkingRepository.findAll()).isEmpty();
        assertThat(parkingBaysRepository.findAll()).isEmpty();
        assertThat(parkingAreasRepository.findAll()).isEmpty();
        assertThat(parkingEntranceRepository.findAll()).isEmpty();
    }
}
