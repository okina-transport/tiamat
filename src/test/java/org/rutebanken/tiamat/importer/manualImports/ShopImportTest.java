package org.rutebanken.tiamat.importer.manualImports;

import org.hibernate.Hibernate;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.PointOfInterestClassification;
import org.rutebanken.tiamat.repository.PointOfInterestClassificationRepository;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.rest.poi.ImportPOIResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Transactional
//Dirties context is used to clear H2 database before each test
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ShopImportTest extends TiamatIntegrationTest {

    @Autowired
    public ImportPOIResource importResource;

    @Autowired
    private PointOfInterestRepository poiRepository;

    @Autowired
    private PointOfInterestClassificationRepository poiClassRepository;



    @Test
    public void testSemiColonFile() throws IOException {
        poiClassRepository.deleteAll();
        launchImportForFile("src/test/resources/manualImports/shop/poi_pdv_correct_sep_semi_colon.csv");
        checkCompleteFile();
    }

   @Test
    public void testCommaFile() throws IOException {
       poiClassRepository.deleteAll();
        launchImportForFile("src/test/resources/manualImports/shop/poi_pdv_correct_sep_comma.csv");
        checkCompleteFile();
    }

    @Test
    public void testDuplicateDetection() throws IOException {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/shop/poi_pdv_with_duplicates.csv") );
        String expectedMessage = "There are duplicated POI in your CSV File (With the same ID & Name";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

   @Test
    public void testPOIWithoutID() throws IOException {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/shop/poi_pdv_without_id.csv") );
        String expectedMessage = "ID is required in all POI";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testPOIWithoutName() throws IOException {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/shop/poi_pdv_without_name.csv") );
        String expectedMessage = "NAME is required for POI ";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testPOIWithoutLongitude() throws IOException {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/shop/poi_pdv_without_longitude.csv") );
        String expectedMessage = "longitude is required for POI";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testPOIWithoutLatitude() throws IOException {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/shop/poi_pdv_without_latitude.csv") );
        String expectedMessage = "latitude is required for POI";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }



    /**
     * Launch a manual import of the file given as parameter
     * @param fileName
     * the file to import
     * @throws IOException
     */
    private void launchImportForFile(String fileName) throws IOException {

        File file = new File(fileName);
        InputStream in = new FileInputStream(file);
        importResource.importPOIFile(in, "test_file_name", "test_user");
    }


    /**
     * Checks if the file has been imported without error (by counting persisted entities)
     */
    private void checkCompleteFile(){

        List<PointOfInterest> persistedPOI = poiRepository.findAllAndInitialize();
        assertEquals(15,persistedPOI.size(), "Wrong number of persisted POI in DB");

        persistedPOI.forEach(this::checkPersistedPOI);

        List<PointOfInterestClassification> persistedClassifications = poiClassRepository.findAll();
        assertEquals(4,persistedClassifications.size(), "Wrong number of persisted classifications in DB");
    }


    /**
     * Perform some checks on the persisted entities
     * @param poi
     */
    private void checkPersistedPOI(PointOfInterest poi){
        Hibernate.initialize(poi.getClassifications());
        assertTrue(poi.getClassifications().size() > 0, "POI must have a classification");
        assertTrue(poi.getPointOfInterestFacilitySet() != null, "shop POI must have a facility set");
    }

}
