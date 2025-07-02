package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.ArrayNode;
import com.github.sulir.runtimesave.nodes.FrameNode;
import com.github.sulir.runtimesave.nodes.GraphNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TreeHasherTest {
    private TreeHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new TreeHasher();
    }

    @Test
    void nonTreeNodesDoNotGetHash() {
        ArrayNode[] cycle = new TestGraphGenerator().circularNodes(2);
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(cycle[0]);
        hasher.assignHashes(dag);
        assertAll(() -> assertFalse(cycle[0].hasHash()),
                  () -> assertFalse(cycle[1].hasHash()));
    }

    @ParameterizedTest
    @MethodSource("sameTreePairs")
    void sameTreesHaveSameHashes(GraphNode tree, GraphNode same) {
        hasher.assignHashes(AcyclicGraph.multiCondensationOf(tree));
        hasher.assignHashes(AcyclicGraph.multiCondensationOf(same));
        assertEquals(tree.hash(), same.hash());

    }

    @ParameterizedTest
    @MethodSource("differentTreePairs")
    void differentTreesHaveDifferentHashes(GraphNode tree, GraphNode different) {
        hasher.assignHashes(AcyclicGraph.multiCondensationOf(tree));
        hasher.assignHashes(AcyclicGraph.multiCondensationOf(different));
        assertNotEquals(tree.hash(), different.hash());
    }

    @ParameterizedTest
    @MethodSource("sameTreePairs")
    void sameSubTreesHaveSameHashes(GraphNode tree, GraphNode same) {
        FrameNode parent = new FrameNode();
        parent.setVariable("subtree", tree);
        hasher.assignHashes(AcyclicGraph.multiCondensationOf(parent));

        hasher.assignHashes(AcyclicGraph.multiCondensationOf(same));
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
}