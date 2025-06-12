package com.github.sulir.runtimesave.nodes;

public class PrimitiveNode extends GraphNode {
    private final Object value;
    private final String type;

    public PrimitiveNode(Object value, String type) {
        this.value = value;
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
