package com.github.sulir.runtimesave.nodes;

import java.util.Map;

public class TypeNode extends GraphNode {
    private static final Map<String, TypeNode> nameToNode = new java.util.HashMap<>();

    private final String name;

    public static TypeNode getInstance(String name) {
        return nameToNode.computeIfAbsent(name, TypeNode::new);
    }

    private TypeNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
