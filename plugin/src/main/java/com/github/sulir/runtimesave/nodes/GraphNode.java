package com.github.sulir.runtimesave.nodes;

import java.util.List;

public abstract class GraphNode {
    public Iterable<GraphNode> iterate() {
        return List.of();
    }
}
