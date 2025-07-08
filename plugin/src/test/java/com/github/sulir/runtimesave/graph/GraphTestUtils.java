package com.github.sulir.runtimesave.graph;

import java.util.*;

public class GraphTestUtils {
    public static boolean deepEqual(GraphNode graph, GraphNode other) {
        return getBfsTraversal(graph).equals(getBfsTraversal(other));
    }

    public static List<?> getBfsTraversal(GraphNode root) {
        List<Object> traversal = new LinkedList<>();
        Queue<GraphNode> toVisit = new ArrayDeque<>();
        toVisit.add(root);
        Map<GraphNode, Integer> orders = new HashMap<>();
        orders.put(root, 0);

        while (!toVisit.isEmpty()) {
            GraphNode node = toVisit.remove();
            traversal.add(node.label());
            traversal.add(Arrays.asList(node.properties()));
            node.forEachEdge((label, target) -> {
                int targetOrder = orders.computeIfAbsent(target, t -> {
                    toVisit.add(target);
                    return orders.size();
                });
                traversal.add(label);
                traversal.add(targetOrder);
            });
        }
        return traversal;
    }
}
