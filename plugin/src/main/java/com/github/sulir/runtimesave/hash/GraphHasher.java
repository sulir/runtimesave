package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class GraphHasher {
    private final TreeHasher treeHasher = new TreeHasher();
    private final ObjectHasher objectHasher = new ObjectHasher();

    public NodeHash assignHashes(GraphNode graph) {
        return assignHashes(AcyclicGraph.multiCondensationOf(graph));
    }

    public NodeHash assignHashes(AcyclicGraph dag) {
        treeHasher.assignHashes(dag);

        GraphNode root = dag.getRootNode();
        root.traverse(node -> {
            if (!node.hasHash())
                node.setHash(computeHash(node));
        });
        return root.hash();
    }

    private NodeHash computeHash(GraphNode node) {
        Queue<GraphNode> toVisit = new ArrayDeque<>();
        toVisit.add(node);
        Map<GraphNode, Integer> orders = new HashMap<>();
        orders.put(node, 0);
        objectHasher.reset();

        while (!toVisit.isEmpty()) {
            GraphNode current = toVisit.remove();
            objectHasher.add(current.label()).add(current.properties());
            current.outEdges().forEach((property, target) -> {
                int targetOrder = orders.computeIfAbsent(target, t -> {
                    toVisit.add(target);
                    return orders.size();
                });
                objectHasher.add(property).add(targetOrder);
            });
        }

        return new NodeHash(objectHasher.finish());
    }
}
