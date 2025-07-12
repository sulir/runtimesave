package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphIdHasher {
    private final ObjectHasher objectHasher = new ObjectHasher();
    private Map<NodeHash, Integer> copies;
    private Map<GraphNode, Integer> nodeCopies;
    private Set<GraphNode> visited;

    public NodeHash assignIdHashes(GraphNode graph) {
        copies = new HashMap<>();
        nodeCopies = new HashMap<>();
        graph.traverse(this::assignHashCopy);
        graph.traverse(node -> {
            objectHasher.reset();
            visited = new HashSet<>();
            computeIdHash(node);
            node.setIdHash(new NodeHash(objectHasher.finish()));
        });
        copies = null;
        nodeCopies = null;
        visited = null;
        return graph.idHash();
    }

    private void assignHashCopy(GraphNode node) {
        int copy = copies.compute(node.hash(), (hash, n) -> n == null ? 0 : n + 1);
        nodeCopies.put(node, copy);
    }

    private void computeIdHash(GraphNode node) {
        if (!visited.add(node))
            return;

        objectHasher.addHash(node.hash())
                .addInt(nodeCopies.get(node));
        node.forEachEdge((label, target) -> computeIdHash(target));
    }
}
