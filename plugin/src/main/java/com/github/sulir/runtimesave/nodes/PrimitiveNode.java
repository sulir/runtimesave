package com.github.sulir.runtimesave.nodes;

public class PrimitiveNode extends GraphNode {
    private Object value;

    public PrimitiveNode(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
