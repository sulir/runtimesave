package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;

import java.util.*;

public class GraphIdHasher {
    private final ObjectHasher objectHasher = new ObjectHasher();
    private Map<GraphNode, Integer> nodeCopies;

    public NodeHash assignIdHashes(GraphNode graph) {
        nodeCopies = new HashMap<>();

        Map<NodeHash, Integer> copies = new HashMap<>();
        graph.traverse(node -> assignHashCopy(node, copies));

        graph.traverse(node -> {
            objectHasher.reset();
            computeIdHash(node, new HashSet<>());
            node.setIdHash(new NodeHash(objectHasher.finish()));
        });

        nodeCopies = null;
        return graph.idHash();
    }

    private void assignHashCopy(GraphNode node, Map<NodeHash, Integer> copies) {
        int copy = copies.compute(node.hash(), (hash, n) -> n == null ? 0 : n + 1);
        nodeCopies.put(node, copy);
    }

    private void computeIdHash(GraphNode node, Set<GraphNode> visited) {
        if (!visited.add(node))
            return;

        objectHasher.addHash(node.hash())
                .addInt(nodeCopies.get(node));
        node.forEachEdge((label, target) -> computeIdHash(target, visited));
    }
}
