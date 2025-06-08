package com.github.sulir.runtimesave.nodes;

public class StringNode extends GraphNode {
    private final String value;

    public StringNode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
