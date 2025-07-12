package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;
import com.github.sulir.runtimesave.graph.TestGraphGenerator;
import com.github.sulir.runtimesave.nodes.ArrayNode;
import com.github.sulir.runtimesave.nodes.FrameNode;
import com.github.sulir.runtimesave.graph.ValueNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TreeHasherTest {
    private LocalHasher localHasher;
    private TreeHasher treeHasher;

    @BeforeEach
    void setUp() {
        localHasher = new LocalHasher(new ObjectHasher());
        treeHasher = new TreeHasher(new ObjectHasher());
    }

    @Test
    void nonTreeNodesDoNotGetHash() {
        ArrayNode[] cycle = new TestGraphGenerator().circularNodes(2);
        assignLocalAndTreeHashes(cycle[0]);
        assertAll(() -> assertFalse(cycle[0].hasHash()),
                  () -> assertFalse(cycle[1].hasHash()));
    }

    @ParameterizedTest
    @MethodSource("sameTreePairs")
    void sameTreesHaveSameHashes(ValueNode tree, ValueNode same) {
        assignLocalAndTreeHashes(tree);
        assignLocalAndTreeHashes(same);
        assertEquals(tree.hash(), same.hash());

    }

    @ParameterizedTest
    @MethodSource("differentTreePairs")
    void differentTreesHaveDifferentHashes(ValueNode tree, ValueNode different) {
        assignLocalAndTreeHashes(tree);
        assignLocalAndTreeHashes(different);
        assertNotEquals(tree.hash(), different.hash());
    }

    @ParameterizedTest
    @MethodSource("sameTreePairs")
    void sameSubTreesHaveSameHashes(ValueNode tree, ValueNode same) {
        FrameNode parent = new FrameNode();
        parent.setVariable("subtree", tree);
        assignLocalAndTreeHashes(parent);

        assignLocalAndTreeHashes(same);
        assertEquals(tree.hash(), same.hash());
    }

    static Stream<Arguments> sameTreePairs() {
        TestGraphGenerator generator = new TestGraphGenerator();
        return generator.samePairs(generator::trees);
    }

    static Stream<Arguments> differentTreePairs() {
        TestGraphGenerator generator = new TestGraphGenerator();
        return generator.differentPairs(generator::trees);
    }

    private void assignLocalAndTreeHashes(GraphNode graph) {
        localHasher.assignLocalHashes(graph);
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(graph);
        treeHasher.assignHashes(dag);
    }
}