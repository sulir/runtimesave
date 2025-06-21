package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.FrameNode;
import com.github.sulir.runtimesave.nodes.GraphNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    static Stream<Arguments> sameGraphPairs() {
        TestGraphGenerator generator = new TestGraphGenerator();
        List<GraphNode> graphs = generator.all();
        List<GraphNode> same = generator.all();

        return IntStream.range(0, graphs.size()).mapToObj(i -> Arguments.of(graphs.get(i), same.get(i)));
    }

    static Stream<Arguments> differentGraphPairs() {
        List<GraphNode> graphs = new TestGraphGenerator().all();
        List<GraphNode> shifted = new ArrayList<>(graphs);
        Collections.rotate(shifted, 1);

        return IntStream.range(0, graphs.size()).mapToObj(i -> Arguments.of(graphs.get(i), shifted.get(i)));
    }
}