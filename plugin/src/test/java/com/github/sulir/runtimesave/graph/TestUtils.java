package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.pack.Packer;
import com.github.sulir.runtimesave.pack.ValuePacker;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtils {
    public static void assertGraphsEqual(GraphNode graph, GraphNode other) {
        assertEquals(getBfsTraversal(graph), getBfsTraversal(other));
    }

    public static void assertGraphsNotEqual(GraphNode graph, GraphNode other) {
        assertNotEquals(getBfsTraversal(graph), getBfsTraversal(other));
    }

    public static List<?> getBfsTraversal(GraphNode root) {
        List<Object> traversal = new ArrayList<>();
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

    @SuppressWarnings("unchecked")
    public static <T extends GraphNode> T copyGraph(T root, NodeFactory factory) {
        return (T) copyGraph(root, factory, new HashMap<>());
    }

    private static GraphNode copyGraph(GraphNode node, NodeFactory factory, Map<GraphNode, GraphNode> clones) {
        GraphNode existing = clones.get(node);
        if (existing != null)
            return existing;

        GraphNode clone = factory.createNode(node.label(), node.properties());
        clones.put(node, clone);

        node.forEachEdge((label, target) -> clone.setTarget(label, copyGraph(target, factory, clones)));
        return clone;
    }

    public static void assertPackingReversible(Object object, Packer packer) {
        GraphNode original = new ReflectionReader().read(object);
        GraphNode packable = new ReflectionReader().read(object);
        ValuePacker valuePacker = new ValuePacker(new Packer[]{packer});

        GraphNode packed = valuePacker.pack(packable);
        assertGraphsNotEqual(original, packed);
        GraphNode unpacked = valuePacker.unpack(packed);
        assertGraphsEqual(original, unpacked);
    }
}
