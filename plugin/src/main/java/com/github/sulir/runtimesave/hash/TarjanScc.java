package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;

import java.util.List;

public class TarjanScc {
    private final GraphNode root;

    public TarjanScc(GraphNode root) {
        this.root = root;
    }

    public List<StrongComponent> computeComponents() {
        root.traverse(n -> {});
        return null;
    }
}
