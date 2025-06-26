package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GraphHasherTest {
    private GraphHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new GraphHasher();
    }

    @ParameterizedTest
    @MethodSource("sameGraphPairs")
    void sameGraphsHaveSameHashes(GraphNode graph, GraphNode same) {
        assertEquals(hasher.assignHashes(graph), hasher.assignHashes(same));
    }

    @ParameterizedTest
    @MethodSource("differentGraphPairs")
    void differentGraphsHaveDifferentHashes(GraphNode graph, GraphNode different) {
        assertNotEquals(hasher.assignHashes(graph), hasher.assignHashes(different));
    }

    @ParameterizedTest
    @MethodSource("sameGraphPairs")
    void sameSubGraphsHaveSameHashes(GraphNode graph, GraphNode same) {
        FrameNode parent = new FrameNode();
        parent.setVariable("subgraph", graph);
        hasher.assignHashes(parent);

        hasher.assignHashes(same);
        assertEquals(graph.hash(), same.hash());
    }

    @Test
    void uniqueRandomGraphsHaveUniqueHashes() {
        TestGraphGenerator generator = new TestGraphGenerator();
        List<GraphNode> graphs = generator.random();
        Map<NodeHash, GraphNode> hashes = new HashMap<>();

        for (GraphNode graph : graphs)
            checkCollision(graph, hashes);
    }

    @Test
    void uniqueGeneratedGraphsHaveUniqueHashes() {
        TestGraphGenerator generator = new TestGraphGenerator();
        Map<NodeHash, GraphNode> hashes = new HashMap<>();

        generator.sequentiallyGenerated().forEach(graph -> checkCollision(graph, hashes));
    }

    @Test
    void edgesToIdenticalNodeAndCopiesAreDiscerned() {
        ObjectNode parentOfIdentical = new ObjectNode("Parent");
        ObjectNode childIdentical = new ObjectNode("Child");
        parentOfIdentical.setField("left", childIdentical);
        parentOfIdentical.setField("right", childIdentical);

        ObjectNode parentOfCopies = new ObjectNode("Parent");
        ObjectNode childCopy1 = new ObjectNode("Child");
        ObjectNode childCopy2 = new ObjectNode("Child");
        parentOfCopies.setField("left", childCopy1);
        parentOfCopies.setField("right", childCopy2);

        assertNotEquals(parentOfIdentical.hash(), parentOfCopies.hash());
    }

    @Test
    void dagMergePointAndNodeCopiesAreDiscerned() {
        ObjectNode dag = new ObjectNode("Top");
        ObjectNode left = new ObjectNode("Left");
        ObjectNode right = new ObjectNode("Right");
        StringNode bottom = new StringNode("");
        dag.setField("left", left);
        dag.setField("right", right);
        left.setField("target", bottom);
        right.setField("target", bottom);

        ObjectNode copy = new ObjectNode("Top");
        ObjectNode leftCopy = new ObjectNode("Left");
        ObjectNode rightCopy = new ObjectNode("Right");
        StringNode bottomCopy1 = new StringNode("");
        StringNode bottomCopy2 = new StringNode("");
        copy.setField("left", leftCopy);
        copy.setField("right", rightCopy);
        leftCopy.setField("target", bottomCopy1);
        rightCopy.setField("target", bottomCopy2);

        assertNotEquals(dag.hash(), copy.hash());
    }

    static Stream<Arguments> sameGraphPairs() {
        TestGraphGenerator generator = new TestGraphGenerator();
        List<GraphNode> graphs = generator.cyclicAndAcyclic();
        List<GraphNode> same = generator.cyclicAndAcyclic();

        return IntStream.range(0, graphs.size()).mapToObj(i -> Arguments.of(graphs.get(i), same.get(i)));
    }

    static Stream<Arguments> differentGraphPairs() {
        List<GraphNode> graphs = new TestGraphGenerator().cyclicAndAcyclic();
        List<GraphNode> shifted = new ArrayList<>(graphs);
        Collections.rotate(shifted, 1);

        return IntStream.range(0, graphs.size()).mapToObj(i -> Arguments.of(graphs.get(i), shifted.get(i)));
    }

    private void checkCollision(GraphNode graph, Map<NodeHash, GraphNode> hashes) {
        NodeHash hash = hasher.assignHashes(graph);
        GraphNode colliding = hashes.put(hash, graph);
        assertNull(colliding, "Hash collision for %s and %s".formatted(graph, colliding));
    }
}