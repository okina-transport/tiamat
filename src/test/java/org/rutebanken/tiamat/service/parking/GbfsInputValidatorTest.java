package org.rutebanken.tiamat.service.parking;

import org.junit.Before;
import org.junit.Test;
import org.rutebanken.tiamat.externalapis.gbfs.GbfsClient;
import org.rutebanken.tiamat.model.gbfs.GbfsImportLinks;
import org.rutebanken.tiamat.model.gbfs.GbfsValidationOutput;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class GbfsInputValidatorTest {

    private final GbfsClient gbfsClient = new GbfsClient();

    private GbfsInputValidator validator;

    @Before()
    public void setUp() {
        validator = new GbfsInputValidator(gbfsClient);
    }

    @Test
    public void nullInput_shouldReturnError_test() {
        GbfsValidationOutput gbfsValidationOutput = validator.validateInput(null);

        assertThat(gbfsValidationOutput).isNotNull();
        assertThat(gbfsValidationOutput.getErrors()).hasSize(1).containsExactly("Target url is invalid");
    }

    @Test
    public void emptyInputUrl_shouldReturnError_test() {
        GbfsImportLinks gbfsImportLinks = new GbfsImportLinks();
        gbfsImportLinks.setGlobalUrl("");
        GbfsValidationOutput gbfsValidationOutput = validator.validateInput(gbfsImportLinks);

        assertThat(gbfsValidationOutput).isNotNull();
        assertThat(gbfsValidationOutput.getErrors()).hasSize(1).containsExactly("Target url is invalid");
    }

    @Test
    public void emptyParkingType_shouldReturnError_test() {
        GbfsImportLinks gbfsImportLinks = new GbfsImportLinks();
        gbfsImportLinks.setGlobalUrl("http://test.com");
        GbfsValidationOutput gbfsValidationOutput = validator.validateInput(gbfsImportLinks);

        assertThat(gbfsValidationOutput).isNotNull();
        assertThat(gbfsValidationOutput.getErrors()).hasSize(1).containsExactly("Parking type is blank");
    }

    @Test
    public void invalidParkingType_shouldReturnError_test() {
        GbfsImportLinks gbfsImportLinks = new GbfsImportLinks();
        gbfsImportLinks.setGlobalUrl("http://test.com");
        gbfsImportLinks.setParkingType("wrong");
        GbfsValidationOutput gbfsValidationOutput = validator.validateInput(gbfsImportLinks);

        assertThat(gbfsValidationOutput).isNotNull();
        assertThat(gbfsValidationOutput.getErrors()).hasSize(1).containsExactly("Parking type is invalid");
    }

}