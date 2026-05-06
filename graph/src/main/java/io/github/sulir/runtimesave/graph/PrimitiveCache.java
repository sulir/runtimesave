package io.github.sulir.runtimesave.graph;

import io.github.sulir.runtimesave.nodes.PrimitiveNode;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class PrimitiveCache {
    private final Map<Object, KeyedWeakRef> valueToNodeRef = new HashMap<>();
    private final ReferenceQueue<PrimitiveNode> queue = new ReferenceQueue<>();

    public PrimitiveNode deduplicate(PrimitiveNode node) {
        removeUnreachable();

        KeyedWeakRef reference = valueToNodeRef.get(node.getValue());
        if (reference != null) {
            PrimitiveNode existing = reference.get();
            if (existing != null)
                return existing;
        }

        KeyedWeakRef ref = new KeyedWeakRef(node, queue, node.getValue());
        valueToNodeRef.put(node.getValue(), ref);
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
