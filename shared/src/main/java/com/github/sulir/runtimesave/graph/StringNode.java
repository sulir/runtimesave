package com.github.sulir.runtimesave.graph;

public class StringNode extends LazyNode {
    private final String value;

    public StringNode(String value) {
        this.value = value;
        this.type = "java.lang.String";
    }

    public String getValue() {
        return value;
    }
}
