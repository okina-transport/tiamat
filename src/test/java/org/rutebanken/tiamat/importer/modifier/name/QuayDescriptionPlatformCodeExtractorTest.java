package org.rutebanken.tiamat.importer.modifier.name;

import org.junit.Test;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class QuayDescriptionPlatformCodeExtractorTest {

    private static final QuayDescriptionPlatformCodeExtractor extractor = new QuayDescriptionPlatformCodeExtractor();

    @Test
    public void plattform25() {
        Quay quay = quayWithDescription("Plattform 25");
        assertThat(quay.getPublicCode()).isEqualTo("25");
    }

    @Test
    public void plattform19b() {
        Quay quay = quayWithDescription("Plattform 19b");
        assertThat(quay.getPublicCode()).isEqualTo("19b");
    }

    @Test
    public void emptyDescriptionIfNothingLeft() {
        Quay quay = quayWithDescription("Plattform 19b");
        assertThat(quay.getDescription()).isNull();
    }

    @Test
    public void plfAWithDescriptikon() {
        Quay quay = quayWithDescription("Plf. A  ved apoteket");
        assertThat(quay.getPublicCode()).isEqualTo("A");
        assertThat(quay.getDescription().getValue()).isEqualTo("ved apoteket");
    }

    @Test
    public void plfNWithDashAndDescriptikon() {
        Quay quay = quayWithDescription("Plf. N - mot øst");
        assertThat(quay.getPublicCode()).isEqualTo("N");
        assertThat(quay.getDescription().getValue()).isEqualTo("mot øst");
    }
    @Test
    public void gateterminalenPlf4() {
        Quay quay = quayWithDescription("gateterminalen plf. 4");
        assertThat(quay.getPublicCode()).isEqualTo("4");
        assertThat(quay.getDescription().getValue()).isEqualTo("gateterminalen");
    }

    @Test
    public void ignoreIfDescriptionIsNull() {
        Quay quay = new Quay();
        extractor.extractPlatformCode(quay);
    }

    @Test
    public void plfG() {
        Quay quay = quayWithDescription("Plattform G");
        assertThat(quay.getPublicCode()).isEqualTo("G");
    }

    private Quay quayWithDescription(String description) {
        Quay quay = new Quay();
        quay.setDescription(new EmbeddableMultilingualString(description));
        extractor.extractPlatformCode(quay);
        return quay;
    }

}