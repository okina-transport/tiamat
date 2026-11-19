package org.rutebanken.tiamat.validator;

import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.general.NetexLineLocator;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.rutebanken.tiamat.config.Messages.*;

class NetexValidationErrorClassifierTest {

    @Test
    void classifiesStructuralCodesAsTemplate() {
        BindException bindException = bindExceptionWithCodes(VALIDATION_TOF_ID_INVALID, VALIDATION_MAXIMUM_PARKING_DEPTH_EXCEEDED);

        assertThat(NetexValidationErrorClassifier.classify(bindException)).containsExactly(AnalyzeImportErrorType.TEMPLATE);
    }

    @Test
    void classifiesFieldCodesAsMissingData() {
        BindException bindException = bindExceptionWithCodes(VALIDATION_PARKING_NAME_REQUIRED, VALIDATION_PARKING_TOTAL_CAPACITY_REQUIRED);

        assertThat(NetexValidationErrorClassifier.classify(bindException)).containsExactly(AnalyzeImportErrorType.MISSING_DATA);
    }

    @Test
    void classifiesUnknownCodeAsMissingDataByDefault() {
        BindException bindException = bindExceptionWithCodes("some.unmapped.future.validation.code");

        assertThat(NetexValidationErrorClassifier.classify(bindException)).containsExactly(AnalyzeImportErrorType.MISSING_DATA);
    }

    @Test
    void classifiesMixedCodesAsBoth() {
        BindException bindException = bindExceptionWithCodes(VALIDATION_TOF_ID_INVALID, VALIDATION_PARKING_NAME_REQUIRED);

        assertThat(NetexValidationErrorClassifier.classify(bindException))
                .containsExactlyInAnyOrder(AnalyzeImportErrorType.TEMPLATE, AnalyzeImportErrorType.MISSING_DATA);
    }

    @Test
    void classifiesInvalidIdFormatAsInvalidFormatId() {
        BindException bindException = bindExceptionWithCodes(VALIDATION_INVALID_ID_FORMAT);

        assertThat(NetexValidationErrorClassifier.classify(bindException)).containsExactly(AnalyzeImportErrorType.INVALID_FORMAT_ID);
    }

    @Test
    void detailsInvalidIdAndMissingDataErrorsAndReportsTemplateErrorsOnce() {
        String xml = """
                <GeneralFrame id="MOBIITI:GeneralFrame:1" version="1">
                    <members>
                        <Parking id="BAD_ID" version="1">
                            <ParkingProperties id="MOBIITI:ParkingProperties:1" version="1"/>
                        </Parking>
                    </members>
                </GeneralFrame>
                """;
        ParkingProperties parkingProperties = new ParkingProperties().withId("MOBIITI:ParkingProperties:1");
        Parking parking = new Parking().withId("BAD_ID")
                .withParkingProperties(new ParkingProperties_RelStructure().withParkingProperties(parkingProperties));
        GeneralFrame generalFrame = new GeneralFrame().withId("MOBIITI:GeneralFrame:1")
                .withMembers(new General_VersionFrameStructure.Members()
                        .withGeneralFrameMemberOrDataManagedObjectOrEntity_Entity(new ObjectFactory().createParking(parking)));

        Errors errors = new BeanPropertyBindingResult(generalFrame, "generalFrame");
        errors.pushNestedPath("members.generalFrameMemberOrDataManagedObjectOrEntity_Entity[0].value");
        errors.rejectValue("id", VALIDATION_INVALID_ID_FORMAT, new Object[]{"BAD_ID", "pattern"}, null);
        errors.rejectValue("parkingLayout", VALIDATION_PARKING_LAYOUT_REQUIRED, new Object[]{"BAD_ID"}, null);
        errors.rejectValue("name.value", VALIDATION_PARKING_NAME_REQUIRED, new Object[]{"BAD_ID"}, null);
        errors.rejectValue("parkingProperties.parkingProperties[0].parkingVehicleTypes", VALIDATION_PARKING_VEHICLE_TYPES_REQUIRED, new Object[]{"MOBIITI:ParkingProperties:1"}, null);
        errors.rejectValue("", VALIDATION_MUST_NOT_CONTAIN_REFS, new Object[]{"ref1"}, null);
        errors.rejectValue("", VALIDATION_MUST_NOT_CONTAIN_REFS, new Object[]{"ref2"}, null);
        errors.popNestedPath();
        BindException bindException = new BindException((BeanPropertyBindingResult) errors);

        List<JobError> jobErrors = NetexValidationErrorClassifier.toJobErrors(bindException, new NetexLineLocator(xml));

        assertThat(jobErrors).containsExactly(
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_ID)
                        .withParam("field", "id")
                        .withParam("entity", "Parking")
                        .withParam("entityId", "BAD_ID")
                        .withParam("line", "3")
                        .withParam("errorValue", "BAD_ID"),
                JobError.error(AnalyzeImportErrorType.MISSING_DATA)
                        .withParam("field", "ParkingLayout")
                        .withParam("entity", "Parking")
                        .withParam("entityId", "BAD_ID")
                        .withParam("line", "3"),
                JobError.error(AnalyzeImportErrorType.MISSING_DATA)
                        .withParam("field", "Name")
                        .withParam("entity", "Parking")
                        .withParam("entityId", "BAD_ID")
                        .withParam("line", "3"),
                JobError.error(AnalyzeImportErrorType.MISSING_DATA)
                        .withParam("field", "ParkingVehicleTypes")
                        .withParam("entity", "ParkingProperties")
                        .withParam("entityId", "MOBIITI:ParkingProperties:1")
                        .withParam("line", "4"),
                JobError.error(AnalyzeImportErrorType.TEMPLATE));
    }

    @Test
    void reportsMissingDataWithoutFieldWhenErrorIsGlobal() {
        BindException bindException = bindExceptionWithCodes(VALIDATION_PARKING_NAME_REQUIRED);

        assertThat(NetexValidationErrorClassifier.toJobErrors(bindException, null)).containsExactly(
                JobError.error(AnalyzeImportErrorType.MISSING_DATA).withParam("entityId", "FR:75056:Parking:1:LOC"));
    }

    @Test
    void convertsFieldPathToNetexElements() {
        var location = NetexValidationErrorClassifier.FieldLocation.of(
                "dataObjects.compositeFrameOrCommonFrame[0].value.members.generalFrameMemberOrDataManagedObjectOrEntity_Entity[2].value.centroid.location.longitude");

        assertThat(location.entityPath()).isEqualTo("dataObjects.compositeFrameOrCommonFrame[0].value.members.generalFrameMemberOrDataManagedObjectOrEntity_Entity[2].value");
        assertThat(location.netexField()).isEqualTo("Centroid/Location/Longitude");
        assertThat(NetexValidationErrorClassifier.FieldLocation.of("a[0].siteRef.ref").netexField()).isEqualTo("SiteRef");
        assertThat(NetexValidationErrorClassifier.FieldLocation.of("a[0].postalAddress.postalRegion").netexField()).isEqualTo("PostalAddress/PostalRegion");
    }

    private BindException bindExceptionWithCodes(String... codes) {
        Object target = new Object();
        Errors errors = new BeanPropertyBindingResult(target, "target");
        for (String code : codes) {
            errors.reject(code, new Object[]{"FR:75056:Parking:1:LOC"}, code);
        }
        return new BindException((BeanPropertyBindingResult) errors);
    }
}
