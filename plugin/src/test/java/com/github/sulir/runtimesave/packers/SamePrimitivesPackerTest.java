package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.TestUtils;
import com.github.sulir.runtimesave.packing.Packer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}