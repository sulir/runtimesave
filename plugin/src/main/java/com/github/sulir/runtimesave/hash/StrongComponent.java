package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;

import java.util.HashSet;
import java.util.Set;

public class StrongComponent {
    private final Set<GraphNode> nodes = new HashSet<>();

    public void add(GraphNode node) {
        nodes.add(node);
    }

    public boolean contains(GraphNode node) {
        return nodes.contains(node);
    }

    public Iterable<GraphNode> iterate() {
        return nodes;
    }
}
