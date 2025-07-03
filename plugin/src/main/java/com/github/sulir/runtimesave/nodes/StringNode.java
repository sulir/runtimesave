package com.github.sulir.runtimesave.nodes;

public class StringNode extends ValueNode {
    private final String value;

    public StringNode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
