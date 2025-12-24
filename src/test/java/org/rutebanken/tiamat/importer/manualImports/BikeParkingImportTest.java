package org.rutebanken.tiamat.importer.manualImports;



import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.rest.parkings.ImportBikeParkingsResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Transactional
//Dirties context is used to clear H2 database before each test
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BikeParkingImportTest extends TiamatIntegrationTest {

    @Autowired
    public ImportBikeParkingsResource importBikeParkingsResource;

    @Autowired
    private ParkingRepository parkingRepository;


    @Test
    public void testSemiColonFile() throws IOException {
        launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_correct_sep_semi_colon.csv");
        checkCompleteFile();
    }

    @Test
    public void testCommaFile() throws IOException {
        launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_correct_sep_comma.csv");
        checkCompleteFile();
    }

    @ParameterizedTest
    @CsvSource({
            "src/test/resources/manualImports/bikeParkings/bike_parkings_with_duplicates.csv, There are duplicated bike parkings in your CSV File 'With the same ID'. Duplicates:",
            "src/test/resources/manualImports/bikeParkings/bike_parkings_without_id.csv, A header name is missing in ",
            "src/test/resources/manualImports/bikeParkings/bike_parkings_without_longitude.csv, A header name is missing in ",
            "src/test/resources/manualImports/bikeParkings/bike_parkings_without_latitude.csv, A header name is missing in ",
            "src/test/resources/manualImports/bikeParkings/bike_parkings_without_capacity.csv, Capacity is required in all your bike parkings",
            "src/test/resources/manualImports/bikeParkings/bike_parkings_without_type_accroche.csv, Hook type is required in all your bike parkings",
    })
    void testInvalidImportFile(String importFilename, String expectedMessage) {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile(importFilename));
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testParkingWithIdLocalAndIdOsmInDB() throws IOException {
        createParkingWithIdLocalAndIdOsm();
        launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_with_id_local_and_id_osm.csv");
        List<Parking> parkings = parkingRepository.findAll();
        Assertions.assertEquals(1, parkings.size());
    }

    @Test
    public void testParkingWithIdLocalAndIdOsmNotInDB() throws IOException {
        launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_with_id_local_and_id_osm.csv");
        List<Parking> parkings = parkingRepository.findAll();
        Assertions.assertEquals(1, parkings.size());
    }

    @Test
    public void testParkingWithIdLocalInImport() throws IOException {
        createParkingWithIdLocalAndIdOsm();
        launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_with_id_local.csv");
        List<Parking> parkings = parkingRepository.findAll();
        Assertions.assertEquals(2, parkings.size());
    }

    @Test
    public void testParkingWithIdLocalAndNotIdOsmInImportAndInDB() throws IOException {
        createParkingWithIdLocal();
        launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_with_id_local.csv");
        List<Parking> parkings = parkingRepository.findAll();
        Assertions.assertEquals(1, parkings.size());
    }

    public void createParkingWithIdLocalAndIdOsm() {
        Parking parking = new Parking();
        parking.setNetexId("MOBIITI:Parking:1");
        parking.setName(new EmbeddableMultilingualString("1"));
        parking.getOrCreateValues("id_local").add("1");
        parking.getOrCreateValues("id_osm").add("2");
        parking.setVersion(1);
        parkingRepository.save(parking);
    }

    public void createParkingWithIdLocal() {
        Parking parking = new Parking();
        parking.setNetexId("MOBIITI:Parking:1");
        parking.getOrCreateValues("id_local").add("1");
        parking.setName(new EmbeddableMultilingualString("1"));
        parking.setVersion(1);
        parkingRepository.save(parking);
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
        importBikeParkingsResource.importBikeParkingsCsvFile(in, "test_name_file", "test_user");
    }


    /**
     * Checks if the file has been imported without error (by counting persisted entities)
     */
    private void checkCompleteFile() {

        List<Parking> persistedEnt = parkingRepository.findAll();
        assertEquals(11, persistedEnt.size(), "Wrong number of persisted parking in DB");

        persistedEnt.forEach(this::checkPersistedEnt);

    }


    /**
     * Perform some checks on the persisted entities
     *
     * @param bikeParking
     */
    private void checkPersistedEnt(Parking bikeParking) {
        assertEquals( ParkingTypeEnumeration.OTHER, bikeParking.getParkingType(), "Wrong parking type for bike import");
    }

}