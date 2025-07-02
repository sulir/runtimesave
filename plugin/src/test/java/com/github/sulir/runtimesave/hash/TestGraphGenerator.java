package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.*;
import org.junit.jupiter.params.provider.Arguments;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class TestGraphGenerator {
    private static final int RANDOM_COUNT = 1000;
    private static final int RANDOM_MIN_SIZE = 5;
    private static final int RANDOM_MAX_SIZE = 15;
    private static final int SMALL_PARALLEL_NONTREE_EDGES = 1;
    private static final int SMALL_MIN_SIZE = 2;
    private static final int SMALL_MAX_SIZE = 4;
    private Random random;

    Stream<Arguments> samePairs(Supplier<Stream<GraphNode>> graphProvider) {
        List<GraphNode> graphs = graphProvider.get().toList();
        List<GraphNode> same = graphProvider.get().toList();

        return IntStream.range(0, graphs.size()).mapToObj(i -> Arguments.of(graphs.get(i), same.get(i)));
    }

    Stream<Arguments> differentPairs(Supplier<Stream<GraphNode>> graphProvider) {
        List<GraphNode> graphs = graphProvider.get().toList();
        List<GraphNode> shifted = new ArrayList<>(graphs);
        Collections.rotate(shifted, 1);

        return IntStream.range(0, graphs.size()).mapToObj(i -> Arguments.of(graphs.get(i), shifted.get(i)));
    }

    Stream<GraphNode> examples() {
        return Stream.of(trees(), dags(), cyclicGraphs()).flatMap(s -> s);
    }

    Stream<GraphNode> trees() {
        GraphNode singleNode = new PrimitiveNode(1, "int");
        GraphNode otherSingleNode = new NullNode();

        ObjectNode oneChild = new ObjectNode("Type");
        oneChild.setField("field", singleNode);

        ArrayNode tree = new ArrayNode("Type[]");
        tree.setElement(0, otherSingleNode);
        tree.setElement(1, oneChild);

        return Stream.of(singleNode, otherSingleNode, oneChild, tree);
    }

    Stream<GraphNode> dags() {
        ObjectNode parallelEdges = new ObjectNode("Top");
        NullNode child = new NullNode();
        parallelEdges.setField("left", child);
        parallelEdges.setField("right", child);

        ObjectNode triangle = new ObjectNode("Triangle");
        ObjectNode triangleA = new ObjectNode("A");
        ObjectNode triangleB = new ObjectNode("B");
        triangle.setField("a", triangleA);
        triangle.setField("b", triangleB);
        triangleA.setField("b", triangleB);

        return Stream.of(parallelEdges, triangle);
    }

    Stream<GraphNode> cyclicGraphs() {
        GraphNode oneCycle = circularNodes(1)[0];
        GraphNode twoCycle = circularNodes(2)[0];
        GraphNode threeCycle = circularNodes(3)[0];

        ArrayNode[] fourCycle = circularNodes(4);
        fourCycle[2].setElement(0, new NullNode());

        ArrayNode[] fiveCycle = circularNodes(5);
        fiveCycle[1].setElement(0, new StringNode("a"));
        fiveCycle[1].setElement(1, new StringNode("b"));
        fiveCycle[2].setElement(0, new StringNode("c"));

        return Stream.of(oneCycle, twoCycle, threeCycle, fourCycle[0], fiveCycle[0]);
    }

    Stream<GraphNode> randomGraphs() {
        random = new Random(0);
        Set<Traversal> uniqueTraversals = new HashSet<>(RANDOM_COUNT);

        return IntStream.range(0, RANDOM_COUNT)
                .mapToObj(i -> randomGraph())
                .filter(graph -> uniqueTraversals.add(getTraversal(graph)));
    }

    Stream<GraphNode> allSmallGraphs() {
        Set<Traversal> uniqueTraversals = new HashSet<>();

        return IntStream.range(SMALL_MIN_SIZE, SMALL_MAX_SIZE + 1).boxed().flatMap(nodeCount -> {
            List<int[]> possibleEdges = allNumbers(2, nodeCount).toList();
            return possibleSpanningSources(nodeCount).flatMap(spanningSources -> {
                Stream<int[]> possibleEdgeCounts = allNumbers(possibleEdges.size(),
                        SMALL_PARALLEL_NONTREE_EDGES + 1);
                return possibleEdgeCounts.map(edgeCounts -> {
                    List<EdgeSet> edgeSets = new ArrayList<>();
                    for (int i = 0; i < edgeCounts.length; i++) {
                        int[] edge = possibleEdges.get(i);
                        edgeSets.add(new EdgeSet(edge[0], edge[1], edgeCounts[i]));
                    }
                    return createGraph(spanningSources, edgeSets);
                }).filter(graph -> uniqueTraversals.add(getTraversal(graph)));
            });
        });
    }

    ArrayNode[] circularNodes(int length) {
        ArrayNode[] nodes = new ArrayNode[length];
        for (int i = 0; i < length; i++)
            nodes[i] = new ArrayNode("Cls[]");

        for (int i = 0; i < length; i++)
            nodes[i].setElement(0, nodes[(i + 1) % length]);

        return nodes;
    }

    private Stream<int[]> possibleSpanningSources(int nodeCount) {
        return allNumbers(nodeCount - 1, nodeCount - 1)
                .filter(n -> IntStream.range(1, n.length).allMatch(i -> n[i] <= i && n[i - 1] <= n[i]))
                .takeWhile(n -> n[0] == 0);
    }

    private Stream<int[]> allNumbers(int length, int base) {
        return IntStream.range(0, (int) Math.pow(base, length)).mapToObj(i -> {
            String number = Integer.toString(i, base);
            String padded = "0".repeat(length - number.length()) + number;
            return padded.chars().map(c -> c - '0').toArray();
        });
    }

    private GraphNode createGraph(int[] spanningSources, List<EdgeSet> edgeSets) {
        int nodeCount = spanningSources.length + 1;
        ArrayNode[] nodes = new ArrayNode[nodeCount];
        for (int i = 0; i < nodeCount; i++)
            nodes[i] = new ArrayNode("Object[]");

        for (int i = 1; i < nodeCount; i++) {
            ArrayNode connected = nodes[spanningSources[i - 1]];
            connected.addElement(nodes[i]);
        }

        for (EdgeSet edgeSet : edgeSets)
            for (int i = 0; i < edgeSet.count; i++)
                nodes[edgeSet.from].addElement(nodes[edgeSet.to]);

        return nodes[0];
    }

    private GraphNode randomGraph() {
        int nodeCount = Math.max(random.nextInt(RANDOM_MIN_SIZE, RANDOM_MAX_SIZE + 1),
                random.nextInt(RANDOM_MIN_SIZE, RANDOM_MAX_SIZE + 1));
        ArrayNode[] nodes = new ArrayNode[nodeCount];
        for (int i = 0; i < nodeCount; i++)
            nodes[i] = new ArrayNode("Type[]");

        for (int i = 1; i < nodeCount; i++) {
            ArrayNode connected = nodes[random.nextInt(i)];
            connected.addElement(nodes[i]);
        }

        int maxAdditionalEdges = Math.max(RANDOM_COUNT, nodeCount * nodeCount);
        int additionalEdges = random.nextInt(maxAdditionalEdges);
        for (int i = 0; i < additionalEdges; i++)
            nodes[random.nextInt(nodeCount)].addElement(nodes[random.nextInt(nodeCount)]);

        return nodes[0];
    }

    private Traversal getTraversal(GraphNode graph) {
        Map<GraphNode, Integer> nodeNumbers = new HashMap<>();
        graph.traverse(node -> nodeNumbers.put(node, nodeNumbers.size()));

        Traversal traversal = new Traversal();
        graph.traverse(node -> traversal.add(node.targets().stream().map(nodeNumbers::get).toList()));
        traversal.trimToSize();
        return traversal;
    }

    private record EdgeSet(int from, int to, int count) { }

    private static class Traversal extends ArrayList<List<Integer>> { }
}
