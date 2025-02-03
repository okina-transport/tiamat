package org.rutebanken.tiamat.service.parking.gbfs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rutebanken.tiamat.externalapis.gbfs.GbfsClient;
import org.rutebanken.tiamat.model.gbfs.GbfsValidationOutput;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(Parameterized.class)
public class GbfsFeedValidatorTest {

    private final GbfsClient gbfsClient = new GbfsClient();
    private final String input;
    private final String expected;

    @Parameterized.Parameters
    public static Collection<Object[]> inputData() {
        return Arrays.asList(new Object[][]{
                {"vehicle_types", "Invalid vehicle type url"},
                {"station_information", "Invalid station information url"},
                {"system_information", "Invalid system information url"}
        });
    }

    public GbfsFeedValidatorTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void feedValidator_blankUrlValidation_test() {
        GbfsFeedValidator validator = GbfsFeedValidatorFactory.init(gbfsClient, input).build();

        GbfsValidationOutput gbfsValidationOutput = validator.validateFeed("");

        assertThat(gbfsValidationOutput).isNotNull();
        assertThat(gbfsValidationOutput.getErrors()).hasSize(1).containsExactly(expected);
    }

}
