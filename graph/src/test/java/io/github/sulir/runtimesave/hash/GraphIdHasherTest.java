package io.github.sulir.runtimesave.hash;

import io.github.sulir.runtimesave.graph.GraphNode;
import io.github.sulir.runtimesave.graph.TestGraphGenerator;
import io.github.sulir.runtimesave.nodes.ReferenceArrayNode;
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
        ReferenceArrayNode mergedNode = new ReferenceArrayNode(TYPE);
        first.leftJoinPoint().addElement(mergedNode);
        first.rightJoinPoint().addElement(mergedNode);

        MergeableGraph second = createMergeableGraph();
        ReferenceArrayNode splitNode1 = new ReferenceArrayNode(TYPE);
        ReferenceArrayNode splitNode2 = new ReferenceArrayNode(TYPE);
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
        ReferenceArrayNode root = new ReferenceArrayNode(TYPE);
        ReferenceArrayNode[] leftCycle = generator.circularNodes(4);
        ReferenceArrayNode[] rightCycle = generator.circularNodes(4);
        ReferenceArrayNode leftJoinPoint = new ReferenceArrayNode(TYPE);
        ReferenceArrayNode rightJoinPoint = new ReferenceArrayNode(TYPE);

        root.addElement(leftCycle[0]);
        root.addElement(rightCycle[0]);
        leftCycle[1].addElement(leftJoinPoint);
        leftCycle[3].addElement(leftJoinPoint);
        rightCycle[1].addElement(rightJoinPoint);
        rightCycle[3].addElement(rightJoinPoint);

        return new MergeableGraph(root, rightCycle[3], leftJoinPoint, rightJoinPoint);
    }

    private record MergeableGraph(ReferenceArrayNode root,
                                  ReferenceArrayNode inCycle,
                                  ReferenceArrayNode leftJoinPoint,
                                  ReferenceArrayNode rightJoinPoint) { }
}