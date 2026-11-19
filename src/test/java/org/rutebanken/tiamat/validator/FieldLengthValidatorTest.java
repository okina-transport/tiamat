package org.rutebanken.tiamat.validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldLengthValidatorTest {

    @Test
    void convertsMappingPathToNetexElements() {
        assertThat(FieldLengthValidator.toFieldName("name.value")).isEqualTo("Name");
        assertThat(FieldLengthValidator.toFieldName("name.lang")).isEqualTo("Name/Lang");
        assertThat(FieldLengthValidator.toFieldName("bookingUrl")).isEqualTo("BookingUrl");
        assertThat(FieldLengthValidator.toFieldName("postalAddress.street")).isEqualTo("PostalAddress/Street");
        assertThat(FieldLengthValidator.toFieldName("keyValues.items")).isEqualTo("KeyValues/Items");
    }
}
