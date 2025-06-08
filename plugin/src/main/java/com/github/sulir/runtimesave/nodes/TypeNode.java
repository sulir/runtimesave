package com.github.sulir.runtimesave.nodes;

public class TypeNode extends GraphNode {
    private final String name;

    public TypeNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
