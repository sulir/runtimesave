package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;

import java.util.*;

public class AcyclicGraph {
    private final List<StrongComponent> reverseComponents;
    private final List<StrongComponent> topoComponents;
    private final GraphNode rootNode;

    AcyclicGraph(List<StrongComponent> reverseComponents, GraphNode rootNode) {
        this.reverseComponents = reverseComponents;
        topoComponents = new ArrayList<>(reverseComponents);
        Collections.reverse(topoComponents);
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

    public StrongComponent getRootComponent() {
        return topoComponents.get(0);
    }

    public int getComponentCount() {
        return topoComponents.size();
    }

    public List<StrongComponent> topoOrder() {
        return topoComponents;
    }

    public List<StrongComponent> reverseTopoOrder() {
        return reverseComponents;
    }
}
