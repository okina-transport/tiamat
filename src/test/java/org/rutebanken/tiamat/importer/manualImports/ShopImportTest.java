package org.rutebanken.tiamat.importer.manualImports;

import org.hibernate.Hibernate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

import static org.junit.jupiter.api.Assertions.*;


@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ShopImportTest extends TiamatIntegrationTest {

    @Autowired
    public ImportPOIResource importResource;

    @Autowired
    private PointOfInterestRepository poiRepository;

    @Autowired
    private PointOfInterestClassificationRepository poiClassRepository;

    @ParameterizedTest
    @ValueSource(strings = {
            "src/test/resources/manualImports/shop/poi_pdv_correct_sep_semi_colon.csv",
            "src/test/resources/manualImports/shop/poi_pdv_correct_sep_comma.csv"
    })
    void testValidFile(String fileName) throws IOException {
        poiClassRepository.deleteAll();
        launchImportForFile(fileName);
        checkCompleteFile();
    }

    @ParameterizedTest
    @CsvSource({
            "src/test/resources/manualImports/shop/poi_pdv_with_duplicates.csv, There are duplicated POI in your CSV File (With the same ID & Name",
            "src/test/resources/manualImports/shop/poi_pdv_without_id.csv, ID is required in all POI",
            "src/test/resources/manualImports/shop/poi_pdv_without_name.csv, NAME is required for POI ",
            "src/test/resources/manualImports/shop/poi_pdv_without_longitude.csv, longitude is required for POI",
            "src/test/resources/manualImports/shop/poi_pdv_without_latitude.csv, latitude is required for POI"
    })
    void testInvalidFileThrows(String fileName, String expectedMessage) {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile(fileName));
        assertTrue(exception.getMessage().contains(expectedMessage));
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
     * @param poi persistent object to check
     */
    private void checkPersistedPOI(PointOfInterest poi){
        Hibernate.initialize(poi.getClassifications());
        assertFalse(poi.getClassifications().isEmpty(), "POI must have a classification");
        assertNotNull(poi.getPointOfInterestFacilitySet(), "shop POI must have a facility set");
    }

}
