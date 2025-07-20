package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.TestUtils;
import com.github.sulir.runtimesave.nodes.ArrayNode;
import com.github.sulir.runtimesave.pack.Packer;
import com.github.sulir.runtimesave.packers.SparseArrayPacker.SparseArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SparseArrayPackerTest {
    @SuppressWarnings("unused")
    static final Object[] primitives = {
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
    void emptyArrayIsNotPacked() {
        assertFalse(packer.canPack(new ArrayNode("Object[]")));
    }

    @ParameterizedTest
    @FieldSource("primitives")
    void packingPrimitiveArrayIsReversible(Object primitiveArray) {
        TestUtils.assertPackingReversible(primitiveArray, packer);
    }

    @Test
    void packingObjectArrayIsReversible() {
        TestUtils.assertPackingReversible(new Object[]{null, 1, 2, null}, packer);
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

    @Test
    void nodeWithNonArrayTypeCannotBeConstructed() {
        assertThrows(IllegalArgumentException.class, () -> new SparseArrayNode("int", 0));
    }
}
