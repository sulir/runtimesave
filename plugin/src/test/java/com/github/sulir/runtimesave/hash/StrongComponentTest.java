package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;
import com.github.sulir.runtimesave.nodes.NullNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrongComponentTest {
    @Test
    void sizeOfEmptyComponentIsZero() {
        StrongComponent component = new StrongComponent();
        assertEquals(0, component.nodes().size());
    }

    @Test
    void oneNodeComponentContainsGivenNode() {
        GraphNode node = new NullNode();
        StrongComponent component = new StrongComponent(node);
        assertTrue(component.contains(node));
    }

    @Test
    void oneNodeComponentHasTwoNodesAfterAdding() {
        StrongComponent component = new StrongComponent(new NullNode());
        component.add(new NullNode());
        assertEquals(2, component.nodes().size());
    }
}