/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.service.stopplace;

import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.TreeSet;

/**
 * Identifies stop places (or parent stop places) that are candidates for being merged, based on
 * their proximity and the similarity of their names, and derives a shared identifier for the group.
 */
@Service
public class StopPlaceMergeCandidateFinder {

    // ~50m, consistent with the "nearby" distance used elsewhere for grouping stop places (NearbyStopPlaceQueryBuilder)
    private static final double NEARBY_BUFFER_DEGREES = 0.000449166666667;

    private static final String MERGE_ID_PREFIX = "MERGE-";

    private final StopPlaceRepository stopPlaceRepository;

    public StopPlaceMergeCandidateFinder(StopPlaceRepository stopPlaceRepository) {
        this.stopPlaceRepository = stopPlaceRepository;
    }

    public String findMergeId(StopPlace stopPlace) {
        if (stopPlace == null || !stopPlace.hasCoordinates() || stopPlace.getNetexId() == null) {
            return null;
        }

        String name = stopPlace.getName() != null ? stopPlace.getName().getValue() : null;
        if (StringUtils.isBlank(name)) {
            return null;
        }

        Geometry buffer = stopPlace.getCentroid().buffer(NEARBY_BUFFER_DEGREES);
        Envelope envelope = buffer.getEnvelopeInternal();

        List<String> candidateNetexIds = stopPlaceRepository.findStopPlacesWithSimilarNameNearby(
                envelope, name, stopPlace.isParentStopPlace(), stopPlace.getNetexId());

        if (candidateNetexIds == null || candidateNetexIds.isEmpty()) {
            return null;
        }

        TreeSet<String> group = new TreeSet<>(candidateNetexIds);
        group.add(stopPlace.getNetexId());

        return MERGE_ID_PREFIX + group.first();
    }
}
