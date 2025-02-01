package com.github.sulir.runtimesave.graph;

public class PrimitiveNode extends GraphNode {
    private final Object value;

    public PrimitiveNode(Object value, String type) {
        this.value = value;
        this.type = type;
    }

    public Object getValue() {
        return value;
    }
}
