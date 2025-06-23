package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;
import com.github.sulir.runtimesave.nodes.NullNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StrongComponentTest {
    private final GraphNode node = new NullNode();
    private final GraphNode otherNode = new NullNode();

    @Test
    void sizeOfEmptyComponentIsZero() {
        StrongComponent component = new StrongComponent();
        assertEquals(0, component.nodes().size());
    }

    @Test
    void oneNodeComponentContainsGivenNode() {
        StrongComponent component = new StrongComponent(node);
        assertTrue(component.contains(node));
    }

    @Test
    void oneNodeComponentHasTwoNodesAfterAdding() {
        StrongComponent component = new StrongComponent(node);
        component.add(otherNode);
        assertEquals(2, component.nodes().size());
    }

    @Test
    void componentDoesNotEqualOtherClassInstance() {
        Object nodes = new Object();
        assertNotEquals(new StrongComponent(), nodes);
    }

    @Test
    void sameComponentsHaveSameHashCodes() {
        assertEquals(new StrongComponent(node).hashCode(), new StrongComponent(node).hashCode());
    }

    @Test
    void differentComponentsHaveDifferentHashCodes() {
        assertNotEquals(new StrongComponent(node).hashCode(), new StrongComponent(otherNode).hashCode());
    }

    @Test
    void stringRepresentationIsFormattedCorrectly() {
        StrongComponent component = new StrongComponent(node);
        String expected = "{" + node + "}";
        assertEquals(expected, component.toString());
    }
}