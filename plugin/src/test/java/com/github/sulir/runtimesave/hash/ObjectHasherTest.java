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
    void differentPrimitivesHaveDifferentHashes(Object primitive, Object different) {
        assertFalse(Arrays.equals(hasher.hash(primitive), hasher.hash(different)));
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
    void samePropertiesHaveSameHashes() {
        NodeProperty[] properties = {new NodeProperty("k", "v"), new NodeProperty("k2", "v")};
        assertArrayEquals(hasher.hash(properties), hasher.hash(properties));
    }

    @Test
    void differentPropertiesHaveDifferentHashes() {
        NodeProperty[] properties = {new NodeProperty("key", "value")};
        NodeProperty[] different = {new NodeProperty("key", "different")};
        assertFalse(Arrays.equals(hasher.hash(properties), hasher.hash(different)));
    }

    @Test
    void sameHashesHaveSameHashes() {
        NodeHash hash = new NodeHash(hasher.hash("test"));
        NodeHash same = new NodeHash(hasher.hash("test"));
        assertArrayEquals(hasher.hash(hash), hasher.hash(same));
    }

    @Test
    void differentHashesHaveDifferentHashes() {
        NodeHash hash = new NodeHash(hasher.hash("test"));
        NodeHash different = new NodeHash(hasher.hash("different"));
        assertFalse(Arrays.equals(hasher.hash(hash), hasher.hash(different)));
    }

    @Test
    void sameListsHaveSameHashes() {
        List<?> list = List.of("a", List.of());
        List<?> same = List.of("a", List.of());
        assertArrayEquals(hasher.hash(list), hasher.hash(same));
    }

    @Test
    void differentListsHaveDifferentHashes() {
        List<?> list = List.of(1, 2);
        List<?> different = List.of(1.0, 2L);
        assertFalse(Arrays.equals(hasher.hash(list), hasher.hash(different)));
    }

    @Test
    void unsupportedTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(new Object()));
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
}
