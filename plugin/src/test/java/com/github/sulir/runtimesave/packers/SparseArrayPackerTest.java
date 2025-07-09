package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.TestUtils;
import com.github.sulir.runtimesave.packing.Packer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SparseArrayPackerTest {
    private Packer packer;

    @BeforeEach
    void setUp() {
        packer = new SparseArrayPacker();
    }

    @Test
    void packingEmptyArrayIsReversible() {
        TestUtils.assertPackingReversible(new boolean[]{}, packer);
    }

    @Test
    void packingPrimitiveArrayIsReversible() {
        TestUtils.assertPackingReversible(new int[]{1, 0, 2, 0, 0, 2}, packer);
    }

    @Test
    void packingObjectArrayIsReversible() {
        Object object = new Object();
        TestUtils.assertPackingReversible(new Object[]{null, object, object, null}, packer);
    }

    @Test
    void packingArrayOfArraysIsReversible() {
        TestUtils.assertPackingReversible(new byte[][]{new byte[]{1}, null}, packer);
    }
}