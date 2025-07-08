package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;
import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.nodes.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GraphHasherTest {
    private static final TestGraphGenerator generator = new TestGraphGenerator();
    private GraphHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new GraphHasher();
    }

    @ParameterizedTest
    @MethodSource("sameGraphPairs")
    void sameGraphsHaveSameHashes(ValueNode graph, ValueNode same) {
        assertEquals(hasher.assignHashes(graph), hasher.assignHashes(same));
    }

    @ParameterizedTest
    @MethodSource("differentGraphPairs")
    void differentGraphsHaveDifferentHashes(ValueNode graph, ValueNode different) {
        assertNotEquals(hasher.assignHashes(graph), hasher.assignHashes(different));
    }

    @ParameterizedTest
    @MethodSource("sameGraphPairs")
    void sameSubGraphsHaveSameHashes(ValueNode graph, ValueNode same) {
        FrameNode parent = new FrameNode();
        parent.setVariable("subgraph", graph);
        hasher.assignHashes(parent);

        hasher.assignHashes(same);
        assertEquals(graph.hash(), same.hash());
    }

    @Test
    void uniqueRandomGraphsHaveUniqueHashes() {
        Set<NodeHash> hashes = new HashSet<>();
        generator.randomGraphs().forEach(graph -> checkCollision(graph, hashes));
    }

    @Test
    void uniqueGeneratedGraphsHaveUniqueHashes() {
        if (System.getProperty("REPORT_COLLIDED_GRAPH") == null) {
            Set<NodeHash> hashes = new HashSet<>();
            generator.allSmallGraphs().forEach(graph -> checkCollision(graph, hashes));
        } else {
            Map<NodeHash, GraphNode> hashes = new HashMap<>();
            generator.allSmallGraphs().forEach(graph -> checkCollision(graph, hashes));
        }
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

        hasher.assignHashes(parentOfIdentical);
        hasher.assignHashes(parentOfCopies);
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

        hasher.assignHashes(dag);
        hasher.assignHashes(copy);
        assertNotEquals(dag.hash(), copy.hash());
    }

    static Stream<Arguments> sameGraphPairs() {
        return generator.samePairs(generator::examples);
    }

    static Stream<Arguments> differentGraphPairs() {
        return generator.differentPairs(generator::examples);
    }

    private void checkCollision(GraphNode graph, Set<NodeHash> hashes) {
        NodeHash hash = hasher.assignHashes(graph);
        assertTrue(hashes.add(hash), () -> "Hash collision for " + graph);
    }

    private void checkCollision(GraphNode graph, Map<NodeHash, GraphNode> hashes) {
        NodeHash hash = hasher.assignHashes(graph);
        GraphNode original = hashes.put(hash, graph);
        assertNull(original, () -> "Hash collision for %s and %s".formatted(original, graph));
    }
}