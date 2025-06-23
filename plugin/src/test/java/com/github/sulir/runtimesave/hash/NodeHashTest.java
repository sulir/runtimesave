package com.github.sulir.runtimesave.hash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;

import static com.github.sulir.runtimesave.UncheckedThrowing.uncheck;
import static org.junit.jupiter.api.Assertions.*;

class NodeHashTest {
    MessageDigest algorithm;
    private NodeHash hashOf1;
    private NodeHash alsoHashOf1;
    private NodeHash hashOf2;

    @BeforeEach
    void setUp() {
        algorithm = uncheck(() -> MessageDigest.getInstance("SHA-224"));
        hashOf1 = new NodeHash(algorithm.digest(new byte[] {1}));
        alsoHashOf1 = new NodeHash(algorithm.digest(new byte[] {1}));
        hashOf2 = new NodeHash(algorithm.digest(new byte[] {2}));
    }

    @Test
    void bytesOfSameHashesAreEqual() {
        assertArrayEquals(hashOf1.getBytes(), alsoHashOf1.getBytes());
    }

    @Test
    void stringHasCorrectLength() {
        int expectedLength = (int) Math.ceil(4 * algorithm.getDigestLength() / 3.0);
        assertEquals(expectedLength, hashOf1.toString().length());
    }

    @Test
    void stringDoesNotHavePadding() {
        assertFalse(hashOf1.toString().contains("="));
    }

    @Test
    void sameHashesProduceSameString() {
        assertEquals(hashOf1.toString(), alsoHashOf1.toString());
    }

    @Test
    void differentHashesProduceDifferentString() {
        assertNotEquals(hashOf1.toString(), hashOf2.toString());
    }

    @Test
    void sameHashesProduceSameShortString() {
        assertEquals(hashOf1.toShortString(), alsoHashOf1.toShortString());
    }

    @Test
    void differentHashesProduceDifferentShortString() {
        assertNotEquals(hashOf1.toShortString(), hashOf2.toShortString());
    }

    @Test
    void toStringRemainsSame() {
        assertEquals(hashOf1.toString(), hashOf1.toString());
    }

    @Test
    void sameHashesAreEqual() {
        assertEquals(hashOf1, alsoHashOf1);
    }

    @Test
    void differentHashesAreNotEqual() {
        assertNotEquals(hashOf1, hashOf2);
    }

    @Test
    void hashDoesNotEqualByteArray() {
        Object bytes = hashOf1.getBytes();
        assertNotEquals(hashOf1, bytes);
    }

    @Test
    void sameHashesHaveSameHashCodes() {
        assertEquals(hashOf1.hashCode(), alsoHashOf1.hashCode());
    }

    @Test
    void differentHashesHaveDifferentHashCodes() {
        assertNotEquals(hashOf1.hashCode(), hashOf2.hashCode());
    }
}
