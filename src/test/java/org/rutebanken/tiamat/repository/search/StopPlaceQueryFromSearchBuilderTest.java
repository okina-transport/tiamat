package org.rutebanken.tiamat.repository.search;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class StopPlaceQueryFromSearchBuilderTest {

    @Test
    public void handleCommonWordsInQuery() {

        StopPlaceQueryFromSearchBuilder stopPlaceQueryFromSearchBuilder = new StopPlaceQueryFromSearchBuilder();
        stopPlaceQueryFromSearchBuilder.commonWordsToIgnore.add("des");
        stopPlaceQueryFromSearchBuilder.commonWordsToIgnore.add("de");

        String result = stopPlaceQueryFromSearchBuilder.handleCommonWordsAndSpacesInQuery("Gare de dax");

        Assertions.assertThat(result).isEqualToIgnoringCase("Gare%dax");

        result = stopPlaceQueryFromSearchBuilder.handleCommonWordsAndSpacesInQuery("Place des cyprès");

        Assertions.assertThat(result).isEqualToIgnoringCase("Place%cyprès");
    }
}