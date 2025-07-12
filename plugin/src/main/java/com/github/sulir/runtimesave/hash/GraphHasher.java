package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;

import java.util.*;

public class GraphHasher {
    private final ObjectHasher objectHasher = new ObjectHasher();
    private final LocalHasher localHasher = new LocalHasher(objectHasher);
    private final TreeHasher treeHasher = new TreeHasher(objectHasher);

    private Map<GraphNode, Integer> orders;

    public NodeHash assignHashes(GraphNode graph) {
        return assignHashes(AcyclicGraph.multiCondensationOf(graph));
    }

    public NodeHash assignHashes(AcyclicGraph dag) {
        GraphNode root = dag.getRootNode();
        localHasher.assignLocalHashes(root);
        treeHasher.assignHashes(dag);

        root.traverse(node -> {
            if (!node.hasHash()) {
                objectHasher.reset();
                orders = new HashMap<>();
                computeHash(node);
                node.setHash(new NodeHash(objectHasher.finish()));
            }
        });
        orders = null;
        return root.hash();
    }

    private void computeHash(GraphNode node) {
        orders.put(node, orders.size());
        objectHasher.addHash(node.localHash());

        node.forEachEdge((label, target) -> {
            Integer targetOrder = orders.get(target);
            if (targetOrder == null)
                computeHash(target);
            else
                objectHasher.addInt(orders.get(target));
        });
    }
}
