package org.rutebanken.tiamat.rest.graphql.helpers;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.rutebanken.tiamat.model.Value;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KeyValueWrapperTest {

    @Test
    public void extractCodeFromKeyValues() {

        // ASSERT
        String defaultIfNull = "yolo";
        Map<String, Value> keyValues = new HashMap<>();
        Value value = new Value();
        Set<String> items = new HashSet<>(Arrays.asList("MOBIITI:StopPlace:724"));
        value.setItems(items);
        keyValues.put("imported-id", value);

        // WHEN
        String codeFound = KeyValueWrapper.extractCodeFromKeyValues(keyValues, defaultIfNull);

        // THEN
        Assertions.assertThat(codeFound).isEqualTo("724");


    }

}