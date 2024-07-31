package org.rutebanken.tiamat.importer.manualImports;

import org.junit.Test;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.importer.NetexImporter;
import org.rutebanken.tiamat.model.job.JobType;
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
public class PoiNetexImportTest extends TiamatIntegrationTest {

    @Autowired
    public ImportParkingsNetexResource importResource;

    @Autowired
    private PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;

    @Autowired
    private NetexImporter netexImporter;

    @Test
    public void testPoiNetex() throws IOException, JAXBException, SAXException, TiamatBusinessException {
        launchImportForFile("poi.xml");
    }

    @Test
    public void testPoiNetexWithoutName() throws IOException, JAXBException, SAXException, TiamatBusinessException {
        launchImportForFile("poi_without_name.xml");
    }

    /**
     * Launch a manual import of the file given as parameter
     * @param fileName
     * the file to import
     * @throws IOException
     */
    private void launchImportForFile(String fileName) throws IOException, JAXBException, SAXException, TiamatBusinessException {
        File file = new File("src/test/resources/manualImports/poiNetex/" + fileName);

        try (InputStream testInputStream = new FileInputStream(file)) {
            PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(testInputStream);
            Provider provider = Collections.singletonList(providerRepository.getProvider(1L)).get(0);
            netexImporter.importProcessTest(incomingPublicationDelivery,  false);
        }
    }

}
