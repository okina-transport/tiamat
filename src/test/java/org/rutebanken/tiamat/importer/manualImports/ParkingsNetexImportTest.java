package org.rutebanken.tiamat.importer.manualImports;

import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.client.mdm.ParkingIdentifier;
import org.rutebanken.tiamat.importer.NetexImporter;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.rutebanken.tiamat.rest.parkingsNetex.ImportParkingsNetexResource;
import org.rutebanken.tiamat.service.parking.ParkingsImportedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Transactional
//Dirties context is used to clear H2 database before each test
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ParkingsNetexImportTest extends TiamatIntegrationTest {

    @Autowired
    public ImportParkingsNetexResource importResource;

    @Autowired
    private PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;

    @Autowired
    private NetexImporter netexImporter;

    @Autowired
    private ParkingsImportedService parkingsImportedService;

    @Autowired
    private ParkingRepository parkingRepository;

    @Test
    public void testParkingsNetex() throws IOException, JAXBException, SAXException, BindException {

        ParkingIdentifier pid = new ParkingIdentifier();
        pid.setOriginalId("63000-PRelais1");

        when(mdmClient.getParkingIdentifiers(any())).thenReturn(List.of(pid));
        launchImportForFile("parkings_relai_vls_velo.xml");
    }

    /**
     * Launch a manual import of the file given as parameter
     *
     * @param fileName the file to import
     * @throws IOException
     */
    private void launchImportForFile(String fileName) throws IOException, JAXBException, SAXException, BindException {
        File file = new File("src/test/resources/manualImports/parkingsNetex/" + fileName);

        try (InputStream testInputStream = new FileInputStream(file)) {
            PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(testInputStream);
            Provider provider = Collections.singletonList(providerRepository.getProvider(1L)).get(0);
            netexImporter.importProcessTest(incomingPublicationDelivery, false);
        }
    }

    @Test
    public void testFoundParkings() throws IOException, JAXBException, SAXException, TiamatBusinessException, BindException {
        parkingRepository.deleteAll();

        List<Parking> parkingsToSave = createInitialParkingList();
        parkingsImportedService.createOrUpdateParkings(parkingsToSave);
        assertEquals(1, parkingRepository.findAll().size());

        parkingsToSave.add(createNewParking());
        parkingsImportedService.createOrUpdateParkings(parkingsToSave);
        assertEquals(2, parkingRepository.findAll().size());
    }

    private Parking createNewParking() {
        Parking p2 = new Parking();
        EmbeddableMultilingualString nameP1 = new EmbeddableMultilingualString();
        nameP1.setValue("nameP2");
        p2.setName(nameP1);
        p2.setOriginalId("customP2");
        p2.setCentroid(ImporterUtils.createPoint(0.8d, 0.8d));
        return p2;
    }

    private List<Parking> createInitialParkingList() {
        List<Parking> parkingsToSave = new ArrayList<>();
        Parking p1 = new Parking();
        EmbeddableMultilingualString nameP1 = new EmbeddableMultilingualString();
        nameP1.setValue("nameP1");
        p1.setName(nameP1);
        p1.setOriginalId("customP1");
        p1.setCentroid(ImporterUtils.createPoint(0.5d, 0.6d));
        parkingsToSave.add(p1);
        return parkingsToSave;
    }

}
