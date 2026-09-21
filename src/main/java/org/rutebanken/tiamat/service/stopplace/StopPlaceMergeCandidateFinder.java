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

import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Groups stop places that are candidates for being merged into numbered merge groups: same transport mode, and
 * either an exact centroid match or an exact name match within the merge candidate distance threshold, regardless
 * of provider (candidates can come from a different provider than the given stop place, e.g. duplicates left over
 * from a re-import under a new provider). All stop places belonging to the same duplicate cluster - including
 * duplicates only indirectly connected through another member of the cluster - share the same merge group number.
 */
@Service
public class StopPlaceMergeCandidateFinder {

    private final StopPlaceRepository stopPlaceRepository;

    public StopPlaceMergeCandidateFinder(StopPlaceRepository stopPlaceRepository) {
        this.stopPlaceRepository = stopPlaceRepository;
    }

    /**
     * Computes the merge group number of every stop place currently involved in at least one duplicate, as the
     * connected components of the merge candidate graph. Meant to be computed once and reused for every stop place
     * resolved within the same request, since it scans the whole database.
     */
    public Map<String, String> computeMergeGroups() {
        List<Pair<String, String>> edges = stopPlaceRepository.findAllMergeableStopPlacePairs();

        Map<String, String> parentByNetexId = new HashMap<>();
        for (Pair<String, String> edge : edges) {
            parentByNetexId.putIfAbsent(edge.getFirst(), edge.getFirst());
            parentByNetexId.putIfAbsent(edge.getSecond(), edge.getSecond());
        }
        for (Pair<String, String> edge : edges) {
            union(parentByNetexId, edge.getFirst(), edge.getSecond());
        }

        Map<String, List<String>> clustersByRoot = new HashMap<>();
        for (String netexId : parentByNetexId.keySet()) {
            clustersByRoot.computeIfAbsent(find(parentByNetexId, netexId), root -> new ArrayList<>()).add(netexId);
        }

        List<List<String>> orderedClusters = new ArrayList<>(clustersByRoot.values());
        orderedClusters.forEach(Collections::sort);
        orderedClusters.sort(Comparator.comparing(cluster -> cluster.get(0)));

        Map<String, String> mergeGroupByNetexId = new HashMap<>();
        for (int i = 0; i < orderedClusters.size(); i++) {
            String mergeGroup = String.valueOf(i + 1);
            for (String netexId : orderedClusters.get(i)) {
                mergeGroupByNetexId.put(netexId, mergeGroup);
            }
        }
        return mergeGroupByNetexId;
    }

    public String findMergeId(StopPlace stopPlace, Map<String, String> mergeGroups) {
        if (stopPlace == null || stopPlace.getNetexId() == null) {
            return null;
        }
        return mergeGroups.get(stopPlace.getNetexId());
    }

    private static String find(Map<String, String> parentByNetexId, String netexId) {
        String root = netexId;
        while (!parentByNetexId.get(root).equals(root)) {
            root = parentByNetexId.get(root);
        }
        while (!parentByNetexId.get(netexId).equals(root)) {
            String next = parentByNetexId.get(netexId);
            parentByNetexId.put(netexId, root);
            netexId = next;
        }
        return root;
    }

    private static void union(Map<String, String> parentByNetexId, String a, String b) {
        String rootA = find(parentByNetexId, a);
        String rootB = find(parentByNetexId, b);
        if (!rootA.equals(rootB)) {
            parentByNetexId.put(rootA, rootB);
        }
    }
}
