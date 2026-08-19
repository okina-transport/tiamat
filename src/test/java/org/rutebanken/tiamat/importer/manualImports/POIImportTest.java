package org.rutebanken.tiamat.importer.manualImports;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.PointOfInterestClassification;
import org.rutebanken.tiamat.repository.PointOfInterestClassificationRepository;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.rest.poi.ImportPOIResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@Transactional
class POIImportTest extends TiamatIntegrationTest {

    @Autowired
    public ImportPOIResource importResource;

    @Autowired
    private PointOfInterestRepository poiRepository;

    @Autowired
    private PointOfInterestClassificationRepository poiClassRepository;

    @Test
    void testSemiColonFile() throws IOException {
        poiClassRepository.deleteAll();
        launchImportForFile("src/test/resources/manualImports/poi/poi_correct_file_semi_colon_sep.csv");
        checkCompleteFile();
    }

    @Test
    void testCommaFile() throws IOException {
        poiClassRepository.deleteAll();
        launchImportForFile("src/test/resources/manualImports/poi/poi_correct_file_comma_sep.csv");
        checkCompleteFile();
    }

    @Test
    void testDuplicateDetection() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/poi/poi_file_with_duplicates.csv"));
        String expectedMessage = "There are duplicated POI in your CSV File (With the same ID & Name)";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testPOIWithoutID() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/poi/poi_file_without_id.csv"));
        String expectedMessage = "ID is required in all POI";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testPOIWithoutName() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/poi/poi_file_without_name.csv"));
        String expectedMessage = "NAME is required for POI ";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testPOIWithoutLongitude() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/poi/poi_file_without_longitude.csv"));
        String expectedMessage = "longitude is required for POI";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testPOIWithoutLatitude() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/poi/poi_file_without_latitude.csv"));
        String expectedMessage = "latitude is required for POI";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }


    /**
     * Launch a manual import of the file given as parameter
     *
     * @param fileName the file to import
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
    private void checkCompleteFile() {

        List<PointOfInterest> persistedPOI = poiRepository.findAllAndInitialize();
        Assertions.assertEquals(86, persistedPOI.size(), "Wrong number of persisted POI in DB");

        persistedPOI.forEach(this::checkPersistedPOI);

        List<PointOfInterestClassification> persistedClassifications = poiClassRepository.findAll();
        Assertions.assertEquals(22, persistedClassifications.size(), "Wrong number of persisted classifications in DB");
    }


    /**
     * Perform some checks on the persisted entities
     *
     * @param poi persisted entity to check
     */
    private void checkPersistedPOI(PointOfInterest poi) {
        Hibernate.initialize(poi.getClassifications());
        assertFalse(poi.getClassifications().isEmpty(), "POI must have a classification");
        assertNull(poi.getPointOfInterestFacilitySet(), "non-shop POI must not have a facility set");
    }

}
