package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.ArrayNode;
import com.github.sulir.runtimesave.graph.GraphNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GraphIdHasherTest {
    private static final String TYPE = "Object[]";
    private static final TestGraphGenerator generator = new TestGraphGenerator();
    private GraphHasher hasher;
    private GraphIdHasher idHasher;

    @BeforeEach
    void setUp() {
        hasher = new GraphHasher();
        idHasher = new GraphIdHasher();
    }

    @ParameterizedTest
    @MethodSource("sameGraphPairs")
    void sameGraphsHaveSameIdHashes(GraphNode graph, GraphNode same) {
        hasher.assignHashes(graph);
        hasher.assignHashes(same);
        assertEquals(idHasher.assignIdHashes(graph), idHasher.assignIdHashes(same));
    }

    @ParameterizedTest
    @MethodSource("differentGraphPairs")
    void differentGraphsHaveDifferentIdHashes(GraphNode graph, GraphNode different) {
        hasher.assignHashes(graph);
        hasher.assignHashes(different);
        assertNotEquals(idHasher.assignIdHashes(graph), idHasher.assignIdHashes(different));
    }

    @Test
    void rightCycleAboveMergedNodeHaveDifferentIdHashThanAboveTwoNodes() {
        MergeableGraph first = createMergeableGraph();
        ArrayNode mergedNode = new ArrayNode(TYPE);
        first.leftJoinPoint().addElement(mergedNode);
        first.rightJoinPoint().addElement(mergedNode);

        MergeableGraph second = createMergeableGraph();
        ArrayNode splitNode1 = new ArrayNode(TYPE);
        ArrayNode splitNode2 = new ArrayNode(TYPE);
        second.leftJoinPoint().addElement(splitNode1);
        second.rightJoinPoint().addElement(splitNode2);

        hasher.assignHashes(first.root());
        idHasher.assignIdHashes(first.root());
        hasher.assignHashes(second.root());
        idHasher.assignIdHashes(second.root());

        assertEquals(first.inCycle().hash(), second.inCycle().hash());
        assertNotEquals(first.inCycle().idHash(), second.inCycle().idHash());
    }

    static Stream<Arguments> sameGraphPairs() {
        return generator.samePairs(generator::examples);
    }

    static Stream<Arguments> differentGraphPairs() {
        return generator.differentPairs(generator::examples);
    }

    private MergeableGraph createMergeableGraph() {
        ArrayNode root = new ArrayNode(TYPE);
        ArrayNode[] leftCycle = generator.circularNodes(4);
        ArrayNode[] rightCycle = generator.circularNodes(4);
        ArrayNode leftJoinPoint = new ArrayNode(TYPE);
        ArrayNode rightJoinPoint = new ArrayNode(TYPE);

        root.addElement(leftCycle[0]);
        root.addElement(rightCycle[0]);
        leftCycle[1].addElement(leftJoinPoint);
        leftCycle[3].addElement(leftJoinPoint);
        rightCycle[1].addElement(rightJoinPoint);
        rightCycle[3].addElement(rightJoinPoint);

        return new MergeableGraph(root, rightCycle[3], leftJoinPoint, rightJoinPoint);
    }

    private record MergeableGraph(ArrayNode root,
                                  ArrayNode inCycle,
                                  ArrayNode leftJoinPoint,
                                  ArrayNode rightJoinPoint) { }
}