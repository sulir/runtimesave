package com.github.sulir.runtimesave.graph;

public class LazyObjectGraph {
    private final LazyNode root;

    public LazyObjectGraph(LazyNode root) {
        this.root = root;
    }

    public LazyNode getRoot() {
        return root;
    }
}
