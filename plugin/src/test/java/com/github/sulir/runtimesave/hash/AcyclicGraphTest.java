package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;
import com.github.sulir.runtimesave.nodes.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AcyclicGraphTest {
    private ObjectNode referring;
    private ObjectNode referred;

    @BeforeEach
    void setUp() {
        referring = new ObjectNode("Referring");
        referred = new ObjectNode("Referred");
        referring.setField("ref", referred);
    }

    @Test
    void condensationOfCycleIsOneScc() {
        GraphNode cycle = new TestGraphGenerator().circularNodes(3).get(0);
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(cycle);
        assertEquals(1, dag.getComponentCount());
    }

    @Test
    void rootNodeStaysTheSame() {
        GraphNode graph = new TestGraphGenerator().circularNodes(4).get(0);
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(graph);
        assertEquals(graph, dag.getRootNode());
    }

    @Test
    void topoOrderStartsWithReferringNode() {
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(referring);
        assertEquals(referring, dag.topoOrder().findFirst().orElseThrow().getSoleNode());
    }

    @Test
    void reverseTopoOrderStartsWithReferredNode() {
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(referring);
        assertEquals(referred, dag.reverseTopoOrder().findFirst().orElseThrow().getSoleNode());
    }
}