package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.TestUtils;
import com.github.sulir.runtimesave.nodes.ArrayNode;
import com.github.sulir.runtimesave.nodes.PrimitiveNode;
import com.github.sulir.runtimesave.pack.Packer;
import com.github.sulir.runtimesave.packers.PrimitiveArrayPacker.PrimitiveArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static com.github.sulir.runtimesave.graph.TestUtils.assertGraphsEqual;
import static org.junit.jupiter.api.Assertions.*;

class PrimitiveArrayPackerTest {
    @SuppressWarnings("unused")
    static final String[] primitiveArrayTypes = {"char[]", "byte[]", "short[]", "int[]", "long[]",
            "float[]", "double[]", "boolean[]"};

    private Packer packer;

    @BeforeEach
    void setUp() {
        packer = new PrimitiveArrayPacker();
    }

    @Test
    void primitiveArrayWithOneElementIsPacked() {
        ArrayNode unpacked = new ArrayNode("int[]");
        unpacked.addElement(new PrimitiveNode(1, "int"));

        PrimitiveArrayNode packed = new PrimitiveArrayNode("int[]", List.of(1));
        assertGraphsEqual(packed, packer.pack(unpacked));
    }

    @Test
    void emptyArrayIsNotPacked() {
        assertFalse(packer.canPack(new ArrayNode("int[]")));
    }

    @Test
    void arrayOfPrimitiveArraysIsNotPacked() {
        ArrayNode array = new ArrayNode("int[][]");
        array.addElement(new ArrayNode("int[]"));
        assertFalse(packer.canPack(array));
    }

    @Test
    void primitiveArrayWithTwoElementsIsUnpacked() {
        PrimitiveArrayNode packed = new PrimitiveArrayNode("char[]", List.of('a', 'b'));

        ArrayNode unpacked = new ArrayNode("char[]");
        unpacked.addElement(new PrimitiveNode('a', "char"));
        unpacked.addElement(new PrimitiveNode('b', "char"));
        assertGraphsEqual(unpacked, packer.unpack(packed));
    }

    @Test
    void ordinaryArrayNodeIsNotUnpacked() {
        assertFalse(packer.canUnpack(new ArrayNode("int[]")));
    }

    @Test
    void packingIntArrayIsReversible() {
        TestUtils.assertPackingReversible(new int[]{3, 2, 1, 0}, packer);
    }

    @ParameterizedTest
    @FieldSource("primitiveArrayTypes")
    void nodeWithPrimitiveArrayTypeCanBeConstructed(String type) {
        assertDoesNotThrow(() -> new PrimitiveArrayNode(type, List.of()));
    }

    @Test
    void nodeWithNonArrayTypeCannotBeConstructed() {
        assertThrows(IllegalArgumentException.class, () ->
                new PrimitiveArrayNode("int", List.of()));
    }

    @Test
    void nodeWithArrayOfArraysTypeCannotBeConstructed() {
        assertThrows(IllegalArgumentException.class, () ->
                new PrimitiveArrayNode("int[][]", List.of()));
    }
}
