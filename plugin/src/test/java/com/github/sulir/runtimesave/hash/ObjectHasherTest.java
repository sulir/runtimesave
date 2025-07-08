package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.NodeProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObjectHasherTest {
    final static Object[][] primitives = {{'a', 'b'}, {(byte) 1, (byte) 2}, {(short) 1, (short) 2},
            {1, 2}, {1L, 2L}, {1.0f, 2.0f}, {1.0, 2.0}, {true, false}};
    private ObjectHasher hasher;
    private ObjectHasher secondHasher;

    @BeforeEach
    void setUp() {
        hasher = new ObjectHasher();
        secondHasher = new ObjectHasher();
    }

    @ParameterizedTest
    @FieldSource("primitives")
    void samePrimitivesHaveSameHash(Object primitive) {
        assertArrayEquals(hasher.hash(primitive), hasher.hash(primitive));
    }

    @ParameterizedTest
    @FieldSource("primitives")
    void differentPrimitivesHaveDifferentHashes(Object primitive, Object differentPrimitive) {
        assertFalse(Arrays.equals(hasher.hash(primitive), hasher.hash(differentPrimitive)));
    }

    @Test
    void sameStringsHaveSameHashes() {
        assertArrayEquals(hasher.hash("same"), hasher.hash("same"));
    }

    @Test
    void differentStringsHaveDifferentHashes() {
        assertFalse(Arrays.equals(hasher.hash("test\uD83D\uDE00"), hasher.hash("test12")));
    }

    @Test
    void sameEnumValuesHaveSameHashes() {
        assertArrayEquals(hasher.hash(SampleEnum.ONE), hasher.hash(SampleEnum.ONE));
    }

    @Test
    void differentEnumValuesHaveDifferentHashes() {
        assertFalse(Arrays.equals(hasher.hash(SampleEnum.ONE), hasher.hash(SampleEnum.TWO)));
    }

    @Test
    void samePropertiesHaveSameHashes() {
        NodeProperty[] properties = {new NodeProperty("k", "v"), new NodeProperty("k2", "v")};
        assertArrayEquals(hasher.hash(properties), hasher.hash(properties));
    }

    @Test
    void differentPropertiesHaveDifferentHashes() {
        NodeProperty[] first = {new NodeProperty("key", "value")};
        NodeProperty[] second = {new NodeProperty("key", "different")};
        assertFalse(Arrays.equals(hasher.hash(first), hasher.hash(second)));
    }

    @Test
    void sameHashesHaveSameHashes() {
        NodeHash hash = new NodeHash(hasher.hash("test"));
        NodeHash sameHash = new NodeHash(hasher.hash("test"));
        assertArrayEquals(hasher.hash(hash), hasher.hash(sameHash));
    }

    @Test
    void differentHashesHaveDifferentHashes() {
        NodeHash firstHash = new NodeHash(hasher.hash("test"));
        NodeHash otherHash = new NodeHash(hasher.hash("different"));
        assertFalse(Arrays.equals(hasher.hash(firstHash), hasher.hash(otherHash)));
    }

    @Test
    void unsupportedTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(List.of(new Object())));
    }

    @Test
    void sameNumbersOfDifferentTypesHaveDifferentHashes() {
        assertFalse(Arrays.equals(hasher.hash(1), hasher.hash(1L)));
    }

    @Test
    void sameObjectSequencesHaveSameHashes() {
        for (Object[] pair : primitives) {
            hasher.add(pair[0]);
            secondHasher.add(pair[0]);
        }
        assertArrayEquals(hasher.finish(), secondHasher.finish());
    }

    @Test
    void differentObjectSequencesHaveDifferentHashes() {
        for (Object[] pair : primitives) {
            hasher.add(pair[0]);
            secondHasher.add(pair[1]);
        }
        assertFalse(Arrays.equals(hasher.finish(), secondHasher.finish()));
    }

    @Test
    void gettingHashResetsState() {
        assertFalse(Arrays.equals(hasher.addString("").finish(), hasher.finish()));
    }

    private enum SampleEnum { ONE, TWO }
}
