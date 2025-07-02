package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TarjanSccTest {
    private TarjanScc tarjanScc;
    private TestGraphGenerator generator;

    @BeforeEach
    void setUp() {
        tarjanScc = new TarjanScc();
        generator = new TestGraphGenerator();
    }

    @Test
    void sccOfOneNodeIsItself() {
        GraphNode node = new NullNode();
        List<StrongComponent> components = tarjanScc.computeComponents(node);
        assertEquals(List.of(new StrongComponent(node)), components);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void circularGraphHasOneScc(int size) {
        ArrayNode[] cycle = generator.circularNodes(size);
        List<StrongComponent> components = tarjanScc.computeComponents(cycle[0]);
        List<StrongComponent> expected = List.of(new StrongComponent(cycle));
        assertEquals(expected, components);
    }

    @Test
    void cycleAndSinkHaveTwoSCCs() {
        ArrayNode[] cycle = generator.circularNodes(3);
        NullNode sink = new NullNode();
        cycle[1].setElement(1, sink);

        List<StrongComponent> components = tarjanScc.computeComponents(cycle[0]);
        List<StrongComponent> expected = List.of(new StrongComponent(sink), new StrongComponent(cycle));
        assertEquals(expected, components);
    }

    @Test
    void twoCyclesConnectedViaNodeWayHaveThreeSCCs() {
        ArrayNode[] left = generator.circularNodes(5);
        ArrayNode[] right = generator.circularNodes(4);
        ObjectNode middle = new ObjectNode("Between");
        left[2].setElement(1, middle);
        middle.setField("oneWay", right[2]);

        List<StrongComponent> components = tarjanScc.computeComponents(left[3]);
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

        List<StrongComponent> components = tarjanScc.computeComponents(top);
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

        List<StrongComponent> components = tarjanScc.computeComponents(root);
        List<StrongComponent> expected = Stream.of(childOfLeft, left, right, root).map(StrongComponent::new).toList();
        assertEquals(expected, components);
    }
}