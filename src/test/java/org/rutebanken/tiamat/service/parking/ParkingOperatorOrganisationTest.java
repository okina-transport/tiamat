package org.rutebanken.tiamat.service.parking;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.general.ParkingsCSVHelper;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Organisation;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.OrganisationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ParkingOperatorOrganisationTest extends TiamatIntegrationTest {

    @Autowired
    private ParkingsImportedService parkingsImportedService;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    @Transactional
    public void csvParkingImportCreatesOneOperatorOrganisationPerOperator() {
        String effia = "EFFIA-" + UUID.randomUUID();
        String indigo = "INDIGO-" + UUID.randomUUID();

        Parking p1 = csvParking(effia, 1.0, 45.0);
        Parking p2 = csvParking(effia, 1.1, 45.1);
        Parking p3 = csvParking(indigo, 1.2, 45.2);
        Parking p4 = csvParking(ParkingsCSVHelper.DEFAULT_OPERATOR, 1.3, 45.3);

        parkingsImportedService.createOrUpdateParkings(List.of(p1, p2, p3, p4));

        Organisation effiaOrg = organisationRepository.findByName(effia).orElseThrow();
        Organisation indigoOrg = organisationRepository.findByName(indigo).orElseThrow();
        assertThat(effiaOrg.getType()).isEqualTo(ParkingOperatorOrganisationService.OPERATOR_ORGANISATION_TYPE);
        assertThat(effiaOrg.getNetexId()).contains(":Organisation:");

        assertParkingOrganisation(p1, effiaOrg);
        assertParkingOrganisation(p2, effiaOrg);
        assertParkingOrganisation(p3, indigoOrg);
        assertThat(parkingRepository.findFirstByNetexIdOrderByVersionDesc(p4.getNetexId()).getOrganisation()).isNull();
    }

    @Test
    @Transactional
    public void csvParkingUpdateReusesExistingOperatorOrganisation() {
        String operator = "EFFIA-" + UUID.randomUUID();
        String originalId = "PK-" + UUID.randomUUID();

        parkingsImportedService.createOrUpdateParkings(List.of(csvParking(originalId, operator, 2.0, 46.0)));
        parkingsImportedService.createOrUpdateParkings(List.of(csvParking(originalId, operator, 2.0, 46.0)));

        Organisation organisation = organisationRepository.findByName(operator).orElseThrow();
        String netexId = parkingRepository.findFirstByKeyValues(NetexIdMapper.ORIGINAL_ID_KEY, Set.of(originalId));
        Parking lastVersion = parkingRepository.findFirstByNetexIdOrderByVersionDesc(netexId);
        assertThat(lastVersion.getVersion()).isEqualTo(2L);
        assertThat(lastVersion.getOrganisation().getId()).isEqualTo(organisation.getId());
    }

    @Test
    @Transactional
    public void csvBikeAndRentalBikeImportsCreateOperatorOrganisation() {
        String operator = "VELO-" + UUID.randomUUID();

        Parking bike = bikeParking(operator, 3.0, 47.0);
        Parking rentalBike = bikeParking(operator, 3.1, 47.1);

        parkingsImportedService.createOrUpdateParkings(List.of(bike));
        parkingsImportedService.createOrUpdateParkings(List.of(rentalBike));

        Organisation organisation = organisationRepository.findByName(operator).orElseThrow();
        assertParkingOrganisation(bike, organisation);
        assertParkingOrganisation(rentalBike, organisation);
    }

    private void assertParkingOrganisation(Parking parking, Organisation expected) {
        Parking saved = parkingRepository.findFirstByNetexIdOrderByVersionDesc(parking.getNetexId());
        assertThat(saved.getOrganisation()).isNotNull();
        assertThat(saved.getOrganisation().getId()).isEqualTo(expected.getId());
    }

    private Parking csvParking(String operator, double x, double y) {
        return csvParking("PK-" + UUID.randomUUID(), operator, x, y);
    }

    private Parking csvParking(String originalId, String operator, double x, double y) {
        Parking parking = new Parking();
        parking.setVersion(1L);
        parking.setOriginalId(originalId);
        parking.setName(new EmbeddableMultilingualString("Parking " + originalId));
        parking.setCentroid(ImporterUtils.createPoint(x, y));
        parking.setOperator(operator);
        return parking;
    }

    private Parking bikeParking(String operator, double x, double y) {
        String idLocal = "BK-" + UUID.randomUUID();
        Parking parking = new Parking();
        parking.setOriginalId(idLocal);
        parking.setName(new EmbeddableMultilingualString("Bike " + idLocal));
        parking.setCentroid(ImporterUtils.createPoint(x, y));
        parking.setOperator(operator);
        parking.getOrCreateValues("id_local").add(idLocal);
        return parking;
    }
}
