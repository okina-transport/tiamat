package org.rutebanken.tiamat.importer.manualImports;


import org.junit.Assert;
import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.rest.parkings.ImportRentalBikeResource;
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
public class RentalBikeImportTest extends TiamatIntegrationTest {

    @Autowired
    public ImportRentalBikeResource importResource;

    @Autowired
    private ParkingRepository parkingRepository;



    @Test
    public void testSemiColonFile() throws IOException {
        launchImportForFile("src/test/resources/manualImports/rentalBikes/rental_bikes_correct_sep_semi_colon.csv");
        checkCompleteFile();
    }

    @Test
    public void testCommaFile() throws IOException {
        launchImportForFile("src/test/resources/manualImports/rentalBikes/rental_bikes_correct_sep_comma.csv");
        checkCompleteFile();
    }

    @Test
    public void testDuplicateDetection() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/rentalBikes/rental_bikes_with_duplicates.csv") );
        String expectedMessage = "There are duplicated bike parkings in your CSV File 'With the same ID'. Duplicates:";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testPOIWithoutID() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/rentalBikes/rental_bikes_without_id.csv") );
        String expectedMessage = "ID is required in all your parkings";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testPOIWithoutLongitude() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> launchImportForFile("src/test/resources/manualImports/rentalBikes/rental_bikes_without_longitude.csv") );
        String expectedMessage = "X Longitud is not correct in the parking with";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testPOIWithoutLatitude() {
        Exception exception = assertThrows(ArrayIndexOutOfBoundsException.class, () -> launchImportForFile("src/test/resources/manualImports/rentalBikes/rental_bikes_without_latitude.csv") );
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
        importResource.importRentalBikesFile(in, "test_name_file", "test_user");
    }


    /**
     * Checks if the file has been imported without error (by counting persisted entities)
     */
    private void checkCompleteFile(){

        List<Parking> persistedEnt = parkingRepository.findAll();
        Assert.assertEquals("Wrong number of persisted parking in DB",11,persistedEnt.size());

        persistedEnt.forEach(this::checkPersistedEnt);

    }


    /**
     * Perform some checks on the persisted entities
     * @param bikeParking
     */
    private void checkPersistedEnt(Parking bikeParking){
        assertEquals("Wrong parking type for bike import",ParkingTypeEnumeration.CYCLE_RENTAL,bikeParking.getParkingType());
    }

}