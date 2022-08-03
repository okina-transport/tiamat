package org.rutebanken.tiamat.importer.manualImports;


import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
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

import static org.junit.Assert.assertEquals;
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

    @Test
    public void testDuplicateDetection() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_with_duplicates.csv"));
        String expectedMessage = "There are duplicated bike parkings in your CSV File 'With the same ID'. Duplicates:";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testPOIWithoutID() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_without_id.csv"));
        String expectedMessage = "A header name is missing in ";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testPOIWithoutLongitude() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_without_longitude.csv"));
        String expectedMessage = "A header name is missing in";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testPOIWithoutLatitude() {

        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_without_latitude.csv"));
        String expectedMessage = "A header name is missing in";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));


    }

    @Test
    public void testPOIWithoutCapacity() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_without_capacity.csv"));
        String expectedMessage = "Capacity is required in all your bike parkings";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testPOIWithoutTypeOfAttachment() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_without_type_accroche.csv"));
        String expectedMessage = "Hook type is required in all your bike parkings";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }


    @Test
    public void testParkingWithIdLocalAndIdOsmInDB() throws IOException {
        createParkingWithIdLocalAndIdOsm();
        launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_with_id_local_and_id_osm.csv");
        List<Parking> parkings = parkingRepository.findAll();
        Assertions.assertEquals(parkings.size(), 1);
    }

    @Test
    public void testParkingWithIdLocalAndIdOsmNotInDB() throws IOException {
        launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_with_id_local_and_id_osm.csv");
        List<Parking> parkings = parkingRepository.findAll();
        Assertions.assertEquals(parkings.size(), 1);
    }

    @Test
    public void testParkingWithIdLocalInImport() throws IOException {
        createParkingWithIdLocalAndIdOsm();
        launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_with_id_local.csv");
        List<Parking> parkings = parkingRepository.findAll();
        Assertions.assertEquals(parkings.size(), 2);
    }

    @Test
    public void testParkingWithIdLocalAndNotIdOsmInImportAndInDB() throws IOException {
        createParkingWithIdLocal();
        launchImportForFile("src/test/resources/manualImports/bikeParkings/bike_parkings_with_id_local.csv");
        List<Parking> parkings = parkingRepository.findAll();
        Assertions.assertEquals(parkings.size(), 1);
    }

    public void createParkingWithIdLocalAndIdOsm() {
        Parking parking = new Parking();
        parking.setNetexId("Test:Parking:1");
        parking.setName(new EmbeddableMultilingualString("1"));
        parking.getOrCreateValues("id_local").add("1");
        parking.getOrCreateValues("id_osm").add("2");
        parking.setVersion(1);
        parkingRepository.save(parking);
    }

    public void createParkingWithIdLocal() {
        Parking parking = new Parking();
        parking.setNetexId("Test:Parking:1");
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
        Assert.assertEquals("Wrong number of persisted parking in DB", 11, persistedEnt.size());

        persistedEnt.forEach(this::checkPersistedEnt);

    }


    /**
     * Perform some checks on the persisted entities
     *
     * @param bikeParking
     */
    private void checkPersistedEnt(Parking bikeParking) {
        assertEquals("Wrong parking type for bike import", ParkingTypeEnumeration.OTHER, bikeParking.getParkingType());
    }

}