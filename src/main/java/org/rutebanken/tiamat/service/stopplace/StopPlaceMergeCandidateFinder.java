package org.rutebanken.tiamat.service.stopplace;

import org.rutebanken.tiamat.dtoassembling.dto.MergeMode;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StopPlaceMergeCandidateFinder {

    private final StopPlaceRepository stopPlaceRepository;

    public StopPlaceMergeCandidateFinder(StopPlaceRepository stopPlaceRepository) {
        this.stopPlaceRepository = stopPlaceRepository;
    }

    public Map<String, String> computeMergeGroups() {
        List<Pair<String, String>> edges = stopPlaceRepository.findMergeableStopPlaces(MergeMode.MULTI_PROVIDER, null, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent()
                .stream()
                .map(pair -> Pair.of(pair.getBase().getNetexId(), pair.getCandidate().getNetexId()))
                .toList();

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
