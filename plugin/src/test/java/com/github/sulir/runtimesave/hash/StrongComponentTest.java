package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.NullNode;
import com.github.sulir.runtimesave.nodes.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StrongComponentTest {
    private final ObjectNode node = new ObjectNode("Test");
    private final NullNode otherNode = new NullNode();

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
    void componentsDoNotReflectNodeEdgesByDefault() {
        node.setField("ref", otherNode);
        StrongComponent component = new StrongComponent(node);
        assertEquals(0, component.targets().size());
    }

    @Test
    void addingTargetIsReflected() {
        node.setField("ref", otherNode);
        StrongComponent component = new StrongComponent(node);
        StrongComponent target = new StrongComponent(otherNode);
        component.addTarget(target);
        assertEquals(List.of(target), component.targets());
    }

    @Test
    void componentsWithSameNodesAreEqual() {
        StrongComponent component = new StrongComponent(node, otherNode);
        StrongComponent equal = new StrongComponent(node, otherNode);
        assertEquals(component, equal);
    }

    @Test
    void componentWithTwoNodesIsNotTrivial() {
        StrongComponent component = new StrongComponent(node, otherNode);
        assertFalse(component.isTrivial());
    }

    @Test
    void componentWithSelfCycleNodeIsNotTrivial() {
        node.setField("selfCycle", node);
        StrongComponent component = new StrongComponent(node);
        assertFalse(component.isTrivial());
    }

    @Test
    void soleNonCycleNodeIsTrivial() {
        StrongComponent component = new StrongComponent(node);
        assertTrue(component.isTrivial());
    }

    @Test
    void gettingSoleNodeFromMultiNodeSccFails() {
        StrongComponent component = new StrongComponent(node, otherNode);
        assertThrows(IllegalStateException.class, component::getSoleNode);
    }

    @Test
    void firstNodeAndRestAreDisjunct() {
        StrongComponent component = new StrongComponent(node, otherNode);
        assertFalse(component.getRestOfNodes().contains(component.getFirstNode()));
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