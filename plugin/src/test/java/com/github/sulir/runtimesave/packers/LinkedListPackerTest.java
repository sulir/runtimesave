package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.GraphNode;
import com.github.sulir.runtimesave.graph.GraphTestUtils;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedListPackerTest {
    @Test
    void packingEmptyListIsInverseToUnpacking() {
        assertPackingReversible(new LinkedList<>(List.of()));
    }

    @Test
    void packingTwoItemListIsInverseToUnpacking() {
        assertPackingReversible(new LinkedList<>(List.of("a", "b")));
    }

    private void assertPackingReversible(Object object) {
        GraphNode original = new ReflectionReader().read(object);

        GraphNode packable = new ReflectionReader().read(object);
        ValuePacker packer = new ValuePacker(new Packer[]{new LinkedListPacker()});
        GraphNode transformed = packer.unpack(packer.pack(packable));

        assertTrue(GraphTestUtils.deepEqual(original, transformed));
    }
}
