package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.Edge;
import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.nodes.PrimitiveNode;
import com.github.sulir.runtimesave.packing.Packer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SamePrimitivesPacker implements Packer {
    private final Map<PrimitiveValue, PrimitiveNode> valueToNode = new HashMap<>();

    @Override
    public boolean canPack(ValueNode node) {
        return node instanceof PrimitiveNode;
    }

    @Override
    public ValueNode pack(ValueNode node) {
        PrimitiveNode primitive = (PrimitiveNode) node;
        PrimitiveValue value = new PrimitiveValue(primitive.getValue(), primitive.getType());
        return valueToNode.computeIfAbsent(value, v -> primitive);
    }

    @Override
    public boolean canUnpack(ValueNode node) {
        return node.edgeCount() != 0;
    }

    @Override
    public ValueNode unpack(ValueNode node) {
        List<Edge> edges = node.edges().filter(e -> e.target() instanceof PrimitiveNode).toList();
        edges.forEach(e -> {
            PrimitiveNode packed = (PrimitiveNode) e.target();
            node.setTarget(e.label(), new PrimitiveNode(packed.getValue(), packed.getType()));
        });
        return node;
    }

    private record PrimitiveValue(Object value, String type) { }
}
