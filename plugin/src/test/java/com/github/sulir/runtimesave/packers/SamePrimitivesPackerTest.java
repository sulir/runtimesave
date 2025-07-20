package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.TestUtils;
import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.nodes.PrimitiveNode;
import com.github.sulir.runtimesave.pack.Packer;
import com.github.sulir.runtimesave.packers.SamePrimitivesPacker.KeyedWeakRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SamePrimitivesPackerTest {
    private Packer packer;

    @BeforeEach
    void setUp() {
        packer = new SamePrimitivesPacker();
    }

    @Test
    void packingDistinctPrimitivesIsNoop() {
        ValueNode[] nodes = {new PrimitiveNode(0, "int"), new PrimitiveNode(1, "int"), new PrimitiveNode(2, "int")};
        for (ValueNode node : nodes)
            assertEquals(node, packer.pack(node));
    }

    @Test
    void packingTwoPairsIsReversible() {
        TestUtils.assertPackingReversible(new int[]{1, 2, 1, 2}, packer);
    }

    @Test
    void packingNestedPairIsReversible() {
        TestUtils.assertPackingReversible(new Object[]{1, new int[]{1, 2}}, packer);
    }

    @Test
    void packingTripleIsReversible() {
        TestUtils.assertPackingReversible(new int[]{1, 2, 1, 1}, packer);
    }

    @Test
    void packingDuringGarbageCollectionDoesNotCrash() {
        Packer packer = new SamePrimitivesPacker(new TestHashMap(), new TestReferenceQueue());
        assertDoesNotThrow(() -> packer.pack(new PrimitiveNode(1, "int")));
    }

    private static class TestReferenceQueue extends ReferenceQueue<PrimitiveNode> {
        private KeyedWeakRef ref = new KeyedWeakRef(null, null, null);

        @Override
        public Reference<? extends PrimitiveNode> poll() {
            KeyedWeakRef oneTimeReference = ref;
            if (ref != null)
                ref = null;
            return oneTimeReference;
        }
    }

    private static class TestHashMap extends HashMap<Object, KeyedWeakRef> {
        @Override
        public KeyedWeakRef get(Object key) {
            return new KeyedWeakRef(null, null, null);
        }
    }
}