package org.rutebanken.tiamat.client.mdm;

import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;

import java.util.Map;
import java.util.Set;

public record IdentifierToCheck(Set<Long> stopPlaceMdmIds,
                                Set<Long> quayMdmIds,
                                Map<Long, StopPlace> stopPlaceMap,
                                Map<Long, Quay> quayMap) {

}
