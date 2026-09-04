package org.rutebanken.tiamat.dtoassembling.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StopPlaceMergeCandidatePairDtoTest {

    @Test
    void buildsBothSidesFromOneRow() {
        Object[] row = new Object[] {
                "NSR:StopPlace:1", "Gare A", 10.5, 59.9, "bus", "prov1",
                "NSR:StopPlace:2", "Gare B", 10.50001, 59.90001, "bus", "prov1"
        };

        StopPlaceMergeCandidatePairDto pair = new StopPlaceMergeCandidatePairDto(row);

        assertThat(pair.getBase().getNetexId()).isEqualTo("NSR:StopPlace:1");
        assertThat(pair.getBase().getName()).isEqualTo("Gare A");
        assertThat(pair.getBase().getLongitude()).isEqualTo(10.5);
        assertThat(pair.getBase().getLatitude()).isEqualTo(59.9);
        assertThat(pair.getBase().getModality()).isEqualTo("bus");
        assertThat(pair.getBase().getProvider()).isEqualTo("prov1");

        assertThat(pair.getCandidate().getNetexId()).isEqualTo("NSR:StopPlace:2");
        assertThat(pair.getCandidate().getName()).isEqualTo("Gare B");
    }
}
