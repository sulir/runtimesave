package com.github.sulir.runtimesave.hash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.*;

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
    void nullHashIsSame() {
        assertArrayEquals(hasher.hash(null), hasher.hash(null));
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
    void sameListsHaveSameHashes() {
        assertArrayEquals(hasher.hash(List.of(1, 2, 3)), hasher.hash(List.of(1, 2, 3)));
    }

    @Test
    void differentListsHaveDifferentHashes() {
        assertFalse(Arrays.equals(hasher.hash(List.of(1, 2, 3)), hasher.hash(List.of(3, 2, 1))));
    }

    @Test
    void sameNestedListsHaveSameHashes() {
        List<Object> list = List.of(List.of(1), List.of(3, 4), "str", 5.0);
        List<Object> sameList = new ArrayList<>(list);
        assertArrayEquals(hasher.hash(list), hasher.hash(sameList));
    }

    @Test
    void differentNestedListsHaveDifferentHashes() {
        List<Object> firstList = List.of(List.of(1), List.of(3, 4), "str", 5.0);
        List<Object> otherList = List.of(List.of(1), List.of(3, "difference"), "str", 5.0);
        assertFalse(Arrays.equals(hasher.hash(firstList), hasher.hash(otherList)));
    }

    @Test
    void sameMapsHaveSameHashes() {
        SortedMap<String, Integer> map = new TreeMap<>(Map.of("key1", 1, "key2", 2));
        SortedMap<String, Integer> sameMap = new TreeMap<>(map);
        assertArrayEquals(hasher.hash(map), hasher.hash(sameMap));
    }

    @Test
    void differentMapsHaveDifferentHashes() {
        SortedMap<String, Integer> firstMap = new TreeMap<>(Map.of("key1", 1, "key2", 2));
        SortedMap<String, Integer> otherMap = new TreeMap<>(Map.of("key1", 1, "key2", 3));
        assertFalse(Arrays.equals(hasher.hash(firstMap), hasher.hash(otherMap)));
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
    void nonHashByteArraysAreUnsupported() {
        byte[] threeBytes = new byte[3];
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(threeBytes));
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
            hasher.addPrimitive(pair[0]);
            secondHasher.addPrimitive(pair[0]);
        }
        assertArrayEquals(hasher.finish(), secondHasher.finish());
    }

    @Test
    void differentObjectSequencesHaveDifferentHashes() {
        for (Object[] pair : primitives) {
            hasher.addPrimitive(pair[0]);
            secondHasher.addPrimitive(pair[1]);
        }
        assertFalse(Arrays.equals(hasher.finish(), secondHasher.finish()));
    }

    @Test
    void gettingHashResetsState() {
        assertFalse(Arrays.equals(hasher.addString("").finish(), hasher.finish()));
    }
}
