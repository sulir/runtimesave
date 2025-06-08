package com.github.sulir.runtimesave.nodes;

public abstract class ReferenceNode extends GraphNode {
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
