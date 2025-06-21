package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.*;

import java.util.*;
import java.util.stream.Stream;

class TestGraphGenerator {
    public static final int RANDOM_GRAPHS = 50;
    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 15;

    private Random random;

    List<GraphNode> all() {
        return Stream.of(acyclic().stream(), cyclic().stream(), random().stream()).flatMap(s -> s).toList();
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
        ObjectNode left = new ObjectNode("Left");
        ObjectNode right = new ObjectNode("Right");
        StringNode bottom = new StringNode("");
        dag.setField("left", left);
        dag.setField("right", right);
        left.setField("target", bottom);
        right.setField("target", bottom);

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
        List<GraphNode> graphs = new ArrayList<>(RANDOM_GRAPHS);
        Set<Traversal> uniqueTraversals = new HashSet<>(RANDOM_GRAPHS);

        while (graphs.size() < RANDOM_GRAPHS) {
            GraphNode graph = randomGraph();
            if (uniqueTraversals.add(getTraversal(graph)))
                graphs.add(graph);
        }
        return graphs;
    }

    List<ArrayNode> circularGraph(int size) {
        List<ArrayNode> nodes = new ArrayList<>();
        for (int i = 0; i < size; i++)
            nodes.add(new ArrayNode("Cls[]"));

        for (int i = 0; i < size; i++)
            nodes.get(i).setElement(0, nodes.get((i + 1) % size));

        return nodes;
    }

    private GraphNode randomGraph() {
        int nodeCount = Math.max(random.nextInt(MIN_SIZE, MAX_SIZE + 1), random.nextInt(MIN_SIZE, MAX_SIZE + 1));
        ArrayNode[] nodes = new ArrayNode[nodeCount];
        for (int i = 0; i < nodeCount; i++)
            nodes[i] = new ArrayNode("Type[]");

        for (int i = 1; i < nodeCount; i++) {
            ArrayNode connected = nodes[random.nextInt(i)];
            connected.addElement(nodes[i]);
        }

        int maxAdditionalEdges = Math.max(RANDOM_GRAPHS, nodeCount * nodeCount);
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

    private static class Traversal extends ArrayList<List<Integer>> { }
}
