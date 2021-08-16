package org.rutebanken.tiamat.importer.finder;

import com.okina.mainti4.mainti4apiclient.model.BtDto;

import java.util.Optional;

public interface IMainti4TravauxFinder {
    void updateCache();

    Optional<BtDto> getCacheEntry(String rId);
}
