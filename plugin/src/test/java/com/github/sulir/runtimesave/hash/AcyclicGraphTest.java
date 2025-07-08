package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;
import com.github.sulir.runtimesave.graph.TestGraphGenerator;
import com.github.sulir.runtimesave.nodes.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        GraphNode cycle = new TestGraphGenerator().circularNodes(3)[0];
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(cycle);
        assertEquals(1, dag.getComponentCount());
    }

    @Test
    void rootNodeStaysTheSame() {
        GraphNode graph = new TestGraphGenerator().circularNodes(4)[0];
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(graph);
        assertEquals(graph, dag.getRootNode());
    }

    @Test
    void rootComponentIsLastInReverseTopoOrder() {
        StrongComponent root = new StrongComponent(referring);
        StrongComponent child = new StrongComponent(referred);
        root.addTarget(child);
        AcyclicGraph dag = new AcyclicGraph(List.of(child, root), referring);
        assertEquals(root, dag.getRootComponent());
    }

    @Test
    void topoOrderStartsWithReferringNode() {
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(referring);
        assertEquals(referring, dag.topoOrder().getFirst().getSoleNode());
    }

    @Test
    void reverseTopoOrderStartsWithReferredNode() {
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(referring);
        assertEquals(referred, dag.reverseTopoOrder().getFirst().getSoleNode());
    }
}