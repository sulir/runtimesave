package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AcyclicGraph {
    private final List<StrongComponent> components;
    private final GraphNode rootNode;

    AcyclicGraph(List<StrongComponent> components, GraphNode rootNode) {
        this.components = components;
        this.rootNode = rootNode;
    }

    public static AcyclicGraph multiCondensationOf(GraphNode cyclicGraph) {
        List<StrongComponent> components = new TarjanScc().computeComponents(cyclicGraph);
        Map<GraphNode, StrongComponent> nodeToComponent = new HashMap<>();

        for (StrongComponent component : components)
            for (GraphNode node : component.nodes())
                nodeToComponent.put(node, component);

        for (StrongComponent component : components)
            for (GraphNode source : component.nodes())
                for (GraphNode target : source.targets())
                    nodeToComponent.get(source).addTarget(nodeToComponent.get(target));

        return new AcyclicGraph(components, cyclicGraph);
    }

    public GraphNode getRootNode() {
        return rootNode;
    }

    public int getComponentCount() {
        return components.size();
    }

    public Stream<StrongComponent> topoOrder() {
        return IntStream.range(0, components.size()).map(i -> components.size() - i - 1).mapToObj(components::get);
    }

    public Stream<StrongComponent> reverseTopoOrder() {
        return components.stream();
    }
}
