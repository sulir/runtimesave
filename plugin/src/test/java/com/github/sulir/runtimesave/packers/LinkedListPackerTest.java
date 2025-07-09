package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.TestUtils;
import com.github.sulir.runtimesave.packing.Packer;
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
