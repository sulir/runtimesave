package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.hash.GraphHasher;
import com.github.sulir.runtimesave.hash.NodeHash;
import com.github.sulir.runtimesave.graph.GraphNode;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        GraphHasher hasher = new GraphHasher();

        GraphNode original = new ReflectionReader().read(object);
        NodeHash originalHash = hasher.assignHashes(original);

        GraphNode packable = new ReflectionReader().read(object);
        ValuePacker packer = new ValuePacker(new Packer[]{new LinkedListPacker()});
        GraphNode transformed = packer.unpack(packer.pack(packable));
        NodeHash transformedHash = hasher.assignHashes(transformed);

        assertEquals(originalHash, transformedHash);
    }
}
