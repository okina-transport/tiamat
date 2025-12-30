package org.rutebanken.tiamat.model;

import jakarta.persistence.Embeddable;

@Embeddable
public final class FixedAddressablePlaceRef extends AddressablePlaceRefStructure {
    public FixedAddressablePlaceRef() { super(); }
    public FixedAddressablePlaceRef(String ref, String version) {
        super(ref, version);
    }
}