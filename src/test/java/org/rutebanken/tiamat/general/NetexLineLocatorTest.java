package org.rutebanken.tiamat.general;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NetexLineLocatorTest {

    private static final String XML = """
            <PublicationDelivery>
                <Parking version="1"
                         id="FR__40088___Parking__327__LOC">
                    <SiteRef ref="FR:40088:Parking:328:LOC"/>
                </Parking>
                <Parking id='FR:40088:Parking:328:LOC' version="1"/>
                <Parking id="A&amp;B" version="1"/>
            </PublicationDelivery>
            """;

    private final NetexLineLocator locator = new NetexLineLocator(XML);

    @Test
    void findsLineOfIdAttribute() {
        assertThat(locator.findIdLine("FR__40088___Parking__327__LOC")).isEqualTo(3);
    }

    @Test
    void ignoresRefAttributesAndSupportsSingleQuotes() {
        assertThat(locator.findIdLine("FR:40088:Parking:328:LOC")).isEqualTo(6);
    }

    @Test
    void findsIdContainingEscapedCharacters() {
        assertThat(locator.findIdLine("A&B")).isEqualTo(7);
    }

    @Test
    void findsDuplicatedIdsWithTheLineOfEachLaterOccurrence() {
        NetexLineLocator duplicatesLocator = new NetexLineLocator("""
                <members>
                    <Parking id="P1" version="1">
                        <SiteRef ref="P1"/>
                        <ParkingArea id='A1' version="1"/>
                    </Parking>
                    <Parking
                        id="P1" version="2">
                        <ParkingArea id="A1" version="1"/>
                    </Parking>
                    <Parking id="A&amp;B" version="1"/>
                    <Parking id="P1" version="3"/>
                    <Parking id="A&amp;B" version="1"/>
                </members>
                """);

        assertThat(duplicatesLocator.findDuplicatedIds()).containsExactly(
                new DuplicatedIdentifierException.Duplicate("P1", 7),
                new DuplicatedIdentifierException.Duplicate("A1", 8),
                new DuplicatedIdentifierException.Duplicate("P1", 11),
                new DuplicatedIdentifierException.Duplicate("A&B", 12));
    }

    @Test
    void findsNoDuplicateWhenIdsAreUnique() {
        assertThat(locator.findDuplicatedIds()).isEmpty();
        assertThat(new NetexLineLocator(null).findDuplicatedIds()).isEmpty();
    }

    @Test
    void returnsNullWhenIdIsUnknownOrMissing() {
        assertThat(locator.findIdLine("unknown")).isNull();
        assertThat(locator.findIdLine(null)).isNull();
        assertThat(new NetexLineLocator(null).findIdLine("FR:40088:Parking:328:LOC")).isNull();
    }
}
