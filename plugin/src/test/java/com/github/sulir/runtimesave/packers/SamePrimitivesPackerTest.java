package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.TestUtils;
import com.github.sulir.runtimesave.nodes.PrimitiveNode;
import com.github.sulir.runtimesave.packing.Packer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SamePrimitivesPackerTest {
    private Packer packer;

    @BeforeEach
    void setUp() {
        packer = new SamePrimitivesPacker();
    }

    @Test
    void packingDistinctPrimitivesIsNoop() {
        TestUtils.assertPackingNoop(new int[]{0, 1, 2}, packer);
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
        private SamePrimitivesPacker.KeyedWeakRef ref = new SamePrimitivesPacker.KeyedWeakRef(null, null, null);

        @Override
        public Reference<? extends PrimitiveNode> poll() {
            SamePrimitivesPacker.KeyedWeakRef oneTimeReference = ref;
            if (ref != null)
                ref = null;
            return oneTimeReference;
        }
    }

    private static class TestHashMap extends HashMap<Object, SamePrimitivesPacker.KeyedWeakRef> {
        @Override
        public SamePrimitivesPacker.KeyedWeakRef get(Object key) {
            return new SamePrimitivesPacker.KeyedWeakRef(null, null, null);
        }
    }
}