package com.github.sulir.runtimesave.nodes;

public class NullNode extends GraphNode {
    private static final NullNode instance = new NullNode();

    public static NullNode getInstance() {
        return instance;
    }
}
