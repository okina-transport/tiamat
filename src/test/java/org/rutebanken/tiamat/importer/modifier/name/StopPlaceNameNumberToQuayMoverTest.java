package org.rutebanken.tiamat.importer.modifier.name;

import org.junit.Test;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;

import static org.assertj.core.api.Assertions.assertThat;

public class StopPlaceNameNumberToQuayMoverTest {
    private StopPlaceNameNumberToQuayMover stopPlaceNameNumberToQuayMover = new StopPlaceNameNumberToQuayMover();

    @Test
    public void moveHplNumberEndingToQuay() throws Exception {

        final String originalName = "Stavanger hpl. 13";
        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString(originalName));

        stopPlace.getQuays().add(new Quay(new EmbeddableMultilingualString(originalName)));

        stopPlaceNameNumberToQuayMover.moveNumberEndingToQuay(stopPlace);

        assertThat(stopPlace.getName().getValue()).isEqualTo("Stavanger");

        assertThat(stopPlace.getQuays().iterator().next().getPublicCode()).isEqualTo("13");
    }

    @Test
    public void moveSporNumberEndingToQuay() throws Exception {

        final String originalName = "Bussterminalen spor 6";
        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString(originalName));

        stopPlace.getQuays().add(new Quay(new EmbeddableMultilingualString(originalName)));

        stopPlaceNameNumberToQuayMover.moveNumberEndingToQuay(stopPlace);

        assertThat(stopPlace.getName().getValue()).isEqualTo("Bussterminalen");

        assertThat(stopPlace.getQuays().iterator().next().getPublicCode()).isEqualTo("6");
    }

    @Test
    public void handleSpacesAndDotsInStopPlaceName() {
        final String originalStopPlaceName = "Sandnes rb.st hpl. 20";
        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString(originalStopPlaceName));

        stopPlace.getQuays().add(new Quay(new EmbeddableMultilingualString(originalStopPlaceName)));

        stopPlaceNameNumberToQuayMover.moveNumberEndingToQuay(stopPlace);

        assertThat(stopPlace.getName().getValue()).isEqualTo("Sandnes rb.st");

        assertThat(stopPlace.getQuays().iterator().next().getPublicCode()).isEqualTo("20");
    }

    @Test
    public void quayNameIsUnchanged() throws Exception {

        final String originalStopPlaceName = "Stavanger hpl. 13";
        final String originalQuayName = "Another quay name";

        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString(originalStopPlaceName));

        stopPlace.getQuays().add(new Quay(new EmbeddableMultilingualString(originalQuayName)));

        stopPlaceNameNumberToQuayMover.moveNumberEndingToQuay(stopPlace);

        assertThat(stopPlace.getName().getValue()).isEqualTo("Stavanger");

        assertThat(stopPlace.getQuays().iterator().next().getName().getValue()).isEqualTo(originalQuayName);
    }

    @Test
    public void quayNameCouldBeNull() throws Exception {

        final String originalStopPlaceName = "Stavanger hpl. 13";

        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString(originalStopPlaceName));

        stopPlace.getQuays().add(new Quay());

        stopPlaceNameNumberToQuayMover.moveNumberEndingToQuay(stopPlace);

        assertThat(stopPlace.getName().getValue()).isEqualTo("Stavanger");

        assertThat(stopPlace.getQuays().iterator().next().getPublicCode()).isEqualTo("13");
    }
}