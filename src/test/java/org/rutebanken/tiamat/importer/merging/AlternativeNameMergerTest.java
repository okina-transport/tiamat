package org.rutebanken.tiamat.importer.merging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rutebanken.tiamat.model.AlternativeName;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.StopPlace;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AlternativeNameMergerTest {

    private final AlternativeNameMerger alternativeNameMerger = new AlternativeNameMerger();

    @Test
    public void updateSiteElementAlternativeName_noAlternativeName_test() {
        StopPlace stopPlace = new StopPlace();
        StopPlace copy = new StopPlace();

        boolean result = alternativeNameMerger.updateSiteElementAlternativeName(stopPlace, copy);

        assertThat(result).isFalse();
    }

    @Test
    public void updateSiteElementAlternativeName_noAlternativeNameChange_test() {
        AlternativeName alternativeName = new AlternativeName();
        EmbeddableMultilingualString multilingualString = new EmbeddableMultilingualString();
        multilingualString.setValue("Stop name");
        alternativeName.setTypeOfName("Libellé de la synthèse vocale");
        alternativeName.setName(multilingualString);

        StopPlace stopPlace = new StopPlace();
        stopPlace.getAlternativeNames().add(alternativeName);
        StopPlace copy = new StopPlace();
        copy.getAlternativeNames().add(alternativeName);

        boolean result = alternativeNameMerger.updateSiteElementAlternativeName(stopPlace, copy);

        assertThat(result).isFalse();
    }

    @Test
    public void updateSiteElementAlternativeName_alternativeNameChanged_test() {
        AlternativeName alternativeName = new AlternativeName();
        EmbeddableMultilingualString multilingualString = new EmbeddableMultilingualString();
        multilingualString.setValue("Stop name");
        alternativeName.setTypeOfName("Libellé de la synthèse vocale");
        alternativeName.setName(multilingualString);

        AlternativeName newAlternativeName = new AlternativeName();
        EmbeddableMultilingualString newMultilingualString = new EmbeddableMultilingualString();
        newMultilingualString.setValue("Stop name changed");
        newAlternativeName.setTypeOfName("Libellé de la synthèse vocale");
        newAlternativeName.setName(newMultilingualString);

        StopPlace stopPlace = new StopPlace();
        stopPlace.getAlternativeNames().add(newAlternativeName);
        StopPlace copy = new StopPlace();
        copy.getAlternativeNames().add(alternativeName);

        boolean result = alternativeNameMerger.updateSiteElementAlternativeName(stopPlace, copy);

        assertThat(result).isTrue();
    }

    @Test
    public void updateSiteElementAlternativeName_alternativeNameCreated_test() {
        AlternativeName newAlternativeName = new AlternativeName();
        EmbeddableMultilingualString newMultilingualString = new EmbeddableMultilingualString();
        newMultilingualString.setValue("Stop name changed");
        newAlternativeName.setTypeOfName("Libellé de la synthèse vocale");
        newAlternativeName.setName(newMultilingualString);

        StopPlace stopPlace = new StopPlace();
        stopPlace.getAlternativeNames().add(newAlternativeName);
        StopPlace copy = new StopPlace();

        boolean result = alternativeNameMerger.updateSiteElementAlternativeName(stopPlace, copy);

        assertThat(result).isTrue();
        assertThat(copy.getAlternativeNames()).isNotEmpty().hasSize(1);
    }

    @Test
    public void updateSiteElementAlternativeName_alternativeNameRemoved_test() {
        AlternativeName formerAlternativeName = new AlternativeName();
        EmbeddableMultilingualString newMultilingualString = new EmbeddableMultilingualString();
        newMultilingualString.setValue("Stop name changed");
        formerAlternativeName.setTypeOfName("Libellé de la synthèse vocale");
        formerAlternativeName.setName(newMultilingualString);

        StopPlace stopPlace = new StopPlace();
        StopPlace copy = new StopPlace();
        copy.getAlternativeNames().add(formerAlternativeName);

        boolean result = alternativeNameMerger.updateSiteElementAlternativeName(stopPlace, copy);

        assertThat(result).isTrue();
        assertThat(copy.getAlternativeNames()).isEmpty();
    }

}