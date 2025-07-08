package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;

import java.util.*;

public class GraphHasher {
    private final TreeHasher treeHasher = new TreeHasher();
    private final ObjectHasher hasher = new ObjectHasher();
    private Map<GraphNode, Integer> orders;

    public NodeHash assignHashes(GraphNode graph) {
        return assignHashes(AcyclicGraph.multiCondensationOf(graph));
    }

    public NodeHash assignHashes(AcyclicGraph dag) {
        treeHasher.assignHashes(dag);

        GraphNode root = dag.getRootNode();
        root.traverse(node -> {
            if (!node.hasHash()) {
                hasher.reset();
                orders = new HashMap<>();
                computeHash(node);
                node.setHash(new NodeHash(hasher.finish()));
            }
        });
        return root.hash();
    }

    private void computeHash(GraphNode node) {
        orders.put(node, orders.size());
        hasher.add(node.label()).add(node.properties());

        if (node.edgeCount() != 0) {
            hasher.add(Marker.TARGETS_START);
            node.forEachEdge((property, target) -> {
                hasher.add(property);
                Integer targetOrder = orders.get(target);
                if (targetOrder == null)
                    computeHash(target);
                else
                    hasher.add(orders.get(target));
            });
            hasher.add(Marker.TARGETS_END);
        }
    }

    private enum Marker { TARGETS_START, TARGETS_END }
}
