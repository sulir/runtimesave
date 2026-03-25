package io.github.sulir.runtimesave.packers;

import io.github.sulir.runtimesave.graph.TestUtils;
import io.github.sulir.runtimesave.pack.Packer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

class LinkedListPackerTest {
    private Packer packer;

    @BeforeEach
    void setUp() {
        packer = new LinkedListPacker();
    }

    @Test
    void packingEmptyListIsReversible() {
        TestUtils.assertPackingReversible(new LinkedList<>(List.of()), packer);
    }

    @Test
    void packingTwoItemListIsReversible() {
        TestUtils.assertPackingReversible(new LinkedList<>(List.of("a", "b")), packer);
    }
}
