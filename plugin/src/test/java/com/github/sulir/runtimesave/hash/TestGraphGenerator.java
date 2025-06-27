package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.*;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class TestGraphGenerator {
    private static final int RANDOM_COUNT = 1000;
    private static final int RANDOM_MIN_SIZE = 5;
    private static final int RANDOM_MAX_SIZE = 15;
    private static final int SEQ_PARALLEL_NONTREE_EDGES = 1;
    private static final int SEQ_MIN_SIZE = 2;
    private static final int SEQ_MAX_SIZE = 4;

    private Random random;

    List<GraphNode> cyclicAndAcyclic() {
        return Stream.of(acyclic().stream(), cyclic().stream()).flatMap(s -> s).toList();
    }

    List<GraphNode> acyclic() {
        GraphNode singleNode = new PrimitiveNode(1, "int");
        GraphNode otherSingleNode = new NullNode();

        ObjectNode oneChild = new ObjectNode("Type");
        oneChild.setField("field", singleNode);

        ArrayNode tree = new ArrayNode("Type[]");
        tree.setElement(0, otherSingleNode);
        tree.setElement(1, oneChild);

        ObjectNode dag = new ObjectNode("Top");
        NullNode dagChild = new NullNode();
        dag.setField("left", dagChild);
        dag.setField("right", dagChild);

        return List.of(singleNode, otherSingleNode, oneChild, tree, dag);
    }

    List<GraphNode> cyclic() {
        GraphNode oneCycle = circularGraph(1).get(0);
        GraphNode twoCycle = circularGraph(2).get(0);
        GraphNode threeCycle = circularGraph(3).get(0);

        List<ArrayNode> fourCycle = circularGraph(4);
        fourCycle.get(2).setElement(0, new NullNode());

        List<ArrayNode> fiveCycle = circularGraph(5);
        fiveCycle.get(1).setElement(0, new StringNode("a"));
        fiveCycle.get(1).setElement(1, new StringNode("b"));
        fiveCycle.get(2).setElement(0, new StringNode("c"));

        return List.of(oneCycle, twoCycle, threeCycle, fourCycle.get(0), fiveCycle.get(0));
    }

    List<GraphNode> random() {
        random = new Random(0);
        List<GraphNode> graphs = new ArrayList<>(RANDOM_COUNT);
        Set<Traversal> uniqueTraversals = new HashSet<>(RANDOM_COUNT);

        while (graphs.size() < RANDOM_COUNT) {
            GraphNode graph = randomGraph();
            if (uniqueTraversals.add(getTraversal(graph)))
                graphs.add(graph);
        }
        return graphs;
    }

    Stream<GraphNode> sequentiallyGenerated() {
        Set<Traversal> uniqueTraversals = new HashSet<>();

        return IntStream.range(SEQ_MIN_SIZE, SEQ_MAX_SIZE + 1).boxed().flatMap(nodeCount ->
            spanningSourcesList(nodeCount).flatMap(spanningSources -> {
                List<int[]> possibleEdges = allNumbers(2, nodeCount).toList();
                List<int[]> edgeCountsList = allNumbers(possibleEdges.size(), SEQ_PARALLEL_NONTREE_EDGES + 1).toList();
                return edgeCountsList.stream().map(edgeCounts -> {
                    List<EdgeSet> edgeSets = new ArrayList<>();
                    for (int i = 0; i < edgeCounts.length; i++) {
                        int[] edge = possibleEdges.get(i);
                        edgeSets.add(new EdgeSet(edge[0], edge[1], edgeCounts[i]));
                    }
                    return graphFromSequence(spanningSources, edgeSets);
                }).filter(graph -> uniqueTraversals.add(getTraversal(graph)));
            })
        );
    }

    List<ArrayNode> circularGraph(int size) {
        List<ArrayNode> nodes = new ArrayList<>();
        for (int i = 0; i < size; i++)
            nodes.add(new ArrayNode("Cls[]"));

        for (int i = 0; i < size; i++)
            nodes.get(i).setElement(0, nodes.get((i + 1) % size));

        return nodes;
    }

    private Stream<int[]> spanningSourcesList(int nodeCount) {
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

    private GraphNode graphFromSequence(int[] spanningSources, List<EdgeSet> edgeSets) {
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
        return traversal;
    }

    private record EdgeSet(int from, int to, int count) { }

    private static class Traversal extends ArrayList<List<Integer>> { }
}
