package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TarjanSccTest {
    @Test
    void sccOfOneNodeIsItself() {
        GraphNode node = new NullNode();
        List<StrongComponent> components = new TarjanScc(node).computeComponents();
        assertEquals(List.of(new StrongComponent(node)), components);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void circularGraphHasOneScc(int size) {
        List<ArrayNode> cycle = createCircularGraph(size);
        List<StrongComponent> components = new TarjanScc(cycle.get(0)).computeComponents();
        List<StrongComponent> expected = List.of(new StrongComponent(cycle));
        assertEquals(expected, components);
    }

    @Test
    void cycleAndSinkHaveTwoSCCs() {
        List<ArrayNode> cycle = createCircularGraph(3);
        NullNode sink = new NullNode();
        cycle.get(1).setElement(1, sink);

        List<StrongComponent> components = new TarjanScc(cycle.get(0)).computeComponents();
        List<StrongComponent> expected = List.of(new StrongComponent(sink), new StrongComponent(cycle));
        assertEquals(expected, components);
    }

    @Test
    void twoCyclesConnectedViaNodeWayHaveThreeSCCs() {
        List<ArrayNode> left = createCircularGraph(5);
        List<ArrayNode> right = createCircularGraph(4);
        ObjectNode middle = new ObjectNode("Between");
        left.get(2).setElement(1, middle);
        middle.setField("oneWay", right.get(2));

        List<StrongComponent> components = new TarjanScc(left.get(3)).computeComponents();
        List<StrongComponent> expected = List.of(new StrongComponent(right), new StrongComponent(middle),
                new StrongComponent(left));
        assertEquals(expected, components);
    }

    @Test
    void threeNodeDagHasThreeSCCs() {
        ObjectNode top = new ObjectNode("TwoTargets");
        ObjectNode middle = new ObjectNode("OneTarget");
        StringNode bottom = new StringNode("Leaf");
        top.setField("one", middle);
        top.setField("two", bottom);
        middle.setField("one", bottom);

        List<StrongComponent> components = new TarjanScc(top).computeComponents();
        List<StrongComponent> expected = Stream.of(bottom, middle, top).map(StrongComponent::new).toList();
        assertEquals(expected, components);
    }

    @Test
    void fourNodeTreeHasFourSCCs() {
        ObjectNode root = new ObjectNode("Root");
        ObjectNode left = new ObjectNode("Node");
        ObjectNode right = new ObjectNode("Node");
        PrimitiveNode childOfLeft = new PrimitiveNode(1, "int");
        root.setField("left", left);
        root.setField("right", right);
        left.setField("left", childOfLeft);

        List<StrongComponent> components = new TarjanScc(root).computeComponents();
        List<StrongComponent> expected = Stream.of(childOfLeft, left, right, root).map(StrongComponent::new).toList();
        assertEquals(expected, components);
    }

    private List<ArrayNode> createCircularGraph(int size) {
        List<ArrayNode> nodes = new ArrayList<>();
        for (int i = 0; i < size; i++)
            nodes.add(new ArrayNode("Cls[]"));

        for (int i = 0; i < size; i++)
            nodes.get(i).setElement(0, nodes.get((i + 1) % size));

        return nodes;
    }
}