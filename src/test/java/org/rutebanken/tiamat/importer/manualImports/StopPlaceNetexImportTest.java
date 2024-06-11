package org.rutebanken.tiamat.importer.manualImports;

import org.junit.Test;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.importer.NetexImporter;
import org.rutebanken.tiamat.model.job.JobImportType;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.rutebanken.tiamat.rest.parkingsNetex.ImportParkingsNetexResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Transactional
//Dirties context is used to clear H2 database before each test
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StopPlaceNetexImportTest extends TiamatIntegrationTest {

    @Autowired
    public ImportParkingsNetexResource importResource;

    @Autowired
    private PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;

    @Autowired
    private NetexImporter netexImporter;

    @Test
    public void testStopPlaceNetex() throws IOException, JAXBException, SAXException, TiamatBusinessException {
        launchImportForFile("stop_place.xml");
    }

    @Test
    public void testStopPlaceNetexWithoutName() throws IOException, JAXBException, SAXException, TiamatBusinessException {
        launchImportForFile("stop_place_without_name.xml");
    }

    /**
     * Launch a manual import of the file given as parameter
     * @param fileName
     * the file to import
     * @throws IOException
     */
    private void launchImportForFile(String fileName) throws IOException, JAXBException, SAXException, TiamatBusinessException {
        File file = new File("src/test/resources/manualImports/stopPlaceNetex/" + fileName);

        try (InputStream testInputStream = new FileInputStream(file)) {
            PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(testInputStream);
            Provider provider = Collections.singletonList(providerRepository.getProvider(1L)).get(0);
            netexImporter.importProcessTest(incomingPublicationDelivery, String.valueOf(provider.getId()), fileName, JobImportType.NETEX_STOP_PlACE_QUAY);
        }
    }

}