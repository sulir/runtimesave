package com.github.sulir.runtimesave.nodes;

public class PrimitiveNode extends GraphNode {
    private Object value;
    private String type;

    public PrimitiveNode(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
