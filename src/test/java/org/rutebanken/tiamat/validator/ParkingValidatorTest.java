package org.rutebanken.tiamat.validator;

import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.Parking;
import org.rutebanken.tiamat.netex.NetexUtils;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.rutebanken.tiamat.config.Messages.*;

public class ParkingValidatorTest {

    public static final String PARKING_ID = "FR:75056:Parking:1:LOC";
    public static final String PARKING_PROPERTIES_ID = "MOBIITI:ParkingProperties:1";
    public static final String PARKING_AREA_ID = "MOBIITI:ParkingArea:1:LOC";
    private final static File NETEX_PARKING_PFv1_2_MISSING_REQUIRED_FIELDS = new File("src/test/resources/manualImports/parkingsNetex/parkings_v1.2_missing_required_fields.xml");
    PublicationDeliveryUnmarshaller unmarshaller = new PublicationDeliveryUnmarshaller();
    ParkingValidator tested = new ParkingValidator();

    public ParkingValidatorTest() throws IOException, SAXException {
    }

    @Test
    public void givenRequiredNetexParkingValuesAreMissing_whenValidatingParking_thenReturnsValidationErrors() throws IOException, JAXBException, SAXException {
        // Arrange
        var parking = retrieveParkingsFromNetexXml(NETEX_PARKING_PFv1_2_MISSING_REQUIRED_FIELDS).get(0);
        Errors errors = new BeanPropertyBindingResult(parking, "");

        // Act
        tested.validate(parking, errors);

        // Assert
        record FieldErrorRecord(String field, String code, Object[] args) {
        }
        List<FieldErrorRecord> expectedFieldErrors = List.of(
                new FieldErrorRecord("name.value", VALIDATION_PARKING_NAME_REQUIRED, new Object[]{PARKING_ID}),
                new FieldErrorRecord("centroid.location.longitude", VALIDATION_PARKING_CENTROID_REQUIRED, new Object[]{PARKING_ID}),
                new FieldErrorRecord("centroid.location.latitude", VALIDATION_PARKING_CENTROID_REQUIRED, new Object[]{PARKING_ID}),
                new FieldErrorRecord("parkingType", VALIDATION_PARKING_TYPE_REQUIRED, new Object[]{PARKING_ID}),
                new FieldErrorRecord("parkingVehicleTypes", VALIDATION_PARKING_VEHICLE_TYPES_REQUIRED, new Object[]{PARKING_ID}),
                new FieldErrorRecord("parkingLayout", VALIDATION_PARKING_LAYOUT_REQUIRED, new Object[]{PARKING_ID}),
                new FieldErrorRecord("totalCapacity", VALIDATION_PARKING_TOTAL_CAPACITY_REQUIRED, new Object[]{PARKING_ID}),
                new FieldErrorRecord("parkingPaymentProcess", VALIDATION_PARKING_PAYMENT_PROCESS_REQUIRED, new Object[]{PARKING_ID}),
                new FieldErrorRecord("parkingProperties.parkingProperties[0].parkingVehicleTypes", VALIDATION_PARKING_VEHICLE_TYPES_REQUIRED, new Object[]{PARKING_PROPERTIES_ID}),
                new FieldErrorRecord("parkingAreas.parkingAreaRefOrParkingArea_[0].value.maximumHeight", VALIDATION_MAXIMUM_HEIGHT_REQUIRED, new Object[]{PARKING_AREA_ID}),
                new FieldErrorRecord("postalAddress.postalRegion", VALIDATION_POSTAL_ADDRESS_POSTAL_REGION_REQUIRED, new Object[]{PARKING_ID})
        );
        for (FieldErrorRecord expectedError : expectedFieldErrors) {
            FieldError fe = errors.getFieldError(expectedError.field);
            assertNotNull(fe, "should be an error on field " + expectedError.field);
            assertEquals(expectedError.code, fe.getCode(), "error code should be " + expectedError.code);
            assertArrayEquals(expectedError.args, fe.getArguments(), "error args should be " + Arrays.toString(expectedError.args));
        }
    }

    private List<Parking> retrieveParkingsFromNetexXml(File inputNetexXml) throws JAXBException, IOException, SAXException {
        InputStream netexParkingIS = new FileInputStream(inputNetexXml);
        var netex = unmarshaller.unmarshal(netexParkingIS);
        var members = NetexUtils.getMembersFromPublicationDelivery(netex);
        return NetexUtils.getMembers(Parking.class, members);
    }

}
