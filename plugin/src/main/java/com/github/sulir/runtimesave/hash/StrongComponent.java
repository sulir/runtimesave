package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;

import java.util.*;

public class StrongComponent {
    private final Set<GraphNode> nodes = new HashSet<>();
    private final List<StrongComponent> targets = new ArrayList<>();
    private GraphNode soleNode;

    public StrongComponent(GraphNode... nodes) {
        Collections.addAll(this.nodes, nodes);
    }

    public void add(GraphNode node) {
        nodes.add(node);
    }

    public boolean contains(GraphNode node) {
        return nodes.contains(node);
    }

    public Collection<GraphNode> nodes() {
        return nodes;
    }

    public List<StrongComponent> targets() {
        return targets;
    }

    public void addTarget(StrongComponent target) {
        targets.add(target);
    }

    public boolean isTrivial() {
        return nodes.size() == 1 && getSoleNode().targets().noneMatch(node -> node == getSoleNode());
    }

    public GraphNode getSoleNode() {
        if (nodes.size() != 1)
            throw new IllegalStateException("SCC has " + nodes.size() + "nodes");
        if (soleNode == null)
            soleNode = nodes.iterator().next();
        return soleNode;
    }

    public GraphNode getFirstNode() {
        return nodes.iterator().next();
    }

    public Set<GraphNode> getRestOfNodes() {
        Set<GraphNode> rest = new HashSet<>(nodes);
        rest.remove(getFirstNode());
        return rest;
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof StrongComponent that && nodes.equals(that.nodes);
    }

    @Override
    public int hashCode() {
        return nodes.hashCode();
    }

    @Override
    public String toString() {
        return nodes.toString().replaceFirst("^\\[(.*)]$", "{$1}");
    }
}
