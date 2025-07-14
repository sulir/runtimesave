package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.Edge;
import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.nodes.PrimitiveNode;
import com.github.sulir.runtimesave.pack.Packer;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SamePrimitivesPacker implements Packer {
    private final Map<Object, KeyedWeakRef> valueToNodeRef;
    private final ReferenceQueue<PrimitiveNode> queue;

    public SamePrimitivesPacker() {
        this(new HashMap<>(), new ReferenceQueue<>());
    }

    SamePrimitivesPacker(Map<Object, KeyedWeakRef> valueToNodeRef, ReferenceQueue<PrimitiveNode> queue) {
        this.valueToNodeRef = valueToNodeRef;
        this.queue = queue;
    }

    @Override
    public boolean canPack(ValueNode node) {
        return node instanceof PrimitiveNode;
    }

    @Override
    public ValueNode pack(ValueNode node) {
        PrimitiveNode primitive = (PrimitiveNode) node;
        removeUnreachable();

        KeyedWeakRef reference = valueToNodeRef.get(primitive.getValue());
        if (reference != null) {
            PrimitiveNode existing = reference.get();
            if (existing != null)
                return existing;
        }

        KeyedWeakRef ref = new KeyedWeakRef(primitive, queue, primitive.getValue());
        valueToNodeRef.put(primitive.getValue(), ref);
        return node;
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

    static class KeyedWeakRef extends WeakReference<PrimitiveNode> {
        final Object key;

        public KeyedWeakRef(PrimitiveNode node, ReferenceQueue<PrimitiveNode> queue, Object key) {
            super(node, queue);
            this.key = key;
        }
    }

    private void removeUnreachable() {
        KeyedWeakRef reference;
        while ((reference = (KeyedWeakRef) queue.poll()) != null)
            valueToNodeRef.remove(reference.key);
    }
}
