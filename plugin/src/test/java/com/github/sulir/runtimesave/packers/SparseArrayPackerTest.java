package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.TestUtils;
import com.github.sulir.runtimesave.packing.Packer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

class SparseArrayPackerTest {
    @SuppressWarnings("unused")
    private static final Object[] primitives = {
            new char[]{0},
            new byte[]{0, 0},
            new short[]{0, 1},
            new int[]{1, 0},
            new long[]{1, 0, 0},
            new float[]{1, 0, 1},
            new double[]{1, 1, 0},
            new boolean[]{true, false, false, false}
    };

    private Packer packer;

    @BeforeEach
    void setUp() {
        packer = new SparseArrayPacker();
    }

    @Test
    void packingEmptyArrayIsNoop() {
        TestUtils.assertPackingNoop(new Object[]{}, packer);
    }

    @ParameterizedTest
    @FieldSource("primitives")
    void packingPrimitiveArrayIsReversible(Object primitiveArray) {
        TestUtils.assertPackingReversible(primitiveArray, packer);
    }

    @Test
    void packingObjectArrayIsReversible() {
        Object object = new Object();
        TestUtils.assertPackingReversible(new Object[]{null, object, object, null}, packer);
    }

    @Test
    void packingArrayOfArraysIsReversible() {
        TestUtils.assertPackingReversible(new int[][]{{1, 0}, {}}, packer);
    }

    @Test
    void packing3DArrayIsReversible() {
        int[][][] array = {{{0, 1}, null}, {null, null, {1, 0}}, null};
        TestUtils.assertPackingReversible(array, packer);
    }
}