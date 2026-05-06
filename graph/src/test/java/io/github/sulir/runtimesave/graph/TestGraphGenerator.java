package io.github.sulir.runtimesave.graph;

import io.github.sulir.runtimesave.nodes.ReferenceArrayNode;
import io.github.sulir.runtimesave.nodes.ObjectNode;
import io.github.sulir.runtimesave.nodes.PrimitiveNode;
import io.github.sulir.runtimesave.nodes.StringNode;
import org.junit.jupiter.params.provider.Arguments;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TestGraphGenerator {
    private static final int RANDOM_COUNT = 1000;
    private static final int RANDOM_MIN_SIZE = 5;
    private static final int RANDOM_MAX_SIZE = 15;
    private static final int SMALL_PARALLEL_NONTREE_EDGES = 1;
    private static final int SMALL_MIN_SIZE = 2;
    private static final int SMALL_MAX_SIZE = 4;
    private Random random;

    public Stream<Arguments> samePairs(Supplier<Stream<ValueNode>> graphProvider) {
        List<ValueNode> graphs = graphProvider.get().toList();
        List<ValueNode> same = graphProvider.get().toList();

        return IntStream.range(0, graphs.size()).mapToObj(i -> Arguments.of(graphs.get(i), same.get(i)));
    }

    public Stream<Arguments> differentPairs(Supplier<Stream<ValueNode>> graphProvider) {
        List<ValueNode> graphs = graphProvider.get().toList();
        List<ValueNode> shifted = new ArrayList<>(graphs);
        Collections.rotate(shifted, 1);

        return IntStream.range(0, graphs.size()).mapToObj(i -> Arguments.of(graphs.get(i), shifted.get(i)));
    }

    public Stream<ValueNode> examples() {
        return Stream.of(trees(), dags(), cyclicGraphs()).flatMap(s -> s);
    }

    public Stream<ValueNode> trees() {
        PrimitiveNode singleNode = PrimitiveNode.getInstance(1, "int");
        StringNode otherSingleNode = new StringNode("");

        ObjectNode oneChild = new ObjectNode("Type");
        oneChild.setField("field", singleNode);

        ReferenceArrayNode tree = new ReferenceArrayNode("Type[]", 2);
        tree.setElement(0, otherSingleNode);
        tree.setElement(1, oneChild);

        return Stream.of(singleNode, otherSingleNode, oneChild, tree);
    }

    public Stream<ValueNode> dags() {
        ObjectNode parallelEdges = new ObjectNode("Top");
        StringNode child = new StringNode("");
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

    public Stream<ValueNode> cyclicGraphs() {
        ReferenceArrayNode oneCycle = circularNodes(1)[0];
        ReferenceArrayNode twoCycle = circularNodes(2)[0];
        ReferenceArrayNode threeCycle = circularNodes(3)[0];

        ReferenceArrayNode[] fourCycle = circularNodes(4);
        fourCycle[2].setElement(0, new StringNode(""));

        ReferenceArrayNode[] fiveCycle = circularNodes(5);
        fiveCycle[1].setElement(0, new StringNode("a"));
        fiveCycle[1].setElement(1, new StringNode("b"));
        fiveCycle[2].setElement(0, new StringNode("c"));

        return Stream.of(oneCycle, twoCycle, threeCycle, fourCycle[0], fiveCycle[0]);
    }

    public Stream<GraphNode> randomGraphs() {
        random = new Random(0);
        Set<List<?>> uniqueTraversals = new HashSet<>(RANDOM_COUNT);

        return IntStream.range(0, RANDOM_COUNT)
                .mapToObj(i -> randomGraph())
                .filter(graph -> uniqueTraversals.add(TestUtils.getBfsTraversal(graph)));
    }

    public Stream<GraphNode> allSmallGraphs() {
        Set<List<?>> uniqueTraversals = new HashSet<>();

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
                }).filter(graph -> uniqueTraversals.add(TestUtils.getBfsTraversal(graph)));
            });
        });
    }

    public ReferenceArrayNode[] circularNodes(int length) {
        ReferenceArrayNode[] nodes = new ReferenceArrayNode[length];
        for (int i = 0; i < length; i++)
            nodes[i] = new ReferenceArrayNode("Object[]", 2);

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
        ReferenceArrayNode[] nodes = new ReferenceArrayNode[nodeCount];
        for (int i = 0; i < nodeCount; i++)
            nodes[i] = new ReferenceArrayNode("Object[]", nodeCount);

        for (int i = 1; i < nodeCount; i++) {
            ReferenceArrayNode connected = nodes[spanningSources[i - 1]];
            connected.setElement(i - 1, nodes[i]);
        }

        for (EdgeSet edgeSet : edgeSets)
            for (int i = 0; i < edgeSet.count; i++)
                nodes[edgeSet.from].setElement(i, nodes[edgeSet.to]);

        return nodes[0];
    }

    private GraphNode randomGraph() {
        int nodeCount = Math.max(random.nextInt(RANDOM_MIN_SIZE, RANDOM_MAX_SIZE + 1),
                random.nextInt(RANDOM_MIN_SIZE, RANDOM_MAX_SIZE + 1));
        ReferenceArrayNode[] nodes = new ReferenceArrayNode[nodeCount];
        for (int i = 0; i < nodeCount; i++)
            nodes[i] = new ReferenceArrayNode("Type[]", nodeCount);

        for (int i = 1; i < nodeCount; i++) {
            ReferenceArrayNode connected = nodes[random.nextInt(i)];
            connected.setElement(i - 1, nodes[i]);
        }

        int maxAdditionalEdges = Math.max(RANDOM_COUNT, nodeCount * nodeCount);
        int additionalEdges = random.nextInt(maxAdditionalEdges);
        for (int i = 0; i < additionalEdges; i++)
            nodes[random.nextInt(nodeCount)].setElement(random.nextInt(nodeCount), nodes[random.nextInt(nodeCount)]);

        return nodes[0];
    }

    private record EdgeSet(int from, int to, int count) { }
}
