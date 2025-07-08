package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphIdHasher {
    private final ObjectHasher hasher = new ObjectHasher();
    private Map<NodeHash, Integer> copies;
    private Map<GraphNode, Integer> nodeCopies;
    private Set<GraphNode> visited;

    public NodeHash assignIdHashes(GraphNode graph) {
        copies = new HashMap<>();
        nodeCopies = new HashMap<>();
        graph.traverse(this::assignHashCopy);
        graph.traverse(node -> {
            hasher.reset();
            visited = new HashSet<>();
            computeIdHash(node);
            node.setIdHash(new NodeHash(hasher.finish()));
        });
        return graph.idHash();
    }

    private void computeIdHash(GraphNode node) {
        visited.add(node);
        hasher.add(node.hash()).add(nodeCopies.get(node));

        if (node.edgeCount() != 0) {
            hasher.add(Marker.TARGETS_START);
            node.forEachEdge((property, target) -> {
                if (!visited.contains(target)) {
                    hasher.add(property);
                    computeIdHash(target);
                }
            });
            hasher.add(Marker.TARGETS_END);
        }
    }

    private void assignHashCopy(GraphNode node) {
        int copy = copies.compute(node.hash(), (hash, n) -> n == null ? 0 : n + 1);
        nodeCopies.put(node, copy);
    }

    private enum Marker { TARGETS_START, TARGETS_END }
}
