package com.github.sulir.runtimesave.graph;

public class PrimitiveNode extends GraphNode {
    private final Object value;

    public PrimitiveNode(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
