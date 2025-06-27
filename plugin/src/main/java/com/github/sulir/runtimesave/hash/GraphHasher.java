package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class GraphHasher {
    private final ObjectHasher hasher = new ObjectHasher();

    public NodeHash assignHashes(GraphNode root) {
        root.traverse(GraphNode::freeze);
        root.traverse(node -> node.setHash(computeHash(node)));
        return root.hash();
    }

    private NodeHash computeHash(GraphNode node) {
        Queue<GraphNode> toVisit = new ArrayDeque<>();
        toVisit.add(node);
        Map<GraphNode, Integer> orders = new HashMap<>();
        orders.put(node, 0);
        hasher.reset();

        while (!toVisit.isEmpty()) {
            GraphNode current = toVisit.remove();
            hasher.add(current.label()).add(current.properties());
            current.outEdges().forEach((property, target) -> {
                int targetOrder = orders.computeIfAbsent(target, t -> {
                    toVisit.add(target);
                    return orders.size();
                });
                hasher.add(property).add(targetOrder);
            });
        }

        return new NodeHash(hasher.finish());
    }
}
