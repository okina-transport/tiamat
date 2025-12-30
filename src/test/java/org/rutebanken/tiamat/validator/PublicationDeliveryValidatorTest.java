package org.rutebanken.tiamat.validator;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rutebanken.tiamat.netex.NetexConstants;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.rutebanken.tiamat.config.Messages.*;

@ExtendWith(MockitoExtension.class)
public class PublicationDeliveryValidatorTest {

    private static final File NETEX_PARKING_XML_MORE_THAN_3_PARKING_LEVELS = new File("src/test/resources/manualImports/parkingsNetex/parkings_v1.2_3_or_more_parking_levels.xml");
    private static final File NETEX_PARKING_XML_3_OR_LESS_PARKING_LEVELS = new File("src/test/resources/manualImports/parkingsNetex/parkings_v1.2_3_or_less_parking_levels.xml");
    private static final File NETEX_PARKING_XML_ORGANISATION_INVALID = new File("src/test/resources/manualImports/parkingsNetex/parkings_v1.2_organisation_invalid.xml");
    private static final File NETEX_PARKING_XML_ORGANISATION_VALID = new File("src/test/resources/manualImports/parkingsNetex/parkings_v1.2_organisation_valid.xml");
    private static final File NETEX_PARKING_XML_GENERAL_ORGANISATION_INVALID = new File("src/test/resources/manualImports/parkingsNetex/parkings_v1.2_general_organisation_invalid.xml");
    private static final File NETEX_PARKING_XML_GENERAL_ORGANISATION_VALID = new File("src/test/resources/manualImports/parkingsNetex/parkings_v1.2_general_organisation_valid.xml");
    private static final File NETEX_PARKING_XML_TYPE_OF_FRAME_MISSING = new File("src/test/resources/manualImports/parkingsNetex/parkings_v1.2_tof_missing.xml");
    private static final File NETEX_PARKING_XML_TYPE_OF_FRAME_INVALID = new File("src/test/resources/manualImports/parkingsNetex/parkings_v1.2_tof_invalid.xml");
    private static final File NETEX_PARKING_XML_TYPE_OF_FRAME_VALID = new File("src/test/resources/manualImports/parkingsNetex/parkings_v1.2_tof_valid.xml");
    private final PublicationDeliveryUnmarshaller unmarshaller = new PublicationDeliveryUnmarshaller();

    @Mock
    private ParkingValidator mockParkingValidator;

    @InjectMocks
    private PublicationDeliveryValidator tested;

    public PublicationDeliveryValidatorTest() throws IOException, SAXException {
    }

    @Test
    public void givenParkingHasMoreThan3Levels_whenValidatingPublicationDelivery_thenProducesValidationError() throws IOException, JAXBException, SAXException {
        // Arrange
        var pd = unmarshaller.unmarshal(new FileInputStream(NETEX_PARKING_XML_MORE_THAN_3_PARKING_LEVELS));
        Errors errors = new BeanPropertyBindingResult(pd, "");
        Mockito.doNothing().when(mockParkingValidator).validate(any(), eq(errors));

        // Act
        tested.validate(pd, errors);

        // Assert
        Mockito.verify(mockParkingValidator, Mockito.times(4)).validate(any(), eq(errors));

        var expectedErrors = errors.getFieldErrors().stream().filter(e -> Objects.equals(e.getCode(), VALIDATION_MAXIMUM_PARKING_DEPTH_EXCEEDED)).toList();
        assertEquals(1, expectedErrors.size(), "there should be one maximum parking depth validation error");
        assertArrayEquals(new Object[]{"FR:75056:Parking:1:LOC", 3}, expectedErrors.get(0).getArguments(), "arguments should match");
    }

    @Test
    public void givenNoParkingHasMoreThan3Levels_whenValidatingPublicationDelivery_thenDoesNotProduceValidationError() throws IOException, JAXBException, SAXException {
        // Arrange
        var pd = unmarshaller.unmarshal(new FileInputStream(NETEX_PARKING_XML_3_OR_LESS_PARKING_LEVELS));
        Errors errors = new BeanPropertyBindingResult(pd, "");
        Mockito.doNothing().when(mockParkingValidator).validate(any(), eq(errors));

        // Act
        tested.validate(pd, errors);

        // Assert
        Mockito.verify(mockParkingValidator, Mockito.times(6)).validate(any(), eq(errors));

        var expectedErrors = errors.getFieldErrors().stream().filter(e -> Objects.equals(e.getCode(), VALIDATION_MAXIMUM_PARKING_DEPTH_EXCEEDED)).toList();
        assertEquals(0, expectedErrors.size(), "there should not be any maximum parking depth validation error");
    }

    @Test
    public void givenOrganisationIsInvalid_whenValidatingPublicationDelivery_thenProducesValidationError() throws IOException, JAXBException, SAXException {
        // Arrange
        var pd = unmarshaller.unmarshal(new FileInputStream(NETEX_PARKING_XML_ORGANISATION_INVALID));
        Errors errors = new BeanPropertyBindingResult(pd, "");
        Mockito.doNothing().when(mockParkingValidator).validate(any(), eq(errors));

        // Act
        tested.validate(pd, errors);

        // Assert
        var expectedErrors = errors.getFieldErrors().stream().filter(e -> Objects.equals(e.getCode(), VALIDATION_COMPANY_NUMBER_REQUIRED)).toList();
        assertEquals(1, expectedErrors.size(), "there should be one company number required validation error");
        assertArrayEquals(new Object[]{"MOBI-ITI:Organisation:1"}, expectedErrors.get(0).getArguments(), "arguments should match");
    }

    @Test
    public void givenOrganisationIsValid_whenValidatingPublicationDelivery_thenDoesNotProduceValidationError() throws IOException, JAXBException, SAXException {
        // Arrange
        var pd = unmarshaller.unmarshal(new FileInputStream(NETEX_PARKING_XML_ORGANISATION_VALID));
        Errors errors = new BeanPropertyBindingResult(pd, "");
        Mockito.doNothing().when(mockParkingValidator).validate(any(), eq(errors));

        // Act
        tested.validate(pd, errors);

        // Assert
        var expectedErrors = errors.getFieldErrors().stream().filter(e -> Objects.equals(e.getCode(), VALIDATION_MAXIMUM_PARKING_DEPTH_EXCEEDED)).toList();
        assertEquals(0, expectedErrors.size(), "there should not be company number required validation error");
    }

    @Test
    public void givenGeneralOrganisationIsInvalid_whenValidatingPublicationDelivery_thenProducesValidationError() throws IOException, JAXBException, SAXException {
        // Arrange
        var pd = unmarshaller.unmarshal(new FileInputStream(NETEX_PARKING_XML_GENERAL_ORGANISATION_INVALID));
        Errors errors = new BeanPropertyBindingResult(pd, "");
        Mockito.doNothing().when(mockParkingValidator).validate(any(), eq(errors));

        // Act
        tested.validate(pd, errors);

        // Assert
        var expectedErrors = errors.getFieldErrors().stream().filter(e -> Objects.equals(e.getCode(), VALIDATION_COMPANY_NUMBER_REQUIRED)).toList();
        assertEquals(1, expectedErrors.size(), "there should be one company number required validation error");
        assertArrayEquals(new Object[]{"MOBI-ITI:GeneralOrganisation:1"}, expectedErrors.get(0).getArguments(), "arguments should match");
    }

    @Test
    public void givenGeneralOrganisationIsValid_whenValidatingPublicationDelivery_thenDoesNotProduceValidationError() throws IOException, JAXBException, SAXException {
        // Arrange
        var pd = unmarshaller.unmarshal(new FileInputStream(NETEX_PARKING_XML_GENERAL_ORGANISATION_VALID));
        Errors errors = new BeanPropertyBindingResult(pd, "");
        Mockito.doNothing().when(mockParkingValidator).validate(any(), eq(errors));

        // Act
        tested.validate(pd, errors);

        // Assert
        var expectedErrors = errors.getFieldErrors().stream().filter(e -> Objects.equals(e.getCode(), VALIDATION_MAXIMUM_PARKING_DEPTH_EXCEEDED)).toList();
        assertEquals(0, expectedErrors.size(), "there should not be company number required validation error");
    }

    @Test
    public void givenTypeOfFrameIsMissing_whenValidatingPublicationDelivery_thenProducesValidationError() throws IOException, JAXBException, SAXException {
        // Arrange
        var pd = unmarshaller.unmarshal(new FileInputStream(NETEX_PARKING_XML_TYPE_OF_FRAME_MISSING));
        Errors errors = new BeanPropertyBindingResult(pd, "");
        Mockito.doNothing().when(mockParkingValidator).validate(any(), eq(errors));

        // Act
        tested.validate(pd, errors);

        // Assert
        var expectedErrors = errors.getFieldErrors().stream().filter(e -> Objects.equals(e.getCode(), VALIDATION_TOF_REQUIRED)).toList();
        assertEquals(1, expectedErrors.size(), "there should be one type of frame required validation error");
        assertArrayEquals(new Object[]{"MOBI-ITI:GeneralFrame:NETEX_PARKING_20240403T130854Z:LOC"}, expectedErrors.get(0).getArguments(), "arguments should match");
    }

    @Test
    public void givenTypeOfFrameIsInvalid_whenValidatingPublicationDelivery_thenProducesValidationError() throws IOException, JAXBException, SAXException {
        // Arrange
        var pd = unmarshaller.unmarshal(new FileInputStream(NETEX_PARKING_XML_TYPE_OF_FRAME_INVALID));
        Errors errors = new BeanPropertyBindingResult(pd, "");
        Mockito.doNothing().when(mockParkingValidator).validate(any(), eq(errors));

        // Act
        tested.validate(pd, errors);

        // Assert
        List<FieldErrorRecordTest> expectedFieldErrors = List.of(
                new FieldErrorRecordTest("dataObjects.compositeFrameOrCommonFrame[0].value.members.generalFrameMemberOrDataManagedObjectOrEntity_Entity[1].value.id", VALIDATION_TOF_ID_INVALID, new Object[]{NetexConstants.NETEX_PARKING_TOF_ID, "FR:TypeOfFrame:1"}),
                new FieldErrorRecordTest("dataObjects.compositeFrameOrCommonFrame[0].value.members.generalFrameMemberOrDataManagedObjectOrEntity_Entity[1].value.version", VALIDATION_TOF_VERSION_INVALID, new Object[]{NetexConstants.NETEX_PARKING_TOF_VERSION, "FR:TypeOfFrame:1"}),
                new FieldErrorRecordTest("dataObjects.compositeFrameOrCommonFrame[0].value.members.generalFrameMemberOrDataManagedObjectOrEntity_Entity[1].value.name.value", VALIDATION_TOF_NAME_INVALID, new Object[]{NetexConstants.NETEX_PARKING_TOF_NAME, "FR:TypeOfFrame:1"}),
                new FieldErrorRecordTest("dataObjects.compositeFrameOrCommonFrame[0].value.members.generalFrameMemberOrDataManagedObjectOrEntity_Entity[1].value.description.value", VALIDATION_TOF_DESCRIPTION_INVALID, new Object[]{NetexConstants.NETEX_PARKING_TOF_DESCRIPTION, "FR:TypeOfFrame:1"})
        );
        for (FieldErrorRecordTest expectedError : expectedFieldErrors) {
            FieldError fe = errors.getFieldError(expectedError.field);
            assertNotNull(fe, "should be an error on field " + expectedError.field);
            assertEquals(expectedError.code, fe.getCode(), "error code should be " + expectedError.code);
            assertArrayEquals(expectedError.args, fe.getArguments(), "error args should be " + Arrays.toString(expectedError.args));
        }
    }

    @Test
    public void givenTypeOfFrameIsValid_whenValidatingPublicationDelivery_thenDoesNotProduceValidationError() throws IOException, JAXBException, SAXException {
        // Arrange
        var pd = unmarshaller.unmarshal(new FileInputStream(NETEX_PARKING_XML_TYPE_OF_FRAME_VALID));
        Errors errors = new BeanPropertyBindingResult(pd, "");
        Mockito.doNothing().when(mockParkingValidator).validate(any(), eq(errors));

        // Act
        tested.validate(pd, errors);

        // Assert
        List<String> expectedErrorCodes = List.of(VALIDATION_TOF_ID_INVALID, VALIDATION_TOF_VERSION_INVALID, VALIDATION_TOF_NAME_INVALID, VALIDATION_TOF_DESCRIPTION_INVALID);
        var expectedErrors = errors.getFieldErrors().stream().filter(e -> expectedErrorCodes.contains(e.getCode())).toList();
        assertEquals(0, expectedErrors.size(), "there should no type of frame validation error");
    }

    record FieldErrorRecordTest(String field, String code, Object[] args) {
    }

}
